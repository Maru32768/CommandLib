package dev.marulabs.commandlib.benchmark;

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
 * Parsing and executing one input: Brigadier's parse, the permission checks, CommandLib's option and argument parsing
 * and the executor.
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.SampleTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ExecuteBenchmark {
    @Param({"10", "1000"})
    public int commands;

    @Param({"cmd5 Alex", "cmd5 Alex 3", "cmd5 -fv --limit 20 Alex 3", "cmd5 list a b c d", "cmd5 admin"})
    public String input;

    private CommandTester tester;
    private final FakeSender sender = FakeSender.player("bench");

    @Setup
    public void setUp() {
        tester = Trees.tester(commands);
        // A failing input would measure the failure path instead.
        if (tester.execute(input, sender) <= 0) {
            throw new IllegalStateException("The input fails: " + input);
        }
    }

    @Benchmark
    public int execute() {
        return tester.execute(input, sender);
    }
}
