package net.minestom.server.event.instance;

import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public class InstanceRegisterEvent implements InstanceEvent {
    private final Instance instance;

    public InstanceRegisterEvent(@NotNull Instance instance) {
        this.instance = Objects.requireNonNull(instance, "instance");
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }
}
