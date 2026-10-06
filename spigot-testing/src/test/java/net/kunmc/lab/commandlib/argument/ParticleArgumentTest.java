package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandTester;
import net.kunmc.lab.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticleArgumentTest {
    @Test
    void particle_is_resolved_by_id() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(ParticleArgumentTest::particleCommand, "test.command")) {
            tester.execute("particle flame", sender);
            tester.execute("particle minecraft:flame", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("particle=FLAME", "particle=FLAME");
    }

    @Test
    void unknown_particle_is_a_syntax_error() {
        try (CommandTester tester = new CommandTester(ParticleArgumentTest::particleCommand, "test.command")) {
            assertThatThrownBy(() -> tester.execute("particle not_a_particle",
                                                    FakeSender.player("Alice"))).hasCauseInstanceOf(
                    CommandSyntaxException.class);
        }
    }

    @Test
    void suggestions_do_not_fail_for_partial_input() {
        try (CommandTester tester = new CommandTester(ParticleArgumentTest::particleCommand, "test.command")) {
            assertThat(tester.suggestions("particle fl", FakeSender.player("Alice"))
                             .join()
                             .getList()).isNotNull();
        }
    }

    private static Command particleCommand() {
        return new Command("particle") {{
            argument(new ParticleArgument("type")).execute((particle, ctx) -> {
                ctx.sendMessage("particle=" + particle.name());
            });
        }};
    }
}
