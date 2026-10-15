package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * JUnit 5 extension that creates a {@link CommandTester} for each test, closes it after the test, and injects it and
 * {@link FakeSender} parameters, configured with {@link Sender}, into test and lifecycle methods. A
 * {@code @BeforeEach} method and the test it precedes get the same tester. An injected player is registered with
 * {@link CommandTester#withFakePlayer(FakeSender)}, so player arguments resolve it by name.
 *
 * <pre>
 * class HelloCommandTest {
 *     &#64;RegisterExtension
 *     final CommandLibExtension commandLib = new CommandLibExtension(() -&gt; new CommandTester(new HelloCommand(),
 *                                                                                           "myplugin.command"));
 *
 *     &#64;Test
 *     void greets(CommandTester tester, &#64;Sender("Alice") FakeSender alice) {
 *         tester.execute("hello", alice);
 *         assertThat(alice.getSentMessageTexts()).containsExactly("Hello Alice");
 *     }
 * }
 * </pre>
 */
public final class CommandLibExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CommandLibExtension.class);

    private final Supplier<CommandTester> testerFactory;

    /**
     * @param testerFactory creates the tester of each test, for example with {@link CommandTester#builder()}
     */
    public CommandLibExtension(Supplier<CommandTester> testerFactory) {
        this.testerFactory = Objects.requireNonNull(testerFactory, "testerFactory");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter()
                                        .getType();
        return type == CommandTester.class || type == FakeSender.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        CommandTester tester = extensionContext.getStore(NAMESPACE)
                                               .getOrComputeIfAbsent(this,
                                                                     key -> new TesterResource(testerFactory.get()),
                                                                     TesterResource.class).tester;
        if (parameterContext.getParameter()
                            .getType() == CommandTester.class) {
            return tester;
        }
        Sender annotation = parameterContext.findAnnotation(Sender.class)
                                            .orElse(null);
        FakeSender sender = createSender(annotation);
        if (annotation == null || !annotation.console()) {
            tester.withFakePlayer(sender);
        }
        return sender;
    }

    private static FakeSender createSender(@Nullable Sender sender) {
        if (sender == null) {
            return FakeSender.player("Player");
        }
        FakeSender result = sender.console() ? FakeSender.console() : FakeSender.player(sender.value());
        List<String> denied = Arrays.asList(sender.denyPermissions());
        if (sender.permissions().length > 0) {
            // FakeSender's permissions(...) and denyPermissions(...) replace each other, so grant the difference.
            return result.permissions(Arrays.stream(sender.permissions())
                                            .filter(node -> !denied.contains(node))
                                            .toArray(String[]::new));
        }
        return result.denyPermissions(sender.denyPermissions());
    }

    private static final class TesterResource implements ExtensionContext.Store.CloseableResource {
        private final CommandTester tester;

        private TesterResource(CommandTester tester) {
            this.tester = tester;
        }

        @Override
        public void close() {
            tester.close();
        }
    }
}
