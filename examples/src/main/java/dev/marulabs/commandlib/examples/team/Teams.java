package dev.marulabs.commandlib.examples.team;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The plugin's own team storage, which the team commands look teams up in.
 */
public final class Teams {
    private final Map<String, Set<String>> members = new TreeMap<>(Map.of("red",
                                                                          new TreeSet<>(),
                                                                          "blue",
                                                                          new TreeSet<>()));

    public Map<String, Set<String>> byName() {
        return members;
    }
}
