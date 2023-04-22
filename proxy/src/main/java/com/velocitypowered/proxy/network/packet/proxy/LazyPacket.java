package com.velocitypowered.proxy.network.packet.proxy;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents a packet that is lazily allocated. Potentially in a different thread.
 * <p>
 * Supplier must be thread-safe.
 */
@ApiStatus.Internal
public final class LazyPacket implements SendablePacket {

    private final Supplier<ProxyPacket> packetSupplier;
    private volatile ProxyPacket packet;

    public LazyPacket(@NotNull Supplier<@NotNull ProxyPacket> packetSupplier) {
        this.packetSupplier = packetSupplier;
    }

    public @NotNull ProxyPacket packet() {
        ProxyPacket packet = this.packet;
        if (packet == null) {
            synchronized (this) {
                packet = this.packet;
                if (packet == null) this.packet = packet = packetSupplier.get();
            }
        }
        return packet;
    }

}