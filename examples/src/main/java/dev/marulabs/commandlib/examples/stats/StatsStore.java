package dev.marulabs.commandlib.examples.stats;

/**
 * A blocking store, such as a database, that must not be queried on the server thread.
 */
@FunctionalInterface
public interface StatsStore {
    int kills(String player);
}
