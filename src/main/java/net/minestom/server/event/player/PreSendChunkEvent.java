package net.minestom.server.event.player;

import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class PreSendChunkEvent implements InstanceEvent {
    private DynamicChunk chunk;

    public PreSendChunkEvent(@NotNull DynamicChunk chunk) {
        this.chunk = chunk;
    }

    public @NotNull DynamicChunk chunk() {
        return chunk;
    }

    public void setChunk(@NotNull DynamicChunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public @NotNull Instance getInstance() {
        return chunk.getInstance();
    }
}
