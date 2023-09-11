package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public class PlayerTablistShowEvent implements PlayerEvent {
    private final Player player;
    private final boolean firstSpawn;

    private final Instance newInstance;

    private Iterable<Player> tablistRecipients;
    private Predicate<? super Player> showPlayer = (ignored) -> true;

    public PlayerTablistShowEvent(@NotNull Player player, boolean firstSpawn, @NotNull Instance newInstance) {
        this.player = Objects.requireNonNull(player);
        this.firstSpawn = firstSpawn;

        this.newInstance = Objects.requireNonNull(newInstance);
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public boolean isFirstSpawn() {
        return firstSpawn;
    }

    public @NotNull Instance newInstance() {
        return newInstance;
    }

    public void setTablistParticipants(@Nullable Iterable<Player> players) {
        this.tablistRecipients = players;
    }

    public @Nullable Iterable<Player> tablistParticipants() {
        return tablistRecipients;
    }

    public @NotNull Predicate<? super @NotNull Player> showPlayerPredicate() {
        return showPlayer;
    }

    public void setShowPlayer(@NotNull Predicate<? super @NotNull Player> showPlayer) {
        this.showPlayer = Objects.requireNonNull(showPlayer);
    }
}