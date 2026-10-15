package dev.marulabs.commandlib;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionCompilationMarkerTest {
    @AfterEach
    void clear() {
        System.clearProperty(FunctionCompilationMarker.CHANGES);
        System.clearProperty(FunctionCompilationMarker.COMPILED);
    }

    @Test
    void compilationCoversChangesBeforeIt() {
        Object resources = new Object();
        FunctionCompilationMarker.changed();
        assertThat(FunctionCompilationMarker.compiled(resources)).isFalse();

        FunctionCompilationMarker.markCompiled(resources);
        FunctionCompilationMarker.changed();
        assertThat(FunctionCompilationMarker.compiled(resources)).isFalse();

        FunctionCompilationMarker.markCompiled(resources);
        assertThat(FunctionCompilationMarker.compiled(resources)).isTrue();
    }

    @Test
    void reloadedResourcesNeedCompilation() {
        Object resources = new Object();
        FunctionCompilationMarker.markCompiled(resources);

        assertThat(FunctionCompilationMarker.compiled(new Object())).isFalse();
    }

    @Test
    void reloadedComparesWithTheResourcesOfTheLastCompilation() {
        Object resources = new Object();
        assertThat(FunctionCompilationMarker.reloaded(resources)).isFalse();

        FunctionCompilationMarker.markCompiled(resources);
        FunctionCompilationMarker.changed();
        assertThat(FunctionCompilationMarker.reloaded(resources)).isFalse();
        assertThat(FunctionCompilationMarker.reloaded(new Object())).isTrue();
    }

    @Test
    void unreadableMarkerNeverSkips() {
        Object resources = new Object();
        System.setProperty(FunctionCompilationMarker.CHANGES, "not a number");
        System.setProperty(FunctionCompilationMarker.COMPILED, "another format");

        assertThat(FunctionCompilationMarker.compiled(resources)).isFalse();
        FunctionCompilationMarker.changed();
        FunctionCompilationMarker.markCompiled(resources);
        assertThat(FunctionCompilationMarker.compiled(resources)).isTrue();
    }
}
