package com.velocitypowered.proxy.network.packet.proxy.handshake;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacket;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.proxy.network.NetworkBuffer.STRING;

public record ResponsePacket(@NotNull String jsonResponse) implements ProxyPacket {

    public ResponsePacket(@NotNull NetworkBuffer reader) {
        this(reader.read(STRING));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(STRING, jsonResponse);
    }

    @Override
    public int getId() {
        return 0x00;
    }

}