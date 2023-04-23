package com.velocitypowered.proxy;

import com.velocitypowered.proxy.exception.ExceptionManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface ProxyProcess {

    void start(@NotNull SocketAddress socketAddress);

    void stop();

    boolean isAlive();

    /**
     * Handles all thrown exceptions from the server.
     */
    @NotNull ExceptionManager exception();

}
