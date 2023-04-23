package com.velocitypowered.proxy;

import com.velocitypowered.proxy.exception.ExceptionManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProxyProcessImpl implements ProxyProcess {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyProcessImpl.class);

    private final ExceptionManager exception;

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    public ProxyProcessImpl() throws IOException {
        this.exception = new ExceptionManager();
    }

    @Override
    public void start(@NotNull SocketAddress socketAddress) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }

        LOGGER.info(MinecraftProxy.getBrandName() + " server started successfully.");

        // Stop the server on SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        LOGGER.info("Stopping " + MinecraftProxy.getBrandName() + " server.");
        LOGGER.info(MinecraftProxy.getBrandName() + " server stopped successfully.");
    }

    @Override
    public boolean isAlive() {
        return started.get() && !stopped.get();
    }

    @Override
    public @NotNull ExceptionManager exception() {
        return exception;
    }

}