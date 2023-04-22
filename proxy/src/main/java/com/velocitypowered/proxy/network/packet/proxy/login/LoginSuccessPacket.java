package com.velocitypowered.proxy.network.packet.proxy.login;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacket;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.velocitypowered.proxy.network.NetworkBuffer.STRING;
import static com.velocitypowered.proxy.network.NetworkBuffer.VAR_INT;

public record LoginSuccessPacket(
        @NotNull UUID uuid,
        @NotNull String username,
        int properties
) implements ProxyPacket {

    public LoginSuccessPacket(@NotNull NetworkBuffer reader) {
        this(
                reader.read(NetworkBuffer.UUID),
                reader.read(STRING),
                reader.read(VAR_INT));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, uuid);
        writer.write(STRING, username);
        writer.write(VAR_INT, properties);
    }

    @Override
    public int getId() {
        return ProxyPacketIdentifier.LOGIN_SUCCESS;
    }

}