package dev.marulabs.commandlib;

import org.bukkit.permissions.Permission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The steps that remove a registration, shared by the Spigot and Paper {@code CommandLib}.
 */
final class RegistrationCleanup {
    private RegistrationCleanup() {
    }

    /**
     * Runs every step, also after one throws, since unregister() cannot be retried, and then throws the first failure,
     * the root cause, with the later ones suppressed.
     */
    static void runAll(Runnable... steps) {
        Throwable failure = null;
        for (Runnable step : steps) {
            try {
                step.run();
            } catch (RuntimeException | Error e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure != null) {
            throw (RuntimeException) failure;
        }
    }

    /**
     * Adds each removed node that a live registration still declares again, as the oldest of them declares it, and
     * makes that registration own it, so its commands keep their default until it is unregistered itself. Its own
     * permission object was given the default FALSE when a later registration replaced it.
     *
     * @param live     the live registrations of this copy of CommandLib, oldest first
     * @param declared the permissions of a registration's commands
     * @param owned    the permissions a registration has registered, which it removes when it is unregistered
     * @param add      adds a permission, replacing one of the same name
     */
    static <R> void restorePermissions(Set<String> removed,
                                       Iterable<R> live,
                                       Function<R, List<Permission>> declared,
                                       Function<R, List<Permission>> owned,
                                       Consumer<Permission> add) {
        Set<String> remaining = new HashSet<>(removed);
        for (R registration : live) {
            if (remaining.isEmpty()) {
                return;
            }
            for (Permission permission : declared.apply(registration)) {
                if (remaining.remove(permission.getName())) {
                    add.accept(permission);
                    List<Permission> registered = owned.apply(registration);
                    registered.removeIf(x -> x.getName()
                                              .equals(permission.getName()));
                    registered.add(permission);
                }
            }
        }
    }
}
