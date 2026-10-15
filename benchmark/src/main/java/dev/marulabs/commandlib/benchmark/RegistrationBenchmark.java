package dev.marulabs.commandlib.benchmark;

import dev.marulabs.commandlib.CommandTester;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * The startup cost: declaring the commands and building their Brigadier tree, as registering them does.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RegistrationBenchmark {
    @Param({"10", "100", "1000"})
    public int commands;

    @Benchmark
    public CommandTester register() {
        return Trees.tester(commands);
    }
}
