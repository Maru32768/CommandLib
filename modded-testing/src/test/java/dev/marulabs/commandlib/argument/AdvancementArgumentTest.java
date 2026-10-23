package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

//? if >=1.20.2
/*import net.minecraft.advancements.AdvancementHolder;*/
import net.minecraft.advancements.Advancement;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancementArgumentTest {
    @Test
    void advancement_of_the_server_is_read_by_its_key_and_suggested() {
        List<String> messages;
        List<String> suggestions;

        try (CommandTester tester = new CommandTester(() -> new Command("advancement") {{
            argument(new AdvancementArgument("advancement")).execute((ctx, advancement) -> {
                //? if >=1.20.2 {
                /*ctx.sendMessage(advancement.id()
                   .toString());
                *///?} else {
                ctx.sendMessage(advancement.getId()
                                           .toString());
                //?}
            });
        }}, "test.command")) {
            FakeSender sender = FakeSender.player("Alice");
            ResourceLocation id = ResourceLocation.tryParse("minecraft:story/mine_stone");
            ServerAdvancementManager advancements = Mockito.mock(ServerAdvancementManager.class);
            //? if >=1.20.2 {
            /*AdvancementHolder advancement = new AdvancementHolder(id, Mockito.mock(Advancement.class));
            Mockito.when(advancements.get(id))
                   .thenReturn(advancement);
            *///?} else {
            Advancement advancement = Mockito.mock(Advancement.class);
            Mockito.when(advancement.getId())
                   .thenReturn(id);
            Mockito.when(advancements.getAdvancement(id))
                   .thenReturn(advancement);
            //?}
            Mockito.when(advancements.getAllAdvancements())
                   .thenReturn(List.of(advancement));
            tester.withServer(server -> Mockito.when(server.getAdvancements())
                                               .thenReturn(advancements));

            tester.execute("advancement minecraft:story/mine_stone", sender);
            tester.execute("advancement commandlib:missing", sender);
            suggestions = tester.suggest("advancement mine", sender);
            messages = sender.getSentMessageTexts();
        }

        assertThat(messages).hasSize(2)
                            .startsWith("minecraft:story/mine_stone");
        assertThat(messages.get(1)).contains("commandlib:missing");
        assertThat(suggestions).containsExactly("minecraft:story/mine_stone");
    }
}
