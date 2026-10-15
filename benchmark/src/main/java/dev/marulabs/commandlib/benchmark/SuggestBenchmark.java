package dev.marulabs.commandlib.benchmark;

import com.mojang.brigadier.suggestion.Suggestions;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Suggestions for partial input, as a client asks for them while the player types.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SuggestBenchmark {
    @Param({"10", "1000"})
    public int commands;

    // The root (every command), a command's children, options and argument, its option tokens, and a provider.
    @Param({"", "cmd5 ", "cmd5 -", "cmd5 A"})
    public String input;

    private CommandTester tester;
    private final FakeSender sender = FakeSender.player("bench");

    @Setup
    public void setUp() {
        tester = Trees.tester(commands);
    }

    @Benchmark
    public Suggestions suggest() {
        return tester.suggestions(input, sender)
                     .join();
    }
}
