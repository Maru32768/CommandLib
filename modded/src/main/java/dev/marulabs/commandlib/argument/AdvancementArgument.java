package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.2 {
/*import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
*///?} else
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.MinecraftServer;
//? if >=1.20.2 {
/*import net.minecraft.advancements.AdvancementHolder;
*///?} else
import net.minecraft.advancements.Advancement;

import java.util.stream.Stream;

// An advancement of the server by its key, such as minecraft:story/mine_stone; the loaded advancements are
// suggested, and an unknown key fails with vanilla's error when the command runs. The value is vanilla's: an
// AdvancementHolder from 1.20.2, which PlayerAdvancements#award takes there, and the Advancement before. From 1.21.2
// vanilla reads the key as a ResourceKey, so the argument type is ResourceKeyArgument there.
//? if >=1.20.2 {
/*public class AdvancementArgument extends Argument<AdvancementHolder, AdvancementArgument> {
*///?} else
public class AdvancementArgument extends Argument<Advancement, AdvancementArgument> {
    public AdvancementArgument(String name) {
        //? if >=1.21.2 {
        /*super(name, ResourceKeyArgument.key(Registries.ADVANCEMENT));
        *///?} else
        super(name, ResourceLocationArgument.id());
        addSuggestionProvider(sb -> RegistryEntries.suggest(sb, ids(RegistryEntries.server(sb.getContext()))));
    }

    private static Stream<ResourceLocation> ids(MinecraftServer server) {
        //? if >=1.20.2 {
        /*return server.getAdvancements()
                     .getAllAdvancements()
                     .stream()
                     .map(AdvancementHolder::id);
        *///?} else {
        return server.getAdvancements()
                     .getAllAdvancements()
                     .stream()
                     .map(Advancement::getId);
        //?}
    }

    //? if >=1.21.2 {
    /*@Override
    public AdvancementHolder cast(Object parsedArgument) {
        return (AdvancementHolder) parsedArgument;
    }

    @Override
    protected AdvancementHolder parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ResourceKeyArgument.getAdvancement(ctx.getHandle(), name());
    }
    *///?} elif >=1.20.2 {
    /*@Override
    public AdvancementHolder cast(Object parsedArgument) {
        return (AdvancementHolder) parsedArgument;
    }

    @Override
    protected AdvancementHolder parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ResourceLocationArgument.getAdvancement(ctx.getHandle(), name());
    }
    *///?} else {
    @Override
    public Advancement cast(Object parsedArgument) {
        return (Advancement) parsedArgument;
    }

    @Override
    protected Advancement parseImpl(CommandContext ctx) throws CommandSyntaxException {
        return ResourceLocationArgument.getAdvancement(ctx.getHandle(), name());
    }
    //?}
}
