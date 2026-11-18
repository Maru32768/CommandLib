package example.cooldown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CooldownRegistry {
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Map<String, UUID> namesToIds = new HashMap<>();
    private final long durationMillis;
    private final Clock clock;

    public CooldownRegistry(long durationMillis, Clock clock) {
        this.durationMillis = durationMillis;
        this.clock = clock;
    }

    public long durationMillis() {
        return durationMillis;
    }

    public Long get(UUID playerId) {
        return lastUse.get(playerId);
    }

    public boolean isOnCooldown(UUID playerId) {
        long now = clock.millis();
        Long last = lastUse.get(playerId);
        lastUse.put(playerId, now);
        return last != null && now - last < durationMillis;
    }

    public long remaining(UUID playerId) {
        Long last = lastUse.get(playerId);
        if (last == null) {
            return 0;
        }
        return Math.max(0, durationMillis - (clock.millis() - last));
    }

    public int countActive() {
        return lastUse.size();
    }

    public void clearExpired() {
        lastUse.clear();
    }

    public UUID findPlayer(String name) {
        return namesToIds.computeIfAbsent(normalize(name), key -> UUID.randomUUID());
    }

    public void ensureEntry(UUID playerId) {
        lastUse.putIfAbsent(playerId, 0L);
    }

    public boolean retryRegister(String name, UUID playerId, int attempts) {
        String key = normalize(name);
        if (namesToIds.containsKey(key)) {
            return false;
        }
        namesToIds.put(key, playerId);
        return true;
    }

    public List<UUID> activePlayers() {
        long now = clock.millis();
        List<UUID> active = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : lastUse.entrySet()) {
            if (now - entry.getValue() < durationMillis) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    public void apply(UUID playerId) {
        lastUse.put(playerId, clock.millis());
    }

    private static String normalize(String name) {
        return name.trim()
                   .toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CooldownRegistry{" + lastUse.size() + " players, " + durationMillis + "ms}";
    }

    public interface Clock {
        long millis();
    }
}
