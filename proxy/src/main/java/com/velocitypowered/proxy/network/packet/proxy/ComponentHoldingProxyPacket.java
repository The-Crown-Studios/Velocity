package com.velocitypowered.proxy.network.packet.proxy;

import com.velocitypowered.proxy.adventure.ComponentHolder;

/**
 * A server packet that can hold components.
 */
public interface ComponentHoldingProxyPacket extends ProxyPacket, ComponentHolder<ProxyPacket> { }