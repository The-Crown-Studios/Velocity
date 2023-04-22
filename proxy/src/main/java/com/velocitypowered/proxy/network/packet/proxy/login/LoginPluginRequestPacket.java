package com.velocitypowered.proxy.network.packet.proxy.login;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacket;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacketIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.velocitypowered.proxy.network.NetworkBuffer.*;

public record LoginPluginRequestPacket(
        int messageId, @NotNull String channel,
        byte @Nullable [] data
) implements ProxyPacket {

    public LoginPluginRequestPacket(@NotNull NetworkBuffer reader) {
        this(
                reader.read(VAR_INT),
                reader.read(STRING),
                reader.read(RAW_BYTES)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(VAR_INT, messageId);
        writer.write(STRING, channel);
        if (data != null && data.length > 0) {
            writer.write(RAW_BYTES, data);
        }
    }

    @Override
    public int getId() {
        return ProxyPacketIdentifier.LOGIN_PLUGIN_REQUEST;
    }

}