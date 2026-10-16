package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.CommonCommandContext;
import dev.marulabs.commandlib.exception.ArgumentParseException;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public class CommonLiteralArgument<C extends CommonCommandContext<?, ?>, SELF extends CommonLiteralArgument<C, SELF>> extends CommonArgument<String, C, SELF> {
    private final Supplier<Collection<String>> literalsSupplier;

    public CommonLiteralArgument(String name, Collection<String> literals) {
        this(name, () -> literals);
    }

    public CommonLiteralArgument(String name, Supplier<Collection<String>> literalsSupplier) {
        super(name, StringArgumentType.string());
        this.literalsSupplier = Objects.requireNonNull(literalsSupplier);
        addSuggestionProvider(sb -> QuotableSuggestions.suggestMatching(sb,
                                                                        literalsSupplier.get()
                                                                                        .stream()));
    }

    @Override
    public final String cast(Object parsedArgument) {
        return ((String) parsedArgument);
    }

    @Override
    protected final String parseImpl(C ctx) throws ArgumentParseException {
        String s = StringArgumentType.getString(ctx.getHandle(), name());
        return literalsSupplier.get()
                               .stream()
                               .filter(s::equals)
                               .findFirst()
                               .orElseThrow(() -> ArgumentParseException.ofIncorrectInput(this.name(), ctx, s));
    }
}
