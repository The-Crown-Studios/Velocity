package com.velocitypowered.proxy.network.packet.client;

/**
 * Represents a packet received from a client.
 * <p>
 * Packets are value-based, and should therefore not be reliant on identity.
 */
public interface ClientPacket extends NetworkBuffer.Writer {
}