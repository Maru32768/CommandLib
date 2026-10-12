package dev.marulabs.commandlib.argument;

import java.util.*;
import java.util.stream.Collectors;

public interface Nameable {
    static <T extends Nameable> Map<String, T> toMap(T object, T... objects) {
        List<T> list = new ArrayList<>();
        list.add(object);
        Collections.addAll(list, objects);
        return toMap(list);
    }

    static <T extends Nameable> Map<String, T> toMap(Collection<? extends T> objects) {
        return objects.stream()
                      .collect(Collectors.toMap(Nameable::tabCompleteName, x -> x));
    }

    String tabCompleteName();
}
