package dev.marulabs.commandlib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FakeSender implements CommandActor {
    private final String name;
    private final @Nullable UUID uniqueId;
    private final CommandActorType type;
    private final boolean operator;
    private final boolean allowAllPermissions;
    private final Set<String> permissions;
    private final Set<String> deniedPermissions;
    private final @Nullable String locale;
    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> sentMessageLegacyTexts = new ArrayList<>();

    public static FakeSender player(String name) {
        return new FakeSender(name, UUID.randomUUID(), CommandActorType.PLAYER, false, true, Set.of(), Set.of(), null);
    }

    public static FakeSender player(String name, UUID uniqueId) {
        return new FakeSender(name, uniqueId, CommandActorType.PLAYER, false, true, Set.of(), Set.of(), null);
    }

    public static FakeSender console() {
        return new FakeSender("Console", null, CommandActorType.CONSOLE, true, true, Set.of(), Set.of(), null);
    }

    public static FakeSender unknown() {
        return new FakeSender("test", null, CommandActorType.UNKNOWN, false, false, Set.of(), Set.of(), null);
    }

    /**
     * Creates a builder for a non-operator player that has every permission.
     */
    public static Builder playerBuilder(String name) {
        return new Builder(name, UUID.randomUUID(), CommandActorType.PLAYER, false);
    }

    /**
     * Creates a builder for the console, which is an operator and has every permission.
     */
    public static Builder consoleBuilder() {
        return new Builder("Console", null, CommandActorType.CONSOLE, true);
    }

    private FakeSender(String name,
                       @Nullable UUID uniqueId,
                       CommandActorType type,
                       boolean operator,
                       boolean allowAllPermissions,
                       Set<String> permissions,
                       Set<String> deniedPermissions,
                       @Nullable String locale) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.type = type;
        this.operator = operator;
        this.allowAllPermissions = allowAllPermissions;
        this.permissions = Set.copyOf(permissions);
        this.deniedPermissions = Set.copyOf(deniedPermissions);
        this.locale = locale;
    }

    public FakeSender op(boolean operator) {
        return new FakeSender(name,
                              uniqueId,
                              type,
                              operator,
                              allowAllPermissions,
                              permissions,
                              deniedPermissions,
                              locale);
    }

    public FakeSender uniqueId(UUID uniqueId) {
        return new FakeSender(name,
                              uniqueId,
                              type,
                              operator,
                              allowAllPermissions,
                              permissions,
                              deniedPermissions,
                              locale);
    }

    public FakeSender locale(String locale) {
        return new FakeSender(name,
                              uniqueId,
                              type,
                              operator,
                              allowAllPermissions,
                              permissions,
                              deniedPermissions,
                              locale);
    }

    public FakeSender permissions(String... permissions) {
        return new FakeSender(name,
                              uniqueId,
                              type,
                              operator,
                              false,
                              Set.copyOf(Arrays.asList(permissions)),
                              deniedPermissions,
                              locale);
    }

    public FakeSender denyPermissions(String... permissions) {
        Set<String> denied = new HashSet<>(deniedPermissions);
        denied.addAll(Arrays.asList(permissions));
        return new FakeSender(name, uniqueId, type, operator, allowAllPermissions, this.permissions, denied, locale);
    }

    @Nullable
    public String getLocale() {
        return locale;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return !deniedPermissions.contains(permission) && (allowAllPermissions || permissions.contains(permission));
    }

    void sendMessage(String message) {
        String value = String.valueOf(message);
        sentMessageLegacyTexts.add(value);
        sentMessages.add(value.replaceAll("(?i)\u00a7[0-9A-FK-ORX]", ""));
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public List<String> getSentMessageTexts() {
        return getSentMessages();
    }

    public List<String> getSentMessageLegacyTexts() {
        return Collections.unmodifiableList(sentMessageLegacyTexts);
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public Optional<UUID> getUniqueId() {
        return Optional.ofNullable(uniqueId);
    }

    @Override
    @NotNull
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
    @NotNull
    public <T> Optional<T> unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }
        return Optional.empty();
    }

    public static final class Builder {
        private String name;
        private @Nullable UUID uniqueId;
        private CommandActorType type;
        private boolean operator;
        private boolean allowAllPermissions = true;
        private Set<String> permissions = Set.of();
        private final Set<String> deniedPermissions = new HashSet<>();
        private @Nullable String locale;

        private Builder(String name, @Nullable UUID uniqueId, CommandActorType type, boolean operator) {
            this.name = name;
            this.uniqueId = uniqueId;
            this.type = type;
            this.operator = operator;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder uniqueId(UUID uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        public Builder type(CommandActorType type) {
            this.type = type;
            return this;
        }

        public Builder op(boolean operator) {
            this.operator = operator;
            return this;
        }

        /**
         * Grants only the given permission nodes, replacing any earlier call.
         */
        public Builder permissions(String... permissions) {
            this.allowAllPermissions = false;
            this.permissions = Set.copyOf(Arrays.asList(permissions));
            return this;
        }

        /**
         * Denies the given permission nodes, even to operators. Calls accumulate.
         */
        public Builder denyPermissions(String... permissions) {
            deniedPermissions.addAll(Arrays.asList(permissions));
            return this;
        }

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public FakeSender build() {
            return new FakeSender(name,
                                  uniqueId,
                                  type,
                                  operator,
                                  allowAllPermissions,
                                  permissions,
                                  deniedPermissions,
                                  locale);
        }
    }
}
