package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.junit.jupiter.api.Test;

import java.util.List;

//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.core.Registry;*/

import static org.assertj.core.api.Assertions.assertThat;

class BiomeArgumentTest {
    @Test
    void biome_of_the_server_registries_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("biome") {{
            argument(new BiomeArgument("biome")).execute((ctx, biome) -> {
                //? if >=1.19.3 {
                ctx.sendMessage(String.valueOf(RegistryEntries.registry(ctx.getSender()
                                                                           .getServer(),
                                                                        Registries.BIOME)
                                                              .getKey(biome)));
                //?} else {
                /*ctx.sendMessage(String.valueOf(RegistryEntries.registry(ctx.getSender()
                                                                           .getServer(),
                                                                        Registry.BIOME_REGISTRY)
                                                              .getKey(biome)));
                *///?}
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            tester.execute("biome minecraft:plains", sender);
            tester.execute("biome cadence:missing", sender);
            suggestions = tester.suggest("biome plai", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).containsExactly("minecraft:plains", "cadence:missing is not a known biome");
        assertThat(suggestions).contains("minecraft:plains");
    }
}
