package dev.marulabs.commandlib.exception;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.internal.PlatformAdapter;
import dev.marulabs.commandlib.util.ChatColorUtil;
import dev.marulabs.commandlib.util.text.RichText;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ArgumentParseException extends Exception {
    private static final int RED = Objects.requireNonNull(ChatColorUtil.RED.getRGB());
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
            context.sendMessage(RichText.translatable("command.unknown.argument")
                                        .color(RED));
            context.sendMessage(contextLine(prefix, incorrectInput));
        };
    }

    /**
     * The line vanilla shows under a command error: the input before the error, the rejected input, and
     * {@code <--[HERE]}.
     */
    private static RichText contextLine(String before, String incorrectInput) {
        return RichText.text(ChatColorUtil.GRAY + before + ChatColorUtil.RED + ChatColorUtil.UNDERLINE + incorrectInput
                + ChatColorUtil.RESET)
                       .append(RichText.translatable("command.context.here")
                                       .italic()
                                       .color(RED));
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

        return abbreviateBefore(input, end);
    }

    private static String abbreviateBefore(String input, int end) {
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

    protected static String incorrectInputText(String argumentName, String incorrectInput) {
        return "Incorrect input for <" + argumentName + ">: '" + incorrectInput + "'";
    }

    public static <C extends CommonCommandContext<?, ?>> ArgumentParseException ofIncorrectInput(String argumentName,
                                                                                                 C ctx,
                                                                                                 String incorrectInput) {
        return new ArgumentParseException(incorrectInputText(argumentName, incorrectInput),
                                          buildIncorrectInputMessage(argumentName, ctx, incorrectInput));
    }

    /**
     * Converts a {@link CommandSyntaxException} thrown while an argument is resolved into the messages vanilla sends
     * for it: the platform's form of its message, which keeps a translatable message translatable, then the context
     * line when the exception carries its input.
     *
     * @param e the exception thrown while resolving the argument
     * @return the exception sending the converted messages
     */
    public static ArgumentParseException ofCommandSyntaxException(CommandSyntaxException e) {
        ArgumentParseException message = PlatformAdapter.get()
                                                        .convertCommandSyntaxException(e);
        String input = e.getInput();
        RichText context = null;
        if (input != null && e.getCursor() >= 0) {
            int cursor = Math.min(input.length(), e.getCursor());
            context = contextLine(abbreviateBefore(input, cursor), input.substring(cursor));
        }
        RichText line = context;
        ArgumentParseException converted = new ArgumentParseException(String.valueOf(e.getMessage()), ctx -> {
            message.sendMessage(ctx);
            if (line != null) {
                ctx.sendMessage(line);
            }
        });
        converted.initCause(e);
        return converted;
    }

    public ArgumentParseException(String message, String... messages) {
        this(message, ctx -> {
            ctx.sendFailure(message);
            for (String s : messages) {
                ctx.sendFailure(s);
            }
        });
    }

    /**
     * Rejects the argument with {@code caption} in the sender's locale with its placeholders replaced, see
     * {@link CommonCommandContext#sendFailure(Caption, Map)}.
     */
    public ArgumentParseException(Caption caption, Map<String, ?> placeholders) {
        this(caption.key() + " " + placeholders, ctx -> ctx.sendFailure(caption, placeholders));
    }

    public ArgumentParseException(Consumer<CommonCommandContext<?, ?>> sendMessages) {
        this.sendMessages = sendMessages;
    }

    // Gives getMessage() a text for logs and handlers; the sender sees what sendMessages sends.
    protected ArgumentParseException(String message, Consumer<CommonCommandContext<?, ?>> sendMessages) {
        super(message);
        this.sendMessages = sendMessages;
    }

    public void sendMessage(CommonCommandContext<?, ?> ctx) {
        sendMessages.accept(ctx);
    }
}
