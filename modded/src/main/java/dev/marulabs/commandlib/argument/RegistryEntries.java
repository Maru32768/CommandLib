package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Caption;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;
import dev.marulabs.commandlib.suggestion.SuggestionBuilder;
import dev.marulabs.commandlib.util.StringUtil;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.stream.Stream;

// The lookup of the arguments that read a resource location and look it up in a registry of the server.
final class RegistryEntries {
    private RegistryEntries() {
    }

    static <T> T get(CommandContext ctx,
                     String name,
                     ResourceKey<? extends Registry<T>> registryKey,
                     Caption unknown) throws ArgumentParseException {
        ResourceLocation key = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx.getHandle(), name);
        // getOptional, unlike get, does not return the default value of a defaulted registry such as items.
        return registry(server(ctx), registryKey).getOptional(key)
                                                 .orElseThrow(() -> new ArgumentParseException(unknown,
                                                                                               Map.of("value", key)));
    }

    static <T> void suggestKeys(SuggestionBuilder<CommandContext> sb, ResourceKey<? extends Registry<T>> registryKey) {
        suggest(sb,
                registry(server(sb.getContext()), registryKey).keySet()
                                                              .stream());
    }

    static void suggest(SuggestionBuilder<CommandContext> sb, Stream<ResourceLocation> keys) {
        String input = sb.getLatestInput();
        keys.map(ResourceLocation::toString)
            .filter(x -> input.isEmpty() || StringUtil.containsIgnoreCase(x, input))
            .forEach(sb::suggest);
    }

    static MinecraftServer server(CommandContext ctx) {
        return ctx.getHandle()
                  .getSource()
                  .getServer();
    }

    @SuppressWarnings("unchecked")
    static <T> Registry<T> registry(MinecraftServer server, ResourceKey<? extends Registry<T>> registryKey) {
        //? if >=1.21.2 {
        /*return server.registryAccess()
                     .lookupOrThrow(registryKey);
        *///?} elif >=1.19.3 {
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
