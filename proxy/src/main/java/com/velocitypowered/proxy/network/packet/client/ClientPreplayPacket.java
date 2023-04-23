package com.velocitypowered.proxy.network.packet.client;

import com.velocitypowered.proxy.MinecraftProxy;
import com.velocitypowered.proxy.network.player.ClientConnection;
import org.jetbrains.annotations.NotNull;

public interface ClientPreplayPacket extends ClientPacket {

    ConnectionManager CONNECTION_MANAGER = MinecraftProxy.getConnectionManager();

    /**
     * Called when the packet is received.
     *
     * @param connection the connection who sent the packet
     */
    void process(@NotNull ClientConnection connection);

}