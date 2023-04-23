package com.velocitypowered.proxy;

import com.velocitypowered.proxy.exception.ExceptionManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * The main server class used to start the server and retrieve all the managers.
 * <p>
 * The server needs to be initialized with {@link #init()} and started with {@link #start(String, int)}.
 * You should register all of your dimensions, biomes, commands, events, etc... in-between.
 */
public class MinecraftProxy {

    public static final ComponentLogger LOGGER = ComponentLogger.logger(MinecraftProxy.class);

    // In-Game Manager
    private static volatile ProxyProcess proxyProcess;

    private static String brandName = "Velocity";

    public static MinecraftProxy init() {
        updateProcess();
        return new MinecraftProxy();
    }

    @ApiStatus.Internal
    public static ProxyProcess updateProcess() {
        ProxyProcess process;
        try {
            process = new ProxyProcessImpl();
            proxyProcess = process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return process;
    }

    public static boolean isStarted() {
        return proxyProcess.isAlive();
    }

    public static boolean isStopping() {
        return !isStarted();
    }

    @ApiStatus.Experimental
    public static @UnknownNullability ProxyProcess process() {
        return proxyProcess;
    }

    public static @NotNull ExceptionManager getExceptionManager() {
        return proxyProcess.exception();
    }

    /**
     * Gets the current server brand name.
     *
     * @return the server brand name
     */
    @NotNull
    public static String getBrandName() {
        return brandName;
    }

    /**
     * Starts the server.
     * <p>
     * It should be called after {@link #init()} and probably your own initialization code.
     *
     * @param address the server address
     * @throws IllegalStateException if called before {@link #init()} or if the server is already running
     */
    public void start(@NotNull SocketAddress address) {
        proxyProcess.start(address);
        //new TickSchedulerThread(proxyProcess).start();
    }

    public void start(@NotNull String address, int port) {
        start(new InetSocketAddress(address, port));
    }

    /**
     * Stops this server properly (saves if needed, kicking players, etc.)
     */
    public static void stopCleanly() {
        proxyProcess.stop();
    }

}
