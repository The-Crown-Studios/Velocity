package com.velocitypowered.proxy.network.packet.proxy.login;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacket;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.proxy.network.NetworkBuffer.BYTE_ARRAY;
import static com.velocitypowered.proxy.network.NetworkBuffer.STRING;

public record EncryptionRequestPacket(
        @NotNull String serverId,
        byte @NotNull [] publicKey,
        byte @NotNull [] verifyToken
) implements ProxyPacket {

    public EncryptionRequestPacket(@NotNull NetworkBuffer reader) {
        this(
                reader.read(STRING),
                reader.read(BYTE_ARRAY),
                reader.read(BYTE_ARRAY)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(STRING, serverId);
        writer.write(BYTE_ARRAY, publicKey);
        writer.write(BYTE_ARRAY, verifyToken);
    }

    @Override
    public int getId() {
        return ProxyPacketIdentifier.LOGIN_ENCRYPTION_REQUEST;
    }

}