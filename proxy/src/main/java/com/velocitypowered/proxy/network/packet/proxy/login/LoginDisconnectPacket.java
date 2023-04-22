package com.velocitypowered.proxy.network.packet.proxy.login;

import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.proxy.ComponentHoldingProxyPacket;
import com.velocitypowered.proxy.network.packet.proxy.ProxyPacketIdentifier;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import static com.velocitypowered.proxy.network.NetworkBuffer.COMPONENT;

public record LoginDisconnectPacket(@NotNull Component kickMessage) implements ComponentHoldingProxyPacket {

    public LoginDisconnectPacket(@NotNull NetworkBuffer reader) {
        this(reader.read(COMPONENT));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(COMPONENT, kickMessage);
    }

    @Override
    public int getId() {
        return ProxyPacketIdentifier.LOGIN_DISCONNECT;
    }

    @Override
    public @NotNull Collection<Component> components() {
        return List.of(this.kickMessage);
    }

    @Override
    public @NotNull LoginDisconnectPacket copyWithOperator(@NotNull UnaryOperator<Component> operator) {
        return new LoginDisconnectPacket(operator.apply(this.kickMessage));
    }

}