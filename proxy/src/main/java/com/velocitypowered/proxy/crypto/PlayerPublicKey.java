package com.velocitypowered.proxy.crypto;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.util.crypto.KeyUtils;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;

import static com.velocitypowered.proxy.network.NetworkBuffer.BYTE_ARRAY;
import static com.velocitypowered.proxy.network.NetworkBuffer.LONG;

/**
 * Player's public key used to sign chat messages
 */
public record PlayerPublicKey(
        Instant expiresAt, PublicKey publicKey,
        byte[] signature

) implements NetworkBuffer.Writer {

    public PlayerPublicKey(@NotNull NetworkBuffer reader) {
        this(
                Instant.ofEpochMilli(reader.read(LONG)),
                KeyUtils.publicRSAKeyFrom(reader.read(BYTE_ARRAY)),
                reader.read(BYTE_ARRAY)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(LONG, expiresAt().toEpochMilli());
        writer.write(BYTE_ARRAY, publicKey.getEncoded());
        writer.write(BYTE_ARRAY, signature());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlayerPublicKey ppk) {
            return expiresAt.equals(ppk.expiresAt) && publicKey.equals(ppk.publicKey) && Arrays.equals(signature, ppk.signature);
        } else {
            return false;
        }
    }

}