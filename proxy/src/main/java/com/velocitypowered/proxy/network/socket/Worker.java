package com.velocitypowered.proxy.network.socket;

import com.velocitypowered.proxy.MinecraftProxy;
import com.velocitypowered.proxy.network.player.ClientSocketConnection;
import com.velocitypowered.proxy.thread.VelocityThread;
import com.velocitypowered.proxy.util.ObjectPool;
import com.velocitypowered.proxy.util.binary.BinaryBuffer;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
public final class Worker extends VelocityThread {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    final Selector selector;
    private final Map<SocketChannel, ClientSocketConnection> connectionMap = new ConcurrentHashMap<>();
    private final Server server;
    private final MpscUnboundedXaddArrayQueue<Runnable> queue = new MpscUnboundedXaddArrayQueue<>(1024);

    Worker(Server server) {
        super("Ms-worker-" + COUNTER.getAndIncrement());
        this.server = server;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (server.isOpen()) {
            try {
                try {
                    this.queue.drain(Runnable::run);
                } catch (Exception e) {
                    MinecraftProxy.getExceptionManager().handleException(e);
                }

                // Flush all connections if needed
                for (ClientSocketConnection connection : connectionMap.values()) {
                    try {
                        connection.flushSync();
                    } catch (Exception e) {
                        connection.disconnect();
                    }
                }

                // Wait for an event
                this.selector.select(key -> {
                    final SocketChannel channel = (SocketChannel) key.channel();
                    if (!channel.isOpen()) return;
                    if (!key.isReadable()) return;
                    final ClientSocketConnection connection = connectionMap.get(channel);
                    if (connection == null) {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            // Empty
                        }
                        return;
                    }
                    try {
                        try (var holder = ObjectPool.PACKET_POOL.hold()) {
                            BinaryBuffer readBuffer = BinaryBuffer.wrap(holder.get());
                            // Consume last incomplete packet
                            connection.consumeCache(readBuffer);
                            // Read & process
                            readBuffer.readChannel(channel);
                            connection.processPackets(readBuffer, server.packetProcessor());
                        }
                    } catch (IOException e) {
                        // TODO print exception? (should ignore disconnection)
                        connection.disconnect();
                    } catch (Throwable t) {
                        MinecraftProxy.getExceptionManager().handleException(t);
                        connection.disconnect();
                    }
                }, MinecraftProxy.TICK_MS);
            } catch (Exception e) {
                MinecraftProxy.getExceptionManager().handleException(e);
            }
        }
    }

    public void disconnect(ClientSocketConnection connection, SocketChannel channel) {
        assert !connection.isOnline();
        assert Thread.currentThread() == this;
        this.connectionMap.remove(channel);
        if (channel.isOpen()) {
            try {
                connection.flushSync();
                channel.close();
            } catch (IOException e) {
                // Socket operation may fail if the socket is already closed
            }
        }
    }

    void receiveConnection(SocketChannel channel) throws IOException {
        this.connectionMap.put(channel, new ClientSocketConnection(this, channel, channel.getRemoteAddress()));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        if (channel.getLocalAddress() instanceof InetSocketAddress) {
            Socket socket = channel.socket();
            socket.setSendBufferSize(Server.SOCKET_SEND_BUFFER_SIZE);
            socket.setReceiveBufferSize(Server.SOCKET_RECEIVE_BUFFER_SIZE);
            socket.setTcpNoDelay(Server.NO_DELAY);
            socket.setSoTimeout(30 * 1000); // 30 seconds
        }
        this.selector.wakeup();
    }

    public MessagePassingQueue<Runnable> queue() {
        return queue;
    }

}