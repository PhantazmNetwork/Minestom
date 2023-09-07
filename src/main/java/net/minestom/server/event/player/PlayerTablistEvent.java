package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerTablistEvent implements PlayerEvent {
    private final Player player;
    private final boolean firstSpawn;

    private final Instance oldInstance;
    private final Instance newInstance;

    private final List<Player> tablistRecipients;

    public PlayerTablistEvent(@NotNull Player player, boolean firstSpawn, @Nullable Instance oldInstance,
                              @NotNull Instance newInstance) {
        this.player = Objects.requireNonNull(player);
        this.firstSpawn = firstSpawn;

        this.oldInstance = oldInstance;
        this.newInstance = Objects.requireNonNull(newInstance);
        this.tablistRecipients = new ArrayList<>();
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public boolean isFirstSpawn() {
        return firstSpawn;
    }

    public @Nullable Instance oldInstance() {
        return oldInstance;
    }

    public @NotNull Instance newInstance() {
        return newInstance;
    }

    public @NotNull List<Player> tablistAddRecipients() {
        return tablistRecipients;
    }
}