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
 * The cost of the combined short flag tokens (-ab, -ba, ...), which grow with the permutations of the flags.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class OptionTreeBenchmark {
    @Param({"2", "4", "6"})
    public int flags;

    @Benchmark
    public CommandTester register() {
        return CommandTester.builder()
                            .command(Trees.flags(flags))
                            .permissionPrefix(Trees.PERMISSION_PREFIX)
                            .build();
    }
}
