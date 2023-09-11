package net.minestom.server.event.player;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Called before tablist packets are sent to new players. Fired when a player first logs in, as well as when they change
 * instances.
 */
public class PlayerTablistShowEvent implements PlayerEvent {
    private final Player player;
    private final boolean firstSpawn;

    private final Instance newInstance;

    private Iterable<Player> tablistRecipients;
    private BiPredicate<? super Player, ? super Player> showParticipantToJoiningPlayer = (ignored, ignored2) -> true;
    private BiPredicate<? super Player, ? super Player> showJoiningPlayerToParticipant = (ignored, ignored2) -> true;

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

    /**
     * The players who are participating in the tablist event; these are both recipients of tablist packets from the
     * joining player, and senders of tablist packets to the joining player. If null, the default vanilla behavior is
     * used, which sends tablist packets to all players on the server.
     * <p>
     * This iterable need not contain {@link PlayerTablistShowEvent#getPlayer()}, as players will always see themselves
     * in the tab list.
     *
     * @return an {@link Iterable} containing all tablist participants
     */
    public @Nullable Iterable<Player> tablistParticipants() {
        return tablistRecipients;
    }

    /**
     * A {@link BiPredicate} that is called in order to determine if the joining player
     * {@link PlayerTablistShowEvent#getPlayer()} should see a participant in the tab list, which will be a different
     * player. The <i>first</i> parameter of the predicate is the joining player; the second is a different
     * participant.
     *
     * @return a BiPredicate for determining which tablist participants should be shown to the joining player
     */
    public @NotNull BiPredicate<? super @NotNull Player, ? super @NotNull Player> showParticipantToJoiningPlayer() {
        return showParticipantToJoiningPlayer;
    }

    public @NotNull BiPredicate<? super @NotNull Player, ? super @NotNull Player> showJoiningPlayerToParticipant() {
        return showJoiningPlayerToParticipant;
    }

    public void setShowParticipantToJoiningPlayer(@NotNull BiPredicate<? super @NotNull Player, ? super @NotNull Player>
                                                          showParticipantToJoiningPlayer) {
        this.showParticipantToJoiningPlayer = Objects.requireNonNull(showParticipantToJoiningPlayer);
    }

    public void setShowJoiningPlayerToParticipant(@NotNull BiPredicate<? super @NotNull Player, ? super @NotNull Player>
                                                          showJoiningPlayerToParticipant) {
        this.showJoiningPlayerToParticipant = Objects.requireNonNull(showJoiningPlayerToParticipant);
    }
}