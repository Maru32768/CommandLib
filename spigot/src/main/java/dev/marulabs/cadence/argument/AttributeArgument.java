package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.Caption;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.internal.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public class AttributeArgument extends Argument<Attribute, AttributeArgument> {
    public AttributeArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Registry.ATTRIBUTE));
    }

    @Override
    public Attribute cast(Object parsedArgument) {
        return (Attribute) parsedArgument;
    }

    @Override
    protected Attribute parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx,
                                                               name(),
                                                               Registry.ATTRIBUTE,
                                                               Caption.UNKNOWN_ATTRIBUTE);
    }
}
