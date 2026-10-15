# Benchmarks

The `benchmark` module measures CommandLib's own cost with [JMH](https://github.com/openjdk/jmh): registering
commands, parsing and executing input, and suggesting. It drives `common` through the `common-testing`
`CommandTester`, which builds the same Brigadier tree as a platform registration, so no server is needed. The module is
not published, and `check` only compiles it; the benchmarks run only through the tasks below.

## Running

```bash
./gradlew -Pcommandlib.typedNms=false --configure-on-demand :benchmark:jmh
./gradlew -Pcommandlib.typedNms=false --configure-on-demand :benchmark:jmhCompare
```

`jmh` runs every benchmark with `-f 1 -wi 3 -w 1s -i 5 -r 1s -prof gc` (one fork, three 1 s warmup iterations, five
1 s measurement iterations, allocation profiler) and writes `benchmark/build/results/jmh/results.json`. A full run takes
about nine minutes. `--args` replaces these options, for example to run one benchmark:

```bash
./gradlew -Pcommandlib.typedNms=false :benchmark:jmh --args="-f 1 -wi 3 -i 5 -prof gc -rf json -rff build/results/jmh/results.json SuggestBenchmark"
```

`jmhCompare` compares `results.json` with `benchmark/baseline.json` (another file with `-Pbenchmark.baseline=<path>`),
prints the change of each metric, and fails when one regressed beyond its tolerance. To record a new baseline, run
`:benchmark:jmhBaseline`, which writes `results.json` to `baseline.json` without the raw samples, and update the
results below.

Compare only runs of the same machine, JDK and power settings; the baseline below is from one desktop, so a run on
another machine needs its own baseline first.

## What is measured

Every command of a tree has the same shape (`Trees.command`), so a tree of N commands has N times its nodes:

```
/cmdI [-f|--force] [-v|--verbose] [-n|--limit <1..100>] <target: word, 20 suggestions> [count: int, default 1]
/cmdI list <words: word, 1 to 4 values>
/cmdI admin                                  (permission bench.admin)
```

The sender is `FakeSender.player("bench")`, which has every permission, so each permission check runs but passes.
Executors run synchronously (the tester's default scheduler) and write their result to a volatile field; they send no
messages. There is no cache between calls beyond what CommandLib and Brigadier keep themselves.

| Benchmark | Mode | Parameters | Measures |
|---|---|---|---|
| `RegistrationBenchmark.register` | average time | `commands` 10 / 100 / 1000 | declaring the commands and building the tree (`CommandTester.build`) |
| `OptionTreeBenchmark.register` | average time | `flags` 2 / 4 / 6 | registering one command with that many short flags, whose combined tokens (`-ab`, `-ba`, ...) grow with their permutations |
| `ExecuteBenchmark.execute` | sample time, throughput | `commands` 10 / 1000; `input` below | `CommandTester.execute`: Brigadier parse, permission checks, option and argument parsing, executor |
| `SuggestBenchmark.suggest` | sample time | `commands` 10 / 1000; `input` below | `CommandTester.suggestions(...).join()` |

Execute inputs: `cmd5 Alex` (optional argument omitted), `cmd5 Alex 3`, `cmd5 -fv --limit 20 Alex 3` (combined flags and
a value option), `cmd5 list a b c d` (variadic), `cmd5 admin` (child command with its own permission).

Suggest inputs: `` (the root, every command), `cmd5 ` (children, option tokens and the argument), `cmd5 -` (option
tokens), `cmd5 A` (the argument's suggestion provider).

Sample time gives the latency distribution (p50 / p95 / p99), throughput the operations per microsecond, and
`-prof gc` the allocation per operation (`gc.alloc.rate.norm`, bytes per operation).

## Regression tolerances

These were set before the first results were recorded. A metric regresses when it is worse than the baseline by more
than:

| Metric | Applies to | Tolerance |
|---|---|---|
| p50 latency | sample time benchmarks | +10% |
| p95 latency | sample time benchmarks | +20% |
| throughput | throughput benchmarks | -10% |
| average time | registration benchmarks | +15% |
| allocation per operation | every benchmark | +5% |

The tail and the registration cost are noisier than the median, so they get more room. Allocation is nearly
deterministic, so a small increase already means more work per call. `jmhCompare` applies these values, which are
also in `benchmark/build.gradle.kts`. An improvement never fails; record it as the new baseline.

## Results

The baseline in `benchmark/baseline.json`, recorded with the default `jmh` options:

- CommandLib: library code of `647ca7c4`, Brigadier 1.0.18, `common-testing`
  `CommandTester` (no platform tester)
- JDK: Temurin 21.0.3 (OpenJDK 64-Bit Server VM 21.0.3+9-LTS), default JVM options and heap, JMH 1.37
- OS / CPU: Windows 11 Home 10.0.26200, AMD Ryzen 5 3600 (6 cores, 12 threads), 64 GB
- Load: other Gradle builds kept the CPU near 100% during the run, so the means, the tails and the throughput are
  noisy (see the errors). The medians and the allocation are the dependable numbers; re-record the baseline on an idle
  machine before gating on the time metrics.

Tree size (`Trees.command`): 61 nodes per command (610 for 10 commands, 61,000 for 1000), 305 nodes per command when
every path is expanded, as the vanilla command packet sends it.

Execute (µs per operation; throughput in operations per µs; allocation in bytes per operation):

| commands | input | p50 | p95 | p99 | throughput | alloc |
|---|---|---|---|---|---|---|
| 10 | `cmd5 Alex` | 2.2 | 2.7 | 66 | 0.048 ± 0.201 | 4,715 |
| 10 | `cmd5 Alex 3` | 2.7 | 3.9 | 121 | 0.044 ± 0.217 | 5,815 |
| 10 | `cmd5 -fv --limit 20 Alex 3` | 4.2 | 6.2 | 123 | 0.103 ± 0.079 | 8,621 |
| 10 | `cmd5 list a b c d` | 5.1 | 12.7 | 97 | 0.050 ± 0.077 | 9,485 |
| 10 | `cmd5 admin` | 1.0 | 1.3 | 22 | 0.248 ± 0.801 | 2,674 |
| 1000 | `cmd5 Alex` | 2.5 | 4.0 | 77 | 0.112 ± 0.293 | 4,709 |
| 1000 | `cmd5 Alex 3` | 3.6 | 6.1 | 94 | 0.091 ± 0.179 | 6,188 |
| 1000 | `cmd5 -fv --limit 20 Alex 3` | 4.9 | 8.8 | 112 | 0.111 ± 0.055 | 8,710 |
| 1000 | `cmd5 list a b c d` | 5.3 | 13.0 | 151 | 0.102 ± 0.120 | 9,733 |
| 1000 | `cmd5 admin` | 1.2 | 1.8 | 50 | 0.227 ± 0.200 | 2,666 |

Suggest (µs per operation, bytes per operation):

| commands | input | p50 | p95 | p99 | alloc |
|---|---|---|---|---|---|
| 10 | `` | 4.5 | 6.8 | 128 | 9,508 |
| 10 | `cmd5 ` | 20.3 | 41.0 | 187 | 26,594 |
| 10 | `cmd5 -` | 21.2 | 75.5 | 523 | 27,246 |
| 10 | `cmd5 A` | 16.6 | 43.1 | 249 | 21,862 |
| 1000 | `` | 1,323 | 5,042 | 13,237 | 936,379 |
| 1000 | `cmd5 ` | 21.4 | 52.1 | 303 | 26,533 |
| 1000 | `cmd5 -` | 22.5 | 49.9 | 229 | 27,187 |
| 1000 | `cmd5 A` | 18.0 | 90.6 | 491 | 21,867 |

Registration (average µs per operation, bytes per operation):

| benchmark | parameter | average | alloc | nodes | expanded nodes |
|---|---|---|---|---|---|
| `RegistrationBenchmark` | 10 commands | 19,351 ± 42,710 | 1,278,357 | 610 | 3,050 |
| `RegistrationBenchmark` | 100 commands | 17,008 ± 18,486 | 12,594,669 | 6,100 | 30,500 |
| `RegistrationBenchmark` | 1000 commands | 539,585 ± 357,311 | 125,756,882 | 61,000 | 305,000 |
| `OptionTreeBenchmark` | 2 flags | 317 ± 2,153 | 37,993 | 16 | 31 |
| `OptionTreeBenchmark` | 4 flags | 11,316 ± 21,916 | 623,530 | 202 | 2,395 |
| `OptionTreeBenchmark` | 6 flags | 43,861 ± 50,432 | 22,755,377 | 5,490 | 491,607 |

Findings:

- Execution costs a few microseconds at the median and does not depend on the number of commands; options and
  variadic arguments add about 2 to 3 µs and 4 to 5 KB.
- Registration allocates about 2 KB per node (about 126 KB per command of this shape), linearly in the number of
  commands, so there is no quadratic step. Most of it is Brigadier's nodes and their child maps. Run on its own, warm,
  registering 1000 commands takes 90 to 130 ms, so the JMH averages above are inflated by the load.
- Suggesting at the root walks every command: Brigadier builds a context and a future for each root child, about
  0.9 KB and 1.3 µs per command at the median.
- Each short flag multiplies the option tree, because every permutation of the combined flags is a literal. Six short
  flags on one command make 5,490 nodes and 23 MB of allocation to register, and 491,607 nodes once the paths are
  expanded, which is what each player receives in the command packet. Keep commands to four short flags or fewer, or
  give the others no short name.
- The option states share their children, but each state still builds its own copy of the argument nodes
  (`argumentNodeSupplier` in `CommandNodeCreator`): 16 of the 61 nodes of this shape, where 2 would do. Building
  them once would remove about a fifth of the nodes to register; the expanded tree that players receive would stay
  the same.

## Future work

- Platform testers (`spigot-testing`, `paper-testing`, `velocity-testing`) wrap the same tree with platform contexts
  and senders; measuring them would add the cost of those adapters and of Paper's Adventure messages.
- Asynchronous executors (`executeAsync`) and suggestion providers that complete later.
- A comparison with other command libraries (Cloud, CommandAPI, Lamp) on the same tree shape. Each needs its own
  server-less harness, so it is left out for now.
- Running `jmhCompare` in CI needs a dedicated runner, since shared runners vary more than these tolerances.
