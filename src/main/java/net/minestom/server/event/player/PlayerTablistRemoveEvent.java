package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlayerTablistRemoveEvent implements PlayerEvent {
    private final Player player;

    private boolean broadcastTablistRemoval = true;

    public PlayerTablistRemoveEvent(@NotNull Player player) {
        this.player = Objects.requireNonNull(player);
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public void setBroadcastTablistRemoval(boolean remove) {
        this.broadcastTablistRemoval = remove;
    }

    public boolean broadcastTablistRemoval() {
        return broadcastTablistRemoval;
    }
}
