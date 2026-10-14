package dev.marulabs.commandlib.exception;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ArgumentParseException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> sendMessages;

    protected static <C extends CommonCommandContext<?, ?>> Consumer<CommonCommandContext<?, ?>> buildIncorrectInputMessage(String argumentName,
                                                                                                                            C ctx,
                                                                                                                            String incorrectInput) {
        // Resolve the input lazily: validators also run while suggesting, where the argument node may not have
        // been parsed yet and the message is never sent. Keep the handle now, since a variadic argument replaces it
        // only while it parses one value.
        CommandContext<?> handle = ctx.getHandle();
        return context -> {
            String prefix = inputBefore(handle, argumentName, incorrectInput);
            int red = Objects.requireNonNull(ChatColorUtil.RED.getRGB());
            context.sendMessage(RichText.translatable("command.unknown.argument")
                                        .color(red));
            context.sendMessage(RichText.text(ChatColorUtil.GRAY + prefix + ChatColorUtil.RED + ChatColorUtil.UNDERLINE
                    + incorrectInput + ChatColorUtil.RESET)
                                        .append(RichText.translatable("command.context.here")
                                                        .italic()
                                                        .color(red)));
        };
    }

    private static String inputBefore(CommandContext<?> handle, String argumentName, String incorrectInput) {
        String input = handle.getInput();
        int end = handle.getNodes()
                        .stream()
                        // A literal such as the command itself may share the argument's name.
                        .filter(n -> n.getNode() instanceof ArgumentCommandNode)
                        .filter(n -> n.getNode()
                                      .getName()
                                      .equals(argumentName))
                        .findFirst()
                        .map(n -> n.getRange()
                                   .getStart())
                        // Without a matching node, cut the input before the incorrect token so that it is not shown twice.
                        .orElseGet(() -> incorrectTokenStart(handle, input, incorrectInput));

        // Player input keeps the leading slash while console input does not; strip it only when present.
        int start = input.startsWith("/") ? 1 : 0;
        String str = input.substring(Math.min(start, end), end);
        if (str.length() > 10) {
            str = "..." + str.substring(str.length() - 10);
        }
        return str;
    }

    private static int incorrectTokenStart(CommandContext<?> handle, String input, String incorrectInput) {
        // The incorrect token follows the nodes parsed so far, so search from their end. Searching the whole input
        // would pick another occurrence of the same text.
        List<? extends ParsedCommandNode<?>> nodes = handle.getNodes();
        int parsedEnd = nodes.isEmpty() ? 0 : Math.min(nodes.get(nodes.size() - 1)
                                                            .getRange()
                                                            .getEnd(),
                                                       input.length());
        if (incorrectInput.isEmpty()) {
            return parsedEnd;
        }
        int index = input.indexOf(incorrectInput, parsedEnd);
        if (index < 0) {
            index = input.lastIndexOf(incorrectInput);
        }
        return index >= 0 ? index : parsedEnd;
    }

    public static <C extends CommonCommandContext<?, ?>> ArgumentParseException ofIncorrectInput(String argumentName,
                                                                                                 C ctx,
                                                                                                 String incorrectInput) {
        return new ArgumentParseException(buildIncorrectInputMessage(argumentName, ctx, incorrectInput));
    }

    public ArgumentParseException(String message, String... messages) {
        this(ctx -> {
            ctx.sendFailure(message);
            for (String s : messages) {
                ctx.sendFailure(s);
            }
        });
    }

    public ArgumentParseException(Consumer<CommonCommandContext<?, ?>> sendMessages) {
        this.sendMessages = sendMessages;
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessages.accept(ctx);
    }
}
