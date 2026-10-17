package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.commandlibtest.PaperResolvers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageArgumentSelectorTest {
    @Test
    void paper_resolver_resolves_the_selectors() throws CommandSyntaxException {
        Message resolved = MessageArgument.resolveSelectors(PaperResolvers.resolving("hello, Alice!"), new Object());

        assertThat(resolved).isNotNull();
        assertThat(resolved.getString()).isEqualTo("hello, Alice!");
    }

    @Test
    void paper_resolver_of_an_unknown_shape_fails_instead_of_keeping_the_selectors() {
        assertThatThrownBy(() -> MessageArgument.resolveSelectors(PaperResolvers.withoutMessage(),
                                                                  new Object())).isInstanceOf(IllegalStateException.class)
                                                                                .hasMessageContaining("Cannot resolve the selectors");
    }

    @Test
    void a_selector_that_fails_to_resolve_is_a_syntax_error() {
        CommandSyntaxException failure = new SimpleCommandExceptionType(new LiteralMessage("No entity was found")).create();

        assertThatThrownBy(() -> MessageArgument.resolveSelectors(PaperResolvers.failing(failure),
                                                                  new Object())).isSameAs(failure);
    }

    @Test
    void another_resolver_keeps_its_content() throws CommandSyntaxException {
        assertThat(MessageArgument.resolveSelectors(new PaperResolvers.Base() {
        }, new Object())).isNull();
    }
}
