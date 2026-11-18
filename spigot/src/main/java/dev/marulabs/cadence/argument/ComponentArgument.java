package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentChatComponent;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * A vanilla text component, such as {@code {"text":"Hello","color":"red"}} (JSON up to 1.21.4, SNBT from 1.21.5,
 * which also reads that JSON), as a Bungee component for {@code ctx.sendMessage} or
 * {@code player.spigot().sendMessage}. Selector, score and NBT components are returned as written, not resolved for a
 * viewer.
 *
 * <p>Up to 1.20.2 vanilla's reader also consumes the character after the component, so an argument after it fails to
 * parse there. Put it last in its branch, as {@code /tellraw} does.
 */
public class ComponentArgument extends Argument<BaseComponent, ComponentArgument> {
    public ComponentArgument(String name) {
        super(name,
              NMSArgumentChatComponent.create()
                                      .argument());
    }

    @Override
    public BaseComponent cast(Object parsedArgument) {
        return (BaseComponent) parsedArgument;
    }

    @Override
    protected BaseComponent parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NMSArgumentChatComponent.create()
                                       .parse(ctx.getHandle(), name());
    }
}
