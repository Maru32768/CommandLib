package dev.marulabs.commandlib;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CommandBuilderTest {
    // CommonCommand methods the builder has no counterpart for, as signatures with erased parameter types.
    private static final Set<String> NOT_FORWARDED = Set.of(
                                                            // They read the command instead of configuring it.
                                                            "name()",
                                                            "description(CommonCommandContext)",
                                                            "permissionName(String)",
                                                            "permissionConfig(String)",
                                                            // A callback cannot follow varargs; the List form covers
                                                            // it.
                                                            "argument(CommonArgument[])");

    /**
     * Each method of {@link CommonCommand} that configures the command has a builder method of the same name. One that
     * returns a branch there takes a callback for the branch as its last parameter here; the others take the same
     * parameters.
     */
    @Test
    void forwards_every_configuration_method_of_the_command() {
        Set<String> builder = Arrays.stream(CommandBuilder.class.getMethods())
                                    .filter(x -> x.getReturnType() == CommandBuilder.class)
                                    .map(CommandBuilderTest::signature)
                                    .collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        List<String> commandMethods = new ArrayList<>();
        for (Method method : CommonCommand.class.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || method.isSynthetic()) {
                continue;
            }
            String signature = signature(method);
            commandMethods.add(signature);
            if (NOT_FORWARDED.contains(signature)) {
                continue;
            }
            List<Class<?>> parameters = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
            if (method.getReturnType()
                      .getPackageName()
                      .equals("dev.marulabs.commandlib.branch")) {
                parameters.add(Consumer.class);
            }
            String expected = signature(method.getName(), parameters);
            if (!builder.contains(expected)) {
                missing.add(expected);
            }
        }

        assertThat(missing).as("CommandBuilder methods missing for CommonCommand methods")
                           .isEmpty();
        assertThat(commandMethods).as("NOT_FORWARDED names only existing CommonCommand methods")
                                  .containsAll(NOT_FORWARDED);
    }

    private static String signature(Method method) {
        return signature(method.getName(), Arrays.asList(method.getParameterTypes()));
    }

    private static String signature(String name, List<Class<?>> parameters) {
        return parameters.stream()
                         .map(Class::getSimpleName)
                         .collect(Collectors.joining(", ", name + "(", ")"));
    }
}
