package net.minestom.server.event.instance;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlockChangeEvent implements BlockEvent, InstanceEvent {
    private final Vec position;
    private final Block oldBlock;
    private final Block newBlock;
    private final Instance instance;

    public BlockChangeEvent(@NotNull Vec position, @NotNull Block oldBlock, @NotNull Block newBlock,
                            @NotNull Instance instance) {
        this.position = Objects.requireNonNull(position, "position");
        this.oldBlock = Objects.requireNonNull(oldBlock, "oldBlock");
        this.newBlock = Objects.requireNonNull(newBlock, "newBlock");
        this.instance = Objects.requireNonNull(instance, "instance");
    }

    public @NotNull Vec blockPosition() {
        return position;
    }

    public @NotNull Block getOldBlock() {
        return oldBlock;
    }

    @Override
    public @NotNull Block getBlock() {
        return newBlock;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }
}
