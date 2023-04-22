package com.velocitypowered.proxy.network.packet.proxy;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a packet that can be sent to a {@link PlayerConnection}.
 */
@ApiStatus.Experimental
public sealed interface SendablePacket permits CachedPacket, FramedPacket, LazyPacket, ProxyPacket {

    @ApiStatus.Experimental
    static @NotNull ProxyPacket extractServerPacket(@NotNull SendablePacket packet) {
        if (packet instanceof ProxyPacket proxyPacket) {
            return proxyPacket;
        } else if (packet instanceof CachedPacket cachedPacket) {
            return cachedPacket.packet();
        } else if (packet instanceof FramedPacket framedPacket) {
            return framedPacket.packet();
        } else if (packet instanceof LazyPacket lazyPacket) {
            return lazyPacket.packet();
        } else {
            throw new RuntimeException("Unknown packet type: " + packet.getClass().getName());
        }
    }
}