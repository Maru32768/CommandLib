package dev.marulabs.commandlib;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * JUnit 5 extension that creates a {@link CommandTester} for each test, closes it after the test, and injects it and
 * {@link FakeSender} parameters, configured with {@link Sender}, into test methods and their {@code @BeforeEach} and
 * {@code @AfterEach} methods, which get the same tester. It refuses other parameters, such as those of
 * {@code @BeforeAll} methods or constructors, since every test of the class would share their tester. An injected
 * player is registered with {@link CommandTester#withFakePlayer(Player)}, so player and entity arguments resolve it by
 * name; two players of one name are refused.
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
        if (extensionContext.getTestMethod()
                            .isEmpty()) {
            throw new ParameterResolutionException("CommandLibExtension injects only into test methods and their @BeforeEach and @AfterEach methods");
        }
        CommandTester tester = extensionContext.getStore(NAMESPACE)
                                               .getOrComputeIfAbsent(this,
                                                                     key -> new TesterResource(testerFactory.get()),
                                                                     TesterResource.class).tester;
        if (parameterContext.getParameter()
                            .getType() == CommandTester.class) {
            return tester;
        }
        FakeSender sender = createSender(parameterContext.findAnnotation(Sender.class)
                                                         .orElse(null));
        if (sender.asSender() instanceof Player) {
            if (tester.hasFakePlayer(sender.getName())) {
                throw new ParameterResolutionException("A player named " + sender.getName()
                        + " is already registered; give each FakeSender parameter its own name with @Sender");
            }
            tester.withFakePlayer((Player) sender.asSender());
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
