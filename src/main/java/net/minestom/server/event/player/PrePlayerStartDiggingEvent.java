package net.minestom.server.event.player;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.listener.PlayerDiggingListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PrePlayerStartDiggingEvent implements PlayerInstanceEvent, BlockEvent, CancellableEvent {
    private PlayerDiggingListener.DiggingResult result;
    private final Player player;
    private final Point blockPosition;

    private boolean cancelled;

    public PrePlayerStartDiggingEvent(@NotNull PlayerDiggingListener.DiggingResult result, @NotNull Player player,
                                      @NotNull Point blockPosition) {
        this.result = Objects.requireNonNull(result, "result");
        this.player = Objects.requireNonNull(player, "player");
        this.blockPosition = Objects.requireNonNull(blockPosition, "blockPosition");
    }

    @Override
    public @NotNull Block getBlock() {
        return result.block();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull PlayerDiggingListener.DiggingResult getResult() {
        return result;
    }

    public void setResult(@NotNull PlayerDiggingListener.DiggingResult result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public @NotNull Point getBlockPosition() {
        return blockPosition;
    }
}
