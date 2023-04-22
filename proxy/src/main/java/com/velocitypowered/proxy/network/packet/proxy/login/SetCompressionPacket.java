package com.velocitypowered.proxy.network.packet.proxy.login;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacket;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacketIdentifier;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.proxy.network.NetworkBuffer.VAR_INT;

public record SetCompressionPacket(int threshold) implements ProxyPacket {

    public SetCompressionPacket(@NotNull NetworkBuffer reader) {
        this(reader.read(VAR_INT));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(VAR_INT, threshold);
    }

    @Override
    public int getId() {
        return ProxyPacketIdentifier.LOGIN_SET_COMPRESSION;
    }

}