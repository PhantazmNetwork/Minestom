package net.minestom.server.network.packet.client.login;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
<<<<<<< HEAD
import net.minestom.server.entity.Player;
=======
>>>>>>> upstream/master
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPreplayPacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.player.PlayerSocketConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static net.minestom.server.network.NetworkBuffer.*;

public record LoginPluginResponsePacket(int messageId, byte @Nullable [] data) implements ClientPreplayPacket {
    private final static ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();
    public static final Component INVALID_PROXY_RESPONSE = Component.text("Invalid proxy response!", NamedTextColor.RED);

    public LoginPluginResponsePacket(@NotNull NetworkBuffer reader) {
        this(reader.read(VAR_INT), reader.readOptional(RAW_BYTES));
    }

    @Override
    public void process(@NotNull PlayerConnection connection) {
        // Proxy support
        if (connection instanceof PlayerSocketConnection socketConnection) {
            final String channel = socketConnection.getPluginRequestChannel(messageId);
            if (channel != null) {
                boolean success = false;

                SocketAddress socketAddress = null;
<<<<<<< HEAD
                UUID playerUuid = null;
                String playerUsername = null;
                VelocityProxy.Response response = null;
=======
                GameProfile gameProfile = null;
>>>>>>> upstream/master

                // Velocity
                if (VelocityProxy.isEnabled() && channel.equals(VelocityProxy.PLAYER_INFO_CHANNEL)) {
                    if (data != null && data.length > 0) {
                        NetworkBuffer buffer = new NetworkBuffer(ByteBuffer.wrap(data));
                        success = VelocityProxy.checkIntegrity(buffer);
                        if (success) {
                            // Get the real connection address
                            final InetAddress address;
                            try {
                                address = InetAddress.getByName(buffer.read(STRING));
                            } catch (UnknownHostException e) {
                                MinecraftServer.getExceptionManager().handleException(e);
                                return;
                            }
                            final int port = ((java.net.InetSocketAddress) connection.getRemoteAddress()).getPort();
                            socketAddress = new InetSocketAddress(address, port);
<<<<<<< HEAD

                            playerUuid = reader.readUuid();
                            playerUsername = reader.readSizedString(16);

                            response = VelocityProxy.readResponse(reader);
=======
                            gameProfile = new GameProfile(buffer);
>>>>>>> upstream/master
                        }
                    }
                }

                if (success) {
<<<<<<< HEAD
                    if (socketAddress != null) {
                        socketConnection.setRemoteAddress(socketAddress);
                    }
                    if (playerUsername != null) {
                        socketConnection.UNSAFE_setLoginUsername(playerUsername);
                    }
                    socketConnection.UNSAFE_setActualProtocolVersion(response.protocolVersion());

                    final String username = socketConnection.getLoginUsername();
                    final UUID uuid = playerUuid != null ?
                            playerUuid : CONNECTION_MANAGER.getPlayerConnectionUuid(connection, username);

                    Player player = CONNECTION_MANAGER.startPlayState(connection, uuid, username, true);
                    player.setSkin(response.playerSkin());
=======
                    socketConnection.setRemoteAddress(socketAddress);
                    socketConnection.UNSAFE_setProfile(gameProfile);
                    CONNECTION_MANAGER.startPlayState(connection, gameProfile.uuid(), gameProfile.name(), true);
>>>>>>> upstream/master
                } else {
                    LoginDisconnectPacket disconnectPacket = new LoginDisconnectPacket(INVALID_PROXY_RESPONSE);
                    socketConnection.sendPacket(disconnectPacket);
                }
            }
        }
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(VAR_INT, messageId);
        writer.writeOptional(RAW_BYTES, data);
    }
}
