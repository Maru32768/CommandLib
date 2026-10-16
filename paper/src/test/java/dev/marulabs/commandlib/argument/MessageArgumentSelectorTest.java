package dev.marulabs.commandlib.argument;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
    void another_resolver_keeps_its_content() throws CommandSyntaxException {
        assertThat(MessageArgument.resolveSelectors(new PaperResolvers.Base() {
        }, new Object())).isNull();
    }
}
