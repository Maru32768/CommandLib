package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentNamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;

public class EntityTypeArgument extends Argument<EntityType, EntityTypeArgument> {
    public EntityTypeArgument(String name) {
        super(name,
              NMSArgumentNamespacedKey.create()
                                      .argument());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Registry.ENTITY_TYPE));
    }

    @Override
    public EntityType cast(Object parsedArgument) {
        return (EntityType) parsedArgument;
    }

    @Override
    protected EntityType parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return NamespacedKeyArgumentSupport.parseRegistryValue(ctx,
                                                               name(),
                                                               Registry.ENTITY_TYPE,
                                                               Caption.UNKNOWN_ENTITY_TYPE);
    }
}
