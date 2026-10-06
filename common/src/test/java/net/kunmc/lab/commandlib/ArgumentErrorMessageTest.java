package net.kunmc.lab.commandlib;

import net.kunmc.lab.commandlib.argument.CommonEnumArgument;
import net.kunmc.lab.commandlib.argument.CommonStringArgument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArgumentErrorMessageTest {
    private static final String HERE = "command.context.here";

    @Test
    void incorrect_input_message_points_at_invalid_token() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(directionCommand());

        TestCommandContext ctx = runner.execute("go up");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument", "§7go §c§nup§r" + HERE);
    }

    @Test
    void incorrect_input_message_strips_leading_slash_of_player_input() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(directionCommand());

        TestCommandContext ctx = runner.executeWithLeadingSlash("/go up");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument", "§7go §c§nup§r" + HERE);
    }

    @Test
    void incorrect_input_message_truncates_long_prefix() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new CommonStringArgument<>("label"),
                     new CommonEnumArgument<>("dir", Direction.class)).execute((label, dir, ctx) -> {
                ctx.sendMessage("unreachable");
            });
        }});

        TestCommandContext ctx = runner.execute("go abcdefghijkl up");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument", "§7...defghijkl §c§nup§r" + HERE);
    }

    @Test
    void validator_failure_uses_incorrect_input_message() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dir").validator(x -> x != Direction.NORTH)).execute((dir, ctx) -> {
                ctx.sendMessage("unreachable");
            });
        }});

        TestCommandContext ctx = runner.execute("go north");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument", "§7go §c§nnorth§r" + HERE);
    }

    @Test
    void validator_filters_enum_suggestions_without_failing() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dir").validator(x -> x != Direction.NORTH)).execute((dir, ctx) -> {
            });
        }});

        assertThat(runner.suggest("go ")).containsExactlyInAnyOrder("south", "east", "west", "help");
        // Enum suggestions match by substring, so "s" also matches east and west.
        assertThat(runner.suggest("go s")).containsExactlyInAnyOrder("south", "east", "west");
        assertThat(runner.suggest("go n")).isEmpty();
    }

    @Test
    void context_aware_validator_filters_enum_suggestions_without_failing() {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("go") {{
            argument(new DirArg("dir").validator((x, ctx) -> x == Direction.EAST)).execute((dir, ctx) -> {
            });
        }});

        assertThat(runner.suggest("go ")).containsExactlyInAnyOrder("east", "help");
    }

    private static TestCommand directionCommand() {
        return new TestCommand("go") {{
            argument(new CommonEnumArgument<>("dir", Direction.class)).execute((dir, ctx) -> {
                ctx.sendMessage("unreachable");
            });
        }};
    }

    enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    private static final class DirArg extends CommonEnumArgument<Direction, TestCommandContext, DirArg> {
        DirArg(String name) {
            super(name, Direction.class);
        }
    }

    @Test
    void incorrect_input_message_uses_argument_node_when_command_has_same_name() throws Exception {
        TestCommandRunner runner = new TestCommandRunner(new TestCommand("dir") {{
            argument(new CommonEnumArgument<>("dir", Direction.class)).execute((dir, ctx) -> {
                ctx.sendMessage("unreachable");
            });
        }});

        TestCommandContext ctx = runner.execute("dir up");

        assertThat(ctx.messages()).containsExactly("command.unknown.argument", "§7dir §c§nup§r" + HERE);
    }
}
