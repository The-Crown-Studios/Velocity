package com.velocitypowered.proxy.network.packet.client.login;

import com.velocitypowered.proxy.crypto.PlayerPublicKey;
import com.velocitypowered.proxy.crypto.SignatureValidator;
import com.velocitypowered.proxy.network.NetworkBuffer;
import com.velocitypowered.proxy.network.packet.client.ClientPreplayPacket;
import com.velocitypowered.proxy.network.player.ClientConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.velocitypowered.proxy.network.NetworkBuffer.*;

public record LoginStartPacket(
        @NotNull String username,
        @Nullable PlayerPublicKey publicKey,
        @Nullable UUID profileId

) implements ClientPreplayPacket {

    private static final Component ALREADY_CONNECTED = Component.text("You are already on this server", NamedTextColor.RED);

    public LoginStartPacket(@NotNull NetworkBuffer reader) {
        this(reader.read(STRING), reader.readOptional(PlayerPublicKey::new), reader.readOptional(UUID));
    }

    @Override
    public void process(@NotNull ClientConnection connection) {
        // TODO use uuid
        // TODO configurable check & messages
        if (publicKey != null) {
            if (!SignatureValidator.YGGDRASIL.validate(binaryWriter -> {
                if (profileId != null) {
                    binaryWriter.write(LONG, profileId.getMostSignificantBits());
                    binaryWriter.write(LONG, profileId.getLeastSignificantBits());
                } else {
                    MinecraftServer.LOGGER.warn("Profile ID was null for player {}, signature will not match!", username);
                }
                binaryWriter.write(LONG, publicKey.expiresAt().toEpochMilli());
                binaryWriter.write(RAW_BYTES, publicKey.publicKey().getEncoded());
            }, publicKey.signature())) {
                connection.sendPacket(new LoginDisconnectPacket(Component.text("Invalid Profile Public Key!")));
                connection.disconnect();
            }
            if (publicKey.expiresAt().isBefore(Instant.now())) {
                connection.sendPacket(new LoginDisconnectPacket(Component.text("Expired Profile Public Key!")));
                connection.disconnect();
            }
            connection.setPlayerPublicKey(publicKey);
        }
        final boolean isSocketConnection = connection instanceof PlayerSocketConnection;
        // Proxy support (only for socket clients) and cache the login username
        if (isSocketConnection) {
            PlayerSocketConnection socketConnection = (PlayerSocketConnection) connection;
            socketConnection.UNSAFE_setLoginUsername(username);
            // Velocity support
            if (VelocityProxy.isEnabled()) {
                final int messageId = ThreadLocalRandom.current().nextInt();
                final String channel = VelocityProxy.PLAYER_INFO_CHANNEL;
                // Important in order to retrieve the channel in the response packet
                socketConnection.addPluginRequestEntry(messageId, channel);
                connection.sendPacket(new LoginPluginRequestPacket(messageId, channel, null));
                return;
            }
        }

        if (MojangAuth.isEnabled() && isSocketConnection) {
            // Mojang auth
            if (CONNECTION_MANAGER.getPlayer(username) != null) {
                connection.sendPacket(new LoginDisconnectPacket(ALREADY_CONNECTED));
                connection.disconnect();
                return;
            }
            final PlayerSocketConnection socketConnection = (PlayerSocketConnection) connection;
            socketConnection.setConnectionState(ConnectionState.LOGIN);

            final byte[] publicKey = MojangAuth.getKeyPair().getPublic().getEncoded();
            byte[] nonce = new byte[4];
            ThreadLocalRandom.current().nextBytes(nonce);
            socketConnection.setNonce(nonce);
            socketConnection.sendPacket(new EncryptionRequestPacket("", publicKey, nonce));
        } else {
            final boolean bungee = BungeeCordProxy.isEnabled();
            // Offline
            final UUID playerUuid = bungee && isSocketConnection ?
                    ((PlayerSocketConnection) connection).gameProfile().uuid() :
                    CONNECTION_MANAGER.getPlayerConnectionUuid(connection, username);
            CONNECTION_MANAGER.startPlayState(connection, playerUuid, username, true);
        }
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        if (username.length() > 16)
            throw new IllegalArgumentException("Username is not allowed to be longer than 16 characters");

        writer.write(STRING, username);
    }

}