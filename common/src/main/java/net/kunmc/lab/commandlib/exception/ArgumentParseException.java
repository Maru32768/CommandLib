package net.kunmc.lab.commandlib.exception;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.kunmc.lab.commandlib.CommonCommandContext;
import net.kunmc.lab.commandlib.PlatformAdapter;
import net.kunmc.lab.commandlib.util.ChatColorUtil;

import java.util.Objects;
import java.util.function.Consumer;

public class ArgumentParseException extends Exception {
    private final Consumer<CommonCommandContext<?, ?>> sendMessages;

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static <C extends CommonCommandContext<?, ?>> Consumer<CommonCommandContext<?, ?>> buildIncorrectInputMessage(
            String argumentName,
            C ctx,
            String incorrectInput) {
        // Resolve the context lazily: validators also run while suggesting, where the argument node may not have
        // been parsed yet and the message is never sent.
        return context -> {
            PlatformAdapter platformAdapter = PlatformAdapter.get();
            String prefix = inputBefore(ctx, argumentName, incorrectInput);
            CommonCommandContext c = context;
            c.sendComponent(platformAdapter.createTranslatableComponentBuilder("command.unknown.argument")
                                           .color(Objects.requireNonNull(ChatColorUtil.RED.getRGB()))
                                           .build());
            c.sendComponent(platformAdapter.createTextComponentBuilder(ChatColorUtil.GRAY + prefix + ChatColorUtil.RED + ChatColorUtil.UNDERLINE + incorrectInput + ChatColorUtil.RESET)
                                           .append(platformAdapter.createTranslatableComponentBuilder(
                                                                          "command.context.here")
                                                                  .italic()
                                                                  .color(ChatColorUtil.RED.getRGB())
                                                                  .build())
                                           .build());
        };
    }

    private static String inputBefore(CommonCommandContext<?, ?> ctx, String argumentName, String incorrectInput) {
        String input = ctx.getHandle()
                          .getInput();
        int end = ctx.getHandle()
                     .getNodes()
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
                     .orElseGet(() -> {
                         int index = input.lastIndexOf(incorrectInput);
                         return index >= 0 ? index : input.length();
                     });

        // Player input keeps the leading slash while console input does not; strip it only when present.
        int start = input.startsWith("/") ? 1 : 0;
        String str = input.substring(Math.min(start, end), end);
        if (str.length() > 10) {
            str = "..." + str.substring(str.length() - 10);
        }
        return str;
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
