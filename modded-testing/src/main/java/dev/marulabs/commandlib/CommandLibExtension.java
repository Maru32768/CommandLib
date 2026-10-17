package dev.marulabs.commandlib;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * JUnit 5 extension that creates a {@link CommandTester} for each test and injects it and {@link FakeSender}
 * parameters, configured with {@link Sender}, into test methods and {@code @BeforeEach} / {@code @AfterEach} methods.
 * A lifecycle method and the test it runs around get the same tester. An injected player is added with
 * {@link CommandTester#withPlayer(FakeSender)}, so player and entity arguments resolve it by name; two injected
 * players with the same name fail, so give them distinct {@link Sender} names.
 *
 * <pre>
 * class HelloCommandTest {
 *     &#64;RegisterExtension
 *     final CommandLibExtension commandLib = new CommandLibExtension(() -&gt; new CommandTester(HelloCommand::new,
 *                                                                                           "mymod.command"));
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
        if (extensionContext.getTestMethod()
                            .isEmpty()) {
            // A class-level store would share one tester, and its players, across the tests.
            throw new ParameterResolutionException("CommandLibExtension injects only into test methods and "
                    + "@BeforeEach / @AfterEach methods");
        }
        // close() does nothing, so the store need not close the tester.
        CommandTester tester = extensionContext.getStore(NAMESPACE)
                                               .getOrComputeIfAbsent(this,
                                                                     key -> testerFactory.get(),
                                                                     CommandTester.class);
        if (parameterContext.getParameter()
                            .getType() == CommandTester.class) {
            return tester;
        }
        FakeSender sender = createSender(parameterContext.findAnnotation(Sender.class)
                                                         .orElse(null));
        if (sender.isPlayer()) {
            if (tester.hasPlayer(sender.getName())) {
                // The later one would replace the earlier one on the server.
                throw new ParameterResolutionException("another injected player is named " + sender.getName()
                        + "; give them distinct @Sender names");
            }
            tester.withPlayer(sender);
        }
        return sender;
    }

    private static FakeSender createSender(@Nullable Sender sender) {
        if (sender == null) {
            return FakeSender.playerBuilder("Player")
                             .build();
        }
        FakeSender.Builder builder = sender.console() ? FakeSender.consoleBuilder()
                : FakeSender.playerBuilder(sender.value());
        if (sender.op()) {
            builder.op(true);
        }
        if (sender.permissions().length > 0) {
            builder.permissions(sender.permissions());
        }
        return builder.denyPermissions(sender.denyPermissions())
                      .build();
    }
}
