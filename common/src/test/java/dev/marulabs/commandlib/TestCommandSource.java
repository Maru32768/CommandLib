package dev.marulabs.commandlib;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class TestCommandSource implements CommandActor {
    private final String name;
    private final UUID uniqueId;
    private final CommandActorType type;
    private final boolean operator;
    private final Set<String> permissions;

    TestCommandSource(String... permissions) {
        this("test", null, CommandActorType.UNKNOWN, false, Set.of(permissions));
    }

    TestCommandSource(String name, UUID uniqueId, CommandActorType type, boolean operator, Set<String> permissions) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.type = type;
        this.operator = operator;
        this.permissions = Set.copyOf(permissions);
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<UUID> getUniqueId() {
        return Optional.ofNullable(uniqueId);
    }

    @Override
    public CommandActorType getType() {
        return type;
    }

    @Override
    public boolean isConsole() {
        return type == CommandActorType.CONSOLE || type == CommandActorType.REMOTE_CONSOLE;
    }

    @Override
    public boolean isPlayer() {
        return type == CommandActorType.PLAYER;
    }

    @Override
    public boolean isOperator() {
        return operator;
    }

    @Override
    public <T> Optional<T> unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }
}
