package net.minestom.server.event.player;

import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class PreSendChunkEvent implements InstanceEvent {
    private Chunk chunk;

    public PreSendChunkEvent(@NotNull Chunk chunk) {
        this.chunk = chunk;
    }

    public @NotNull Chunk chunk() {
        return chunk;
    }

    public void setChunk(@NotNull Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public @NotNull Instance getInstance() {
        return chunk.getInstance();
    }
}
