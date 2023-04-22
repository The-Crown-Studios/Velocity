package com.velocitypowered.proxy.util;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.adventure.ComponentHolder;
import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.*;
import com.velocitypowered.proxy.util.binary.BinaryBuffer;
import com.velocitypowered.proxy.util.binary.BinaryUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

/**
 * Utils class for packets. Including writing a {@link ServerPacket} into a {@link ByteBuffer}
 * for network processing.
 * <p>
 * Note that all methods are mostly internal and can change at any moment.
 * This is due to their very unsafe nature (use of local buffers as cache) and their potential performance impact.
 * Be sure to check the implementation code.
 */
public final class PacketUtils {

    private static final ThreadLocal<Deflater> LOCAL_DEFLATER = ThreadLocal.withInitial(Deflater::new);

    public static final boolean GROUPED_PACKET = PropertyUtils.getBoolean("minestom.grouped-packet", true);
    public static final boolean CACHED_PACKET = PropertyUtils.getBoolean("minestom.cached-packet", true);
    public static final boolean VIEWABLE_PACKET = PropertyUtils.getBoolean("minestom.viewable-packet", true);

    private PacketUtils() {
    }

    /**
     * Sends a packet to an audience. This method performs the following steps in the
     * following order:
     * <ol>
     *     <li>If {@code audience} is a {@link Player}, send the packet to them.</li>
     *     <li>Otherwise, if {@code audience} is a {@link PacketGroupingAudience}, call
     *     {@link #sendGroupedPacket(Collection, ServerPacket)} on the players that the
     *     grouping audience contains.</li>
     *     <li>Otherwise, if {@code audience} is a {@link ForwardingAudience.Single},
     *     call this method on the single audience inside the forwarding audience.</li>
     *     <li>Otherwise, if {@code audience} is a {@link ForwardingAudience}, call this
     *     method for each audience member of the forwarding audience.</li>
     *     <li>Otherwise, do nothing.</li>
     * </ol>
     *
     * @param audience the audience
     * @param packet   the packet
     */
    @SuppressWarnings("OverrideOnly") // we need to access the audiences inside ForwardingAudience
    public static void sendPacket(@NotNull Audience audience, @NotNull ProxyPacket packet) {
        if (audience instanceof Player player) {
            player.sendPacket(packet);
        } else if (audience instanceof PacketGroupingAudience groupingAudience) {
            PacketUtils.sendGroupedPacket(groupingAudience.getPlayers(), packet);
        } else if (audience instanceof ForwardingAudience.Single singleAudience) {
            PacketUtils.sendPacket(singleAudience.audience(), packet);
        } else if (audience instanceof ForwardingAudience forwardingAudience) {
            for (Audience member : forwardingAudience.audiences()) {
                PacketUtils.sendPacket(member, packet);
            }
        }
    }

    /**
     * Sends a {@link ProxyPacket} to multiple players.
     * <p>
     * Can drastically improve performance since the packet will not have to be processed as much.
     *
     * @param players   the players to send the packet to
     * @param packet    the packet to send to the players
     * @param predicate predicate to ignore specific players
     */
    public static void sendGroupedPacket(
            @NotNull Collection<Player> players,
            @NotNull ProxyPacket packet,
            @NotNull Predicate<Player> predicate)
    {
        final var sendablePacket = shouldUseCachePacket(packet) ? new CachedPacket(packet) : packet;

        players.forEach(player -> {
            if (predicate.test(player)) player.sendPacket(sendablePacket);
        });
    }

    /**
     * Checks if the {@link ProxyPacket} is suitable to be wrapped into a {@link CachedPacket}.
     * Note: {@link ComponentHoldingProxyPacket}s are not translated inside a {@link CachedPacket}.
     *
     * @see CachedPacket#body()
     * @see PlayerSocketConnection#writePacketSync(SendablePacket, boolean)
     */
    static boolean shouldUseCachePacket(final @NotNull ProxyPacket packet) {
        if (!MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION) return GROUPED_PACKET;
        if (!(packet instanceof ComponentHoldingProxyPacket holder)) return GROUPED_PACKET;
        return !containsTranslatableComponents(holder);
    }

    private static boolean containsTranslatableComponents(final @NotNull ComponentHolder<?> holder) {
        for (final Component component : holder.components()) {
            if (isTranslatable(component)) return true;
        }

        return false;
    }

    private static boolean isTranslatable(final @NotNull Component component) {
        if (component instanceof TranslatableComponent) return true;

        final var children = component.children();
        if (children.isEmpty()) return false;

        for (final Component child : children) {
            if (isTranslatable(child)) return true;
        }

        return false;
    }

    /**
     * Same as {@link #sendGroupedPacket(Collection, ProxyPacket, Predicate)}
     * but with the player validator sets to null.
     *
     * @see #sendGroupedPacket(Collection, ProxyPacket, Predicate)
     */
    public static void sendGroupedPacket(@NotNull Collection<Player> players, @NotNull ProxyPacket packet) {
        sendGroupedPacket(players, packet, player -> true);
    }

    public static void broadcastPacket(@NotNull ProxyPacket packet) {
        sendGroupedPacket(MinecraftServer.getConnectionManager().getOnlinePlayers(), packet);
    }

    @ApiStatus.Internal
    public static void flush() {
        if (VIEWABLE_PACKET) {
            VIEWABLE_STORAGE_MAP.asMap().entrySet().parallelStream().forEach(entry ->
                    entry.getValue().process(entry.getKey()));
        }
    }

    @ApiStatus.Internal
    public static @Nullable BinaryBuffer readPackets(@NotNull BinaryBuffer readBuffer, boolean compressed,
                                                     BiConsumer<Integer, ByteBuffer> payloadConsumer) throws DataFormatException {
        BinaryBuffer remaining = null;
        ByteBuffer pool = ObjectPool.PACKET_POOL.get();
        while (readBuffer.readableBytes() > 0) {
            final var beginMark = readBuffer.mark();
            try {
                // Ensure that the buffer contains the full packet (or wait for next socket read)
                final int packetLength = readBuffer.readVarInt();
                final int readerStart = readBuffer.readerOffset();
                if (!readBuffer.canRead(packetLength)) {
                    // Integrity fail
                    throw new BufferUnderflowException();
                }
                // Read packet https://wiki.vg/Protocol#Packet_format
                BinaryBuffer content = readBuffer;
                int decompressedSize = packetLength;
                if (compressed) {
                    final int dataLength = readBuffer.readVarInt();
                    final int payloadLength = packetLength - (readBuffer.readerOffset() - readerStart);
                    if (payloadLength < 0) {
                        throw new DataFormatException("Negative payload length " + payloadLength);
                    }
                    if (dataLength == 0) {
                        // Data is too small to be compressed, payload is following
                        decompressedSize = payloadLength;
                    } else {
                        // Decompress to content buffer
                        content = BinaryBuffer.wrap(pool);
                        decompressedSize = dataLength;
                        Inflater inflater = new Inflater(); // TODO: Pool?
                        inflater.setInput(readBuffer.asByteBuffer(readBuffer.readerOffset(), payloadLength));
                        inflater.inflate(content.asByteBuffer(0, dataLength));
                        inflater.reset();
                    }
                }
                // Slice packet
                ByteBuffer payload = content.asByteBuffer(content.readerOffset(), decompressedSize);
                final int packetId = Utils.readVarInt(payload);
                try {
                    payloadConsumer.accept(packetId, payload);
                } catch (Exception e) {
                    // Empty
                }
                // Position buffer to read the next packet
                readBuffer.readerOffset(readerStart + packetLength);
            } catch (BufferUnderflowException e) {
                readBuffer.reset(beginMark);
                remaining = BinaryBuffer.copy(readBuffer);
                break;
            }
        }
        ObjectPool.PACKET_POOL.add(pool);
        return remaining;
    }

    public static void writeFramedPacket(@NotNull ByteBuffer buffer,
                                         @NotNull ProxyPacket packet,
                                         boolean compression) {
        writeFramedPacket(buffer, packet.getId(), packet,
                compression ? MinecraftServer.getCompressionThreshold() : 0);
    }

    public static void writeFramedPacket(@NotNull ByteBuffer buffer,
                                         int id,
                                         @NotNull NetworkBuffer.Writer writer,
                                         int compressionThreshold) {
        NetworkBuffer networkBuffer = new NetworkBuffer(buffer, false);
        if (compressionThreshold <= 0) {
            // Uncompressed format https://wiki.vg/Protocol#Without_compression
            final int lengthIndex = networkBuffer.skipWrite(3);
            networkBuffer.write(NetworkBuffer.VAR_INT, id);
            networkBuffer.write(writer);
            final int finalSize = networkBuffer.writeIndex() - (lengthIndex + 3);
            BinaryUtils.writeVarIntHeader(buffer, lengthIndex, finalSize);
            buffer.position(networkBuffer.writeIndex());
            return;
        }
        // Compressed format https://wiki.vg/Protocol#With_compression
        final int compressedIndex = networkBuffer.skipWrite(3);
        final int uncompressedIndex = networkBuffer.skipWrite(3);

        final int contentStart = networkBuffer.writeIndex();
        networkBuffer.write(NetworkBuffer.VAR_INT, id);
        networkBuffer.write(writer);
        final int packetSize = networkBuffer.writeIndex() - contentStart;
        final boolean compressed = packetSize >= compressionThreshold;
        if (compressed) {
            // Packet large enough, compress it
            try (var hold = ObjectPool.PACKET_POOL.hold()) {
                final ByteBuffer input = hold.get().put(0, buffer, contentStart, packetSize);
                Deflater deflater = LOCAL_DEFLATER.get();
                deflater.setInput(input.limit(packetSize));
                deflater.finish();
                deflater.deflate(buffer.position(contentStart));
                deflater.reset();

                networkBuffer.skipWrite(buffer.position() - contentStart);
            }
        }
        // Packet header (Packet + Data Length)
        BinaryUtils.writeVarIntHeader(buffer, compressedIndex, networkBuffer.writeIndex() - uncompressedIndex);
        BinaryUtils.writeVarIntHeader(buffer, uncompressedIndex, compressed ? packetSize : 0);

        buffer.position(networkBuffer.writeIndex());
    }

    @ApiStatus.Internal
    public static ByteBuffer createFramedPacket(@NotNull ByteBuffer buffer, @NotNull ProxyPacket packet, boolean compression) {
        writeFramedPacket(buffer, packet, compression);
        return buffer.flip();
    }

    @ApiStatus.Internal
    public static ByteBuffer createFramedPacket(@NotNull ByteBuffer buffer, @NotNull ProxyPacket packet) {
        return createFramedPacket(buffer, packet, MinecraftServer.getCompressionThreshold() > 0);
    }

    @ApiStatus.Internal
    public static FramedPacket allocateTrimmedPacket(@NotNull ProxyPacket packet) {
        try (var hold = ObjectPool.PACKET_POOL.hold()) {
            final ByteBuffer temp = PacketUtils.createFramedPacket(hold.get(), packet);
            final int size = temp.remaining();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(size).put(0, temp, 0, size);
            return new FramedPacket(packet, buffer);
        }
    }

}