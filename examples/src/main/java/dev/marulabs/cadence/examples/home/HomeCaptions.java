package dev.marulabs.cadence.examples.home;

import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.Captions;

/**
 * The home commands' messages: English defaults here, other languages in {@code lang/homes_<locale>.properties}.
 */
public final class HomeCaptions {
    public static final Caption DESCRIPTION = Caption.of("example.home.description", "Manage your homes");
    public static final Caption SET = Caption.of("example.home.set", "Home {name} set.");
    public static final Caption TELEPORTED = Caption.of("example.home.teleported", "Teleported to {name}.");
    public static final Caption UNKNOWN = Caption.of("example.home.unknown", "You have no home named {name}.");

    private HomeCaptions() {
    }

    public static Captions create() {
        return Captions.create()
                       .bundle("lang/homes", HomeCaptions.class.getClassLoader());
    }
}
