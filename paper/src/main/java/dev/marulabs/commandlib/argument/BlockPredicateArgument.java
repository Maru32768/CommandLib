package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * A block predicate in the vanilla {@code block_predicate} syntax of {@code /execute if block}: a block
 * ({@code stone}), a block tag ({@code #minecraft:logs}), with block states ({@code oak_stairs[facing=east]}) and NBT
 * ({@code chest{Items:[]}}). The client parses it and suggests blocks, tags and states.
 *
 * <p>The value tests a block in its world as the server does, loading its chunk when needed. An unknown block or tag
 * fails while parsing.
 *
 * <p>It uses Paper's {@code ArgumentTypes.blockInWorldPredicate()}, which Paper added in 1.21.8. On an older server
 * the constructor throws {@link UnsupportedOperationException}.
 */
public class BlockPredicateArgument extends Argument<Predicate<Block>, BlockPredicateArgument> {
    private static final String BLOCK_IN_WORLD_PREDICATE = "io.papermc.paper.command.brigadier.argument.predicate"
            + ".BlockInWorldPredicate";

    public BlockPredicateArgument(String name) {
        super(name, blockInWorldPredicate());
    }

    // shortcut: reflection, since the module compiles against the Paper 1.21 API; call the methods once it needs 1.21.8.
    private static ArgumentType<?> blockInWorldPredicate() {
        Method method;
        try {
            method = ArgumentTypes.class.getMethod("blockInWorldPredicate");
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("BlockPredicateArgument needs Paper 1.21.8 or later", e);
        }
        return (ArgumentType<?>) invoke(method, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<Block> cast(Object parsedArgument) {
        return (Predicate<Block>) parsedArgument;
    }

    @Override
    protected Predicate<Block> parseImpl(CommandContext ctx) {
        Object predicate = ctx.getHandle()
                              .getArgument(name(), Object.class);
        Method testBlock;
        Method asBoolean;
        try {
            testBlock = Class.forName(BLOCK_IN_WORLD_PREDICATE, false, ArgumentTypes.class.getClassLoader())
                             .getMethod("testBlock", Block.class, boolean.class);
            asBoolean = testBlock.getReturnType()
                                 .getMethod("asBoolean");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Paper's BlockInWorldPredicate has no testBlock(Block, boolean)", e);
        }
        return block -> (Boolean) invoke(asBoolean, invoke(testBlock, predicate, block, true));
    }

    private static Object invoke(Method method, @Nullable Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not call " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Could not call " + method, cause);
        }
    }
}
