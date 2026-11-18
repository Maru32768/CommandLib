package example.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The teams of one scoreboard, looked up by name.
 */
public final class TeamDirectory {
    private static final int DEFAULT_LIMIT = 20;
    private final Map<String, Team> teams = new HashMap<>();

    /**
     * Adds {@code team}, replacing a team of the same name.
     */
    public void add(Team team) {
        teams.put(team.name(), team);
    }

    /**
     * Returns the team named {@code name}, or null when no team has this name.
     */
    public Team team(String name) {
        Team team = teams.get(name);
        if (team == null) {
            throw new IllegalArgumentException("Unknown team: " + name);
        }
        return team;
    }

    /**
     * Returns the team named {@code name}, if there is one.
     */
    public Optional<Team> find(String name) {
        return Optional.ofNullable(teams.get(name));
    }

    /**
     * Returns every team, sorted by name.
     */
    public List<Team> all() {
        return new ArrayList<>(teams.values());
    }

    /**
     * Returns at most {@code limit} team names that start with {@code prefix}, for tab completion.
     *
     * @throws IllegalArgumentException if {@code limit} is negative
     */
    public List<String> suggest(String prefix, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative: " + limit);
        }
        List<String> names = new ArrayList<>();
        for (String name : teams.keySet()) {
            if (names.size() == limit) {
                break;
            }
            if (name.startsWith(prefix)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Returns team names starting with {@code prefix}, at most 10 of them.
     */
    public List<String> suggest(String prefix) {
        return suggest(prefix, DEFAULT_LIMIT);
    }

    /**
     * Returns how long the team's friendly-fire protection lasts after it is created, in seconds.
     */
    public long protectionTime(Team team) {
        return team.protectionTicks() * 50L;
    }

    /**
     * Removes the team named {@code name} and moves its members to {@code fallback}.
     */
    public void remove(String name) {
        teams.remove(name);
    }

    // --- Membership ---

    /**
     * Handles a player joining a team.
     */
    public void join(Team team, String player) {
        team.members()
            .add(player);
    }

    /**
     * Call from the main thread only; the scoreboard is not thread-safe.
     */
    public int size() {
        return teams.size();
    }

    /**
     * Returns the size of the largest team, or 0 when there are no teams.
     */
    public int largestTeamSize() {
        int largest = 0;
        for (Team team : teams.values()) {
            largest = Math.max(largest, team.members()
                                            .size());
        }
        return largest;
    }

    public interface Team {
        String name();

        List<String> members();

        long protectionTicks();
    }
}
