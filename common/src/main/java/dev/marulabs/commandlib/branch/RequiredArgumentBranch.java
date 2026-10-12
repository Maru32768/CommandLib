package dev.marulabs.commandlib.branch;

import dev.marulabs.commandlib.CommonCommand;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.command.CommandExecutor;
import dev.marulabs.commandlib.command.Extractor;
import dev.marulabs.commandlib.command.Prerequisite;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RequiredArgumentBranch<S, C extends CommonCommandContext<?, ?>, T extends CommonCommand<C, T>> {
    private final Extractor<C, S> extractor;
    // Holding a CommonCommand directly would cause stamp coupling and would not work uniformly:
    // command.require() registers children on CommonCommand, while argument().require() registers them
    // on the Arguments object via ArgumentBranchDelegate. Consumers abstract over both cases.
    private final Consumer<CommandExecutor<C>> executeRegistrar;
    private final Consumer<Collection<? extends T>> childrenAdder;

    public RequiredArgumentBranch(@NotNull Extractor<C, S> extractor,
                                  @NotNull Consumer<CommandExecutor<C>> executeRegistrar,
                                  @NotNull Consumer<Collection<? extends T>> childrenAdder) {
        this.extractor = Objects.requireNonNull(extractor);
        this.executeRegistrar = Objects.requireNonNull(executeRegistrar);
        this.childrenAdder = Objects.requireNonNull(childrenAdder);
    }

    public RequiredArgumentBranch<S, C, T> execute(@NotNull BiConsumer<S, C> action) {
        Objects.requireNonNull(action);
        executeRegistrar.accept(ctx -> {
            S s = extractor.extract(ctx);
            action.accept(s, ctx);
        });
        return this;
    }

    @SafeVarargs
    public final RequiredArgumentBranch<S, C, T> child(@NotNull T child, @NotNull T... children) {
        List<T> list = new ArrayList<>();
        list.add(child);
        list.addAll(Arrays.asList(children));
        Prerequisite<C> prerequisite = extractor::extract;
        list.forEach(c -> c.addPrerequisite(prerequisite));
        childrenAdder.accept(list);
        return this;
    }
}
