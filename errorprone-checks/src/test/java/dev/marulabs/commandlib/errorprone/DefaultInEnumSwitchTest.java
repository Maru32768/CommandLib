package dev.marulabs.commandlib.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

class DefaultInEnumSwitchTest {
    private final CompilationTestHelper helper = CompilationTestHelper.newInstance(DefaultInEnumSwitch.class,
                                                                                   getClass());

    @Test
    void a_default_in_an_enum_switch_statement_is_reported() {
        helper.addSourceLines("Test.java",
                              "class Test {",
                              "  enum Kind { A, B, C }",
                              "  int of(Kind kind) {",
                              "    switch (kind) {",
                              "      case A:",
                              "        return 1;",
                              "      // BUG: Diagnostic contains: DefaultInEnumSwitch",
                              "      default:",
                              "        return 2;",
                              "    }",
                              "  }",
                              "}")
              .doTest();
    }

    @Test
    void a_default_in_an_enum_switch_expression_is_reported() {
        helper.addSourceLines("Test.java",
                              "class Test {",
                              "  enum Kind { A, B }",
                              "  int of(Kind kind) {",
                              "    return switch (kind) {",
                              "      case A -> 1;",
                              "      // BUG: Diagnostic contains: DefaultInEnumSwitch",
                              "      default -> 2;",
                              "    };",
                              "  }",
                              "}")
              .doTest();
    }

    @Test
    void every_constant_without_a_default_and_a_switch_on_a_string_pass() {
        helper.addSourceLines("Test.java",
                              "class Test {",
                              "  enum Kind { A, B }",
                              "  int of(Kind kind) {",
                              "    switch (kind) {",
                              "      case A:",
                              "        return 1;",
                              "      case B:",
                              "        return 2;",
                              "    }",
                              "    throw new AssertionError(kind);",
                              "  }",
                              "  int of(String name) {",
                              "    switch (name) {",
                              "      case \"a\":",
                              "        return 1;",
                              "      default:",
                              "        return 2;",
                              "    }",
                              "  }",
                              "}")
              .doTest();
    }
}
