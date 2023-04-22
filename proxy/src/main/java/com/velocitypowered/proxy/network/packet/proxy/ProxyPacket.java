package com.velocitypowered.proxy.network.packet.proxy;

import com.velocitypowered.proxy.network.NetworkBuffer;

/**
 * Represents a packet which can be sent to a player using {@link PlayerConnection#sendPacket(SendablePacket)}.
 * <p>
 * Packets are value-based, and should therefore not be reliant on identity.
 */
public non-sealed interface ProxyPacket extends NetworkBuffer.Writer, SendablePacket {

    /**
     * Gets the id of this packet.
     * <p>
     * Written in the final buffer header, so it needs to match the client id.
     *
     * @return the id of this packet
     */
    int getId();

}