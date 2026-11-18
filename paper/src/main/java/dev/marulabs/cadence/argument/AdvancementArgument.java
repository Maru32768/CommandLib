package dev.marulabs.cadence.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.cadence.Argument;
import dev.marulabs.cadence.CommandContext;
import dev.marulabs.cadence.exception.ArgumentParseException;
import dev.marulabs.cadence.util.text.RichText;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;

@SuppressWarnings("UnstableApiUsage")
public class AdvancementArgument extends Argument<Advancement, AdvancementArgument> {
    public AdvancementArgument(String name) {
        super(name, ArgumentTypes.namespacedKey());
        addSuggestionProvider(sb -> NamespacedKeyArgumentSupport.suggestKeys(sb, Bukkit.advancementIterator()));
    }

    @Override
    public Advancement cast(Object parsedArgument) {
        return (Advancement) parsedArgument;
    }

    @Override
    protected Advancement parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        NamespacedKey key = ctx.getHandle()
                               .getArgument(name(), NamespacedKey.class);
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            throw new ArgumentParseException(x -> x.sendFailure(RichText.translatable("advancement.advancementNotFound",
                                                                                      RichText.text(key.toString()))));
        }
        return advancement;
    }
}
