package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;
//? if >=1.19 {
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
//?}

import static org.assertj.core.api.Assertions.assertThat;

class UnparsedArgumentTest {
    private static CommandTester tester() {
        // A supplier, so the argument registers its type after the tester has bootstrapped Minecraft.
        return new CommandTester(() -> new Command("raw") {{
            argument(new UnparsedArgument("value")).execute((ctx, value) -> ctx.sendMessage(value));
        }}, "test");
    }

    @Test
    void a_word_with_selector_and_path_characters_parses_as_written() {
        FakeSender console = FakeSender.console();
        try (CommandTester tester = tester()) {
            tester.execute("raw @../config/value", console);
            tester.execute("raw @e[type=zombie]", console);
        }
        assertThat(console.getSentMessageTexts()).containsExactly("@../config/value", "@e[type=zombie]");
    }

    //? if >=1.19 {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void clients_are_sent_a_greedy_string() {
        try (CommandTester tester = tester()) {
            // Fabric has no registerByClass, so RawWordArgumentType puts itself into vanilla's class map by reflection.
            // Without the entry, sending the command tree to a client fails on the unknown argument type.
            assertThat(ArgumentTypeInfos.isClassRecognized(RawWordArgumentType.class)).isTrue();
            ArgumentTypeInfo.Template template = ArgumentTypeInfos.unpack(RawWordArgumentType.rawWord());
            assertThat(template.type()).isSameAs(ArgumentTypeInfos.unpack(StringArgumentType.greedyString())
                                                                  .type());
            JsonObject json = new JsonObject();
            template.type()
                    .serializeToJson(template, json);
            assertThat(json.get("type")
                           .getAsString()).isEqualTo("greedy");
        }
    }
    //?}
}
