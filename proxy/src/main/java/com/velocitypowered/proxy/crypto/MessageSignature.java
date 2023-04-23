package com.velocitypowered.proxy.crypto;

import com.velocitypowered.proxy.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static com.velocitypowered.proxy.network.NetworkBuffer.BYTE_ARRAY;

public record MessageSignature(byte @NotNull [] signature) implements NetworkBuffer.Writer {

    public MessageSignature(@NotNull NetworkBuffer reader) {
        this(reader.read(BYTE_ARRAY));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(BYTE_ARRAY, signature);
    }

}