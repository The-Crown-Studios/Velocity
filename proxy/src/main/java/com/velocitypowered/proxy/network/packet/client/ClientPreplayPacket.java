package com.velocitypowered.proxy.network.packet.client;

import org.jetbrains.annotations.NotNull;

public interface ClientPreplayPacket extends ClientPacket {

    ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    /**
     * Called when the packet is received.
     *
     * @param connection the connection who sent the packet
     */
    void process(@NotNull PlayerConnection connection);

}