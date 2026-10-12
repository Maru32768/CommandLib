package dev.marulabs.commandlib;

import com.mojang.brigadier.arguments.*;
import org.jetbrains.annotations.NotNull;

public final class Options {
    private Options() {
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> flag(@NotNull String name) {
        return new CommandOption<>(name, null, false);
    }

    public static <C extends CommonCommandContext<?, ?>> CommandOption<Boolean, C> flag(@NotNull String name,
                                                                                        char shortName) {
        return new CommandOption<>(name, shortName, false);
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
