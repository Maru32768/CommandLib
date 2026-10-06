package net.kunmc.lab.commandlib.argument;

import com.mojang.brigadier.StringReader;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentTypeRegistrar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RawWordArgumentTypeTest {
    @BeforeEach
    @AfterEach
    void resetRegistration() {
        RawWordArgumentType.resetRegistrationForTest();
    }

    @Test
    void parse_reads_single_token_including_special_characters() {
        StringReader reader = new StringReader("@e[type=zombie] next");

        assertThat(RawWordArgumentType.rawWord()
                                      .parse(reader)).isEqualTo("@e[type=zombie]");
        assertThat(reader.getRemaining()).isEqualTo(" next");
    }

    @Test
    void parse_accepts_empty_remaining_input() {
        StringReader reader = new StringReader("");

        assertThat(RawWordArgumentType.rawWord()
                                      .parse(reader)).isEmpty();
    }

    @Test
    void registration_happens_once_after_success() {
        NMSArgumentTypeRegistrar registrar = mock(NMSArgumentTypeRegistrar.class);
        try (MockedStatic<NMSArgumentTypeRegistrar> registrarStatic = mockStatic(NMSArgumentTypeRegistrar.class)) {
            registrarStatic.when(NMSArgumentTypeRegistrar::create)
                           .thenReturn(registrar);

            RawWordArgumentType.ensureRegistered();
            RawWordArgumentType.ensureRegistered();

            verify(registrar, times(1)).registerAsGreedyString(eq(RawWordArgumentType.class), any());
            assertThat(RawWordArgumentType.isRegistered()).isTrue();
        }
    }

    @Test
    void failed_registration_is_retried_on_next_use() {
        NMSArgumentTypeRegistrar registrar = mock(NMSArgumentTypeRegistrar.class);
        doThrow(new IllegalStateException("registry is frozen")).doNothing()
                                                                 .when(registrar)
                                                                 .registerAsGreedyString(eq(RawWordArgumentType.class),
                                                                                         any());
        try (MockedStatic<NMSArgumentTypeRegistrar> registrarStatic = mockStatic(NMSArgumentTypeRegistrar.class)) {
            registrarStatic.when(NMSArgumentTypeRegistrar::create)
                           .thenReturn(registrar);

            assertThatThrownBy(RawWordArgumentType::ensureRegistered).isInstanceOf(IllegalStateException.class);
            assertThat(RawWordArgumentType.isRegistered()).isFalse();

            RawWordArgumentType.ensureRegistered();

            verify(registrar, times(2)).registerAsGreedyString(eq(RawWordArgumentType.class), any());
            assertThat(RawWordArgumentType.isRegistered()).isTrue();
        }
    }
}
