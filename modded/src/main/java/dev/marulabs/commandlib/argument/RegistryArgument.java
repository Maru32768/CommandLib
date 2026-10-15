package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Argument;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.util.StringUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * A value of a registry of the server by its key, such as {@code minecraft:diamond} for
 * {@code new RegistryArgument<>("item", Registries.ITEM)} ({@code Registry.ITEM_REGISTRY} before 1.19.3). Input without a
 * namespace is in {@code minecraft}, and the registry's keys are suggested. The registry is looked up in the server's
 * registries, so data-driven ones such as biomes work. An unknown key fails when the command runs.
 */
public class RegistryArgument<T> extends Argument<T, RegistryArgument<T>> {
    private final ResourceKey<? extends Registry<T>> registryKey;

    public RegistryArgument(String name, ResourceKey<? extends Registry<T>> registryKey) {
        super(name, net.minecraft.commands.arguments.ResourceLocationArgument.id());
        this.registryKey = registryKey;
        addSuggestionProvider(sb -> {
            String input = sb.getLatestInput();
            registry(sb.getContext()
                       .getHandle()
                       .getSource()
                       .getServer()).keySet()
                                    .stream()
                                    .map(ResourceLocation::toString)
                                    .filter(x -> input.isEmpty() || StringUtil.containsIgnoreCase(x, input))
                                    .forEach(sb::suggest);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public T cast(Object parsedArgument) {
        return (T) parsedArgument;
    }

    @Override
    protected T parseImpl(CommandContext ctx) throws ArgumentParseException {
        ResourceLocation key = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx.getHandle(), name());
        // getOptional, unlike get, does not return the default value of a defaulted registry such as items.
        return registry(ctx.getHandle()
                           .getSource()
                           .getServer()).getOptional(key)
                                        .orElseThrow(() -> new ArgumentParseException(x -> x.sendFailure("Can't find element '"
                                                + key + "' of type '" + registryKey.location() + "'")));
    }

    @SuppressWarnings("unchecked")
    private Registry<T> registry(MinecraftServer server) {
        //? if >=1.19.3 {
        return server.registryAccess()
                     .registryOrThrow(registryKey);
        //?} else {
        /*// The server's registries hold only the data-driven ones before 1.19.3.
        return server.registryAccess()
                     .registry(registryKey)
                     .<Registry<T>>map(x -> x)
                     .orElseGet(() -> (Registry<T>) (Registry<?>) java.util.Objects.requireNonNull(Registry.REGISTRY.get(registryKey.location()),
                                                                                                 () -> "No registry " + registryKey.location()));
        *///?}
    }
}
