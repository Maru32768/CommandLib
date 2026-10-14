package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
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
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx, name(), Registry.ATTRIBUTE, "attribute");
    }
}
