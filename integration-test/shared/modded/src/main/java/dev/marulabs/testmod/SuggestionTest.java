package dev.marulabs.testmod;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.DefaultPermission;
import dev.marulabs.commandlib.argument.StringArgument;
import dev.marulabs.integration.core.TestResult;
import dev.marulabs.integration.core.TestStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class SuggestionTest extends TestBase {
    private final AtomicReference<String> capturedLatestInput = new AtomicReference<>();

    public SuggestionTest(Command command) {
        super(command);
    }

    @Override
    public List<String> build() {
        String key = getKey("latestInputFromClientTabComplete");
        putResult(new TestResult(key, TestStatus.FAILED, "Suggestion action was not called before verification."));

        // IntegrationTest sends "/commandlibtest suggestionCapture abc" as a tab-complete request before runTests.
        command.addChildren(new Command("suggestionCapture") {{
            permission(DefaultPermission.ALL);
            argument(new StringArgument("a").addSuggestionAction(builder -> {
                capturedLatestInput.set(builder.getLatestInput());
                builder.suggest("result");
            })).execute((a, ctx) -> {
            });
        }});

        command.addChildren(new Command("verifySuggestionCapture") {{
            execute(ctx -> {
                String captured = capturedLatestInput.get();
                if (captured == null) {
                    putResult(new TestResult(key, TestStatus.FAILED, "Suggestion action was not called."));
                } else {
                    putResult(key, captured, "abc");
                }
            });
        }});

        return List.of(buildCommand("verifySuggestionCapture"));
    }
}
