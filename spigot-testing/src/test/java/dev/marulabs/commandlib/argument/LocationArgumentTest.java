package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationArgumentTest {
    @Test
    void coordinates_are_parsed_as_location() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new LocationArgument("pos")).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            });
        }}, "test.command")) {
            tester.execute("tp 1.0 2.0 3.0", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1.0,2.0,3.0");
    }

    @Test
    void negative_coordinates_are_parsed() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new LocationArgument("pos")).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            });
        }}, "test.command")) {
            tester.execute("tp -10.5 64.0 200.0", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("-10.5,64.0,200.0");
    }

    @Test
    void world_is_derived_from_player_sender() {
        FakeSender steve = FakeSender.player("Steve");
        org.bukkit.World world = org.mockito.Mockito.mock(org.bukkit.World.class);
        org.mockito.Mockito.when(((org.bukkit.entity.Player) steve.asSender()).getWorld())
                           .thenReturn(world);

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new LocationArgument("pos")).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getWorld() == world ? "correct_world" : "wrong_world");
            });
        }}, "test.command")) {
            tester.execute("tp 0.0 0.0 0.0", steve);
        }

        assertThat(steve.getSentMessageTexts()).containsExactly("correct_world");
    }

    @Test
    void integer_x_and_z_are_centered_by_default() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new LocationArgument("pos")).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            });
        }}, "test.command")) {
            tester.execute("tp 1 64 -2", sender);
            tester.execute("tp 1.0 64 -2.25", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1.5,64.0,-1.5", "1.0,64.0,-2.25");
    }

    @Test
    void integer_coordinates_are_kept_without_centering() {
        FakeSender sender = FakeSender.player("Alice");

        try (CommandTester tester = new CommandTester(() -> new Command("tp") {{
            argument(new LocationArgument("pos", false)).execute((ctx, pos) -> {
                ctx.sendMessage(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            });
        }}, "test.command")) {
            tester.execute("tp 1 64 -2", sender);
        }

        assertThat(sender.getSentMessageTexts()).containsExactly("1.0,64.0,-2.0");
    }
}
