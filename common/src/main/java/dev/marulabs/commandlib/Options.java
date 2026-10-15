package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.*;
import org.jetbrains.annotations.NotNull;

public final class Options {
    private Options() {
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> flag(@NotNull String name) {
        return CommandOption.flag(name, null);
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> flag(@NotNull String name,
                                                                                        char shortName) {
        return CommandOption.flag(name, shortName);
    }

    /**
     * Returns a flag whose value is how many times it is given, such as 3 for {@code -vvv} or
     * {@code -v --verbose -v}, and 0 when it is not given. It can be given up to {@code maxCount} times.
     */
    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> count(@NotNull String name,
                                                                                         int maxCount) {
        return CommandOption.counter(name, null, maxCount);
    }

    /**
     * Returns a flag whose value is how many times it is given, such as 3 for {@code -vvv} or
     * {@code -v --verbose -v}, and 0 when it is not given. It can be given up to {@code maxCount} times.
     */
    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> count(@NotNull String name,
                                                                                         char shortName,
                                                                                         int maxCount) {
        return CommandOption.counter(name, shortName, maxCount);
    }

    /**
     * Returns an option whose value any CommandLib argument parses, such as
     * {@code Options.of("target", new PlayerArgument("target"))} for {@code --target Steve}. The value gets the
     * argument's type, suggestions, validator and transformer. When the option is not given, the default of an
     * {@linkplain CommonArgument#optional(Object) optional} argument applies; the option of an argument that is not
     * optional is {@linkplain CommandOption#required() required}.
     *
     * @throws IllegalArgumentException if the argument is variadic; use {@link CommandOption#repeatable(int)} instead
     */
    public static <T, C extends CommonCommandContext<?, ?>> CommandOption<T, C> of(@NotNull String name,
                                                                                   @NotNull CommonArgument<T, C, ?> argument) {
        return CommandOption.of(name, null, argument);
    }

    /**
     * Same as {@link #of(String, CommonArgument)}, with a short name.
     */
    public static <T, C extends CommonCommandContext<?, ?>> CommandOption<T, C> of(@NotNull String name,
                                                                                   char shortName,
                                                                                   @NotNull CommonArgument<T, C, ?> argument) {
        return CommandOption.of(name, shortName, argument);
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> bool(@NotNull String name,
                                                                                        boolean defaultValue) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   BoolArgumentType.bool(),
                                   (ctx, optionName) -> BoolArgumentType.getBool(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> bool(@NotNull String name,
                                                                                        char shortName,
                                                                                        boolean defaultValue) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   BoolArgumentType.bool(),
                                   (ctx, optionName) -> BoolArgumentType.getBool(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> integer(@NotNull String name,
                                                                                           int defaultValue) {
        return integer(name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> integer(@NotNull String name,
                                                                                           char shortName,
                                                                                           int defaultValue) {
        return integer(name, shortName, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> integer(@NotNull String name,
                                                                                           int defaultValue,
                                                                                           int min,
                                                                                           int max) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   IntegerArgumentType.integer(min, max),
                                   (ctx, optionName) -> IntegerArgumentType.getInteger(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Integer, C> integer(@NotNull String name,
                                                                                           char shortName,
                                                                                           int defaultValue,
                                                                                           int min,
                                                                                           int max) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   IntegerArgumentType.integer(min, max),
                                   (ctx, optionName) -> IntegerArgumentType.getInteger(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Long, C> longValue(@NotNull String name,
                                                                                          long defaultValue) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   LongArgumentType.longArg(Long.MIN_VALUE, Long.MAX_VALUE),
                                   (ctx, optionName) -> LongArgumentType.getLong(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Long, C> longValue(@NotNull String name,
                                                                                          char shortName,
                                                                                          long defaultValue) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   LongArgumentType.longArg(Long.MIN_VALUE, Long.MAX_VALUE),
                                   (ctx, optionName) -> LongArgumentType.getLong(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Float, C> floatValue(@NotNull String name,
                                                                                            float defaultValue) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   FloatArgumentType.floatArg(-Float.MAX_VALUE, Float.MAX_VALUE),
                                   (ctx, optionName) -> FloatArgumentType.getFloat(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Float, C> floatValue(@NotNull String name,
                                                                                            char shortName,
                                                                                            float defaultValue) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   FloatArgumentType.floatArg(-Float.MAX_VALUE, Float.MAX_VALUE),
                                   (ctx, optionName) -> FloatArgumentType.getFloat(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Double, C> doubleValue(@NotNull String name,
                                                                                              double defaultValue) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   DoubleArgumentType.doubleArg(-Double.MAX_VALUE, Double.MAX_VALUE),
                                   (ctx, optionName) -> DoubleArgumentType.getDouble(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Double, C> doubleValue(@NotNull String name,
                                                                                              char shortName,
                                                                                              double defaultValue) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   DoubleArgumentType.doubleArg(-Double.MAX_VALUE, Double.MAX_VALUE),
                                   (ctx, optionName) -> DoubleArgumentType.getDouble(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<String, C> string(@NotNull String name,
                                                                                         @NotNull String defaultValue) {
        return new CommandOption<>(name,
                                   null,
                                   defaultValue,
                                   StringArgumentType.string(),
                                   (ctx, optionName) -> StringArgumentType.getString(ctx.getHandle(), optionName));
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<String, C> string(@NotNull String name,
                                                                                         char shortName,
                                                                                         @NotNull String defaultValue) {
        return new CommandOption<>(name,
                                   shortName,
                                   defaultValue,
                                   StringArgumentType.string(),
                                   (ctx, optionName) -> StringArgumentType.getString(ctx.getHandle(), optionName));
    }
}
