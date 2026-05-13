package net.kunmc.lab.testplugin;

import net.kunmc.lab.integration.core.TestResult;
import net.kunmc.lab.integration.core.TestStatus;

import net.kunmc.lab.commandlib.Command;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.CommandOption;
import net.kunmc.lab.commandlib.Options;
import net.kunmc.lab.commandlib.argument.StringArgument;

import java.util.ArrayList;
import java.util.List;

public final class OptionTest extends TestBase {
    public OptionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        List<String> commands = new ArrayList<>();

        commands.addAll(flagAndValueOptions());
        commands.addAll(defaultOptionValues());
        commands.addAll(combinedShortFlags());
        commands.addAll(requiredOption());
        commands.addAll(requiredOptionValue());
        commands.addAll(requiredOptionValueInvalid());
        commands.addAll(valueOptionWithoutValue());
        commands.addAll(optionAfterArgument());

        return commands;
    }

    public List<String> flagAndValueOptions() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f')
                                                                         .description("Force execution"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100)
                                                                         .description("Maximum count"));
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text")
                                                                         .description("Output format"));

            argument(new StringArgument("str", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(limit) + ":" + ctx.getOption(format) + ":" + target,
                          "true:20:json:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -f -n 20 --format json alex"));
    }

    public List<String> defaultOptionValues() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10, 1, 100));
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key,
                          ctx.getOption(force) + ":" + ctx.getOption(limit) + ":" + ctx.getOption(format) + ":" + target,
                          "false:10:text:alex");
            });
        }});

        return List.of(buildCommand(command, name + " alex"));
    }

    public List<String> combinedShortFlags() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<Boolean, CommandContext> verbose = option(Options.flag("verbose", 'v'));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(force) + ":" + ctx.getOption(verbose) + ":" + target, "true:true:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -fv alex"));
    }

    public List<String> requiredOption() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));
            CommandOption<String, CommandContext> reason = option(Options.string("reason", 'r', "")
                                                                         .requires(force));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(force) + ":" + ctx.getOption(reason) + ":" + target, "true:cleanup:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -f -r cleanup alex"));
    }

    public List<String> requiredOptionValue() {
        String name = getMethodName();
        String key = getKey();

        putCommandNotExecutedResult(key);
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                         .requires(mode, "parallel"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(key, ctx.getOption(mode) + ":" + ctx.getOption(limit) + ":" + target, "parallel:20:alex");
            });
        }});

        return List.of(buildCommand(command, name + " -n 20 -m parallel alex"));
    }

    public List<String> requiredOptionValueInvalid() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.SUCCEEDED, "Command was not executed."));
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> mode = option(Options.string("mode", 'm', "normal"));
            CommandOption<Integer, CommandContext> limit = option(Options.integer("limit", 'n', 10)
                                                                         .requires(mode, "parallel"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(new TestResult(key,
                                         TestStatus.FAILED,
                                         "Command was executed with mode=" + ctx.getOption(mode) + ", limit=" + ctx.getOption(
                                                 limit)));
            });
        }});

        return List.of(buildCommand(command, name + " -m normal -n 20 alex"));
    }

    public List<String> valueOptionWithoutValue() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.SUCCEEDED, "Command was not executed."));
        command.addChildren(new Command(name) {{
            CommandOption<String, CommandContext> format = option(Options.string("format", 'F', "text"));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(new TestResult(key,
                                         TestStatus.FAILED,
                                         "Command was executed with format=" + ctx.getOption(format)));
            });
        }});

        return List.of(buildCommand(command, name + " --format"));
    }

    public List<String> optionAfterArgument() {
        String name = getMethodName();
        String key = getKey();

        putResult(new TestResult(key, TestStatus.SUCCEEDED, "Command was not executed."));
        command.addChildren(new Command(name) {{
            CommandOption<Boolean, CommandContext> force = option(Options.flag("force", 'f'));

            argument(new StringArgument("target", StringArgument.Type.WORD)).execute((target, ctx) -> {
                putResult(new TestResult(key,
                                         TestStatus.FAILED,
                                         "Command was executed with force=" + ctx.getOption(force)));
            });
        }});

        return List.of(buildCommand(command, name + " alex -f"));
    }

}
