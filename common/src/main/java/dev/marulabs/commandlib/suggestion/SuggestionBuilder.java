package dev.marulabs.commandlib.suggestion;

import com.mojang.brigadier.Message;
import dev.marulabs.commandlib.CommonCommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class SuggestionBuilder<C extends CommonCommandContext<?, ?>> {
    private final List<Suggestion> suggestions = Collections.synchronizedList(new ArrayList<>());
    private final List<CompletionStage<?>> awaitedStages = Collections.synchronizedList(new ArrayList<>());
    private final C ctx;
    private final String latestInput;

    public SuggestionBuilder(@NotNull C ctx, @NotNull String latestInput) {
        this.ctx = Objects.requireNonNull(ctx);
        this.latestInput = latestInput;
    }

    @NotNull
    public C getContext() {
        return ctx;
    }

    public String getInput() {
        return ctx.getHandle()
                  .getInput();
    }

    @NotNull
    public String getLatestInput() {
        return latestInput;
    }

    public List<String> getInputs() {
        return ctx.getInputs();
    }

    public String getInput(int index) {
        return ctx.getInput(index);
    }

    public String getInput(String name) {
        return ctx.getInput(name);
    }

    public List<Object> getArguments() {
        return ctx.getArguments();
    }

    public Object getArgument(int index) {
        return ctx.getArgument(index);
    }

    public Object getArgument(String name) {
        return ctx.getArgument(name);
    }

    public <T> T getArgument(int index, Class<T> clazz) {
        return ctx.getArgument(index, clazz);
    }

    public <T> T getArgument(String name, Class<T> clazz) {
        return ctx.getArgument(name, clazz);
    }

    public void sendMessage(String message) {
        ctx.sendMessage(message);
    }

    public void sendSuccess(String message) {
        ctx.sendSuccess(message);
    }

    public void sendWarn(String message) {
        ctx.sendWarn(message);
    }

    public void sendFailure(String message) {
        ctx.sendFailure(message);
    }

    public SuggestionBuilder<C> suggest(@NotNull String suggest) {
        return suggest(suggest, (String) null);
    }

    public SuggestionBuilder<C> suggest(@NotNull String suggest, @Nullable String tooltip) {
        suggestions.add(new Suggestion(suggest, tooltip));
        return this;
    }

    public SuggestionBuilder<C> suggest(@NotNull String suggest, @NotNull Message tooltipMessage) {
        suggestions.add(new Suggestion(suggest, tooltipMessage));
        return this;
    }

    public SuggestionBuilder<C> await(@NotNull CompletionStage<?> stage) {
        awaitedStages.add(Objects.requireNonNull(stage));
        return this;
    }

    public CompletionStage<Void> awaitAll() {
        List<CompletableFuture<?>> futures;
        synchronized (awaitedStages) {
            futures = awaitedStages.stream()
                                   .map(CompletionStage::toCompletableFuture)
                                   .collect(Collectors.toList());
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public List<Suggestion> build() {
        synchronized (suggestions) {
            return List.copyOf(suggestions);
        }
    }
}
