package dev.marulabs.testplugin;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class SuggestionTest extends TestBase {
    private final AtomicReference<String> capturedLatestInput = new AtomicReference<>(null);

    public SuggestionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        return suggestionLatestInput();
    }

    private List<String> suggestionLatestInput() {
        String key = getKey();
        putResult(new TestResult(key, TestStatus.FAILED, "Suggestion action was not called before verification."));

        // Receives tab-complete requests; records getLatestInput() for later verification.
        command.addChildren(new Command("suggestionCapture") {{
            permission(PermissionDefault.TRUE);
            argument(new StringArgument("a").addSuggestionAction(builder -> {
                capturedLatestInput.set(builder.getLatestInput());
                builder.suggest("result");
            })).execute((a, ctx) -> {
            });
        }});

        // Dispatched by runTests to verify the captured value equals "abc".
        command.addChildren(new Command("verifySuggestionCapture") {{
            permission(PermissionDefault.TRUE);
            execute(ctx -> {
                String captured = capturedLatestInput.get();
                if (captured == null) {
                    putResult(new TestResult(key, TestStatus.FAILED, "Suggestion action was not called."));
                } else {
                    putResult(key, captured, "abc");
                }
            });
        }});

        return List.of(buildCommand(command, "verifySuggestionCapture"));
    }
}
