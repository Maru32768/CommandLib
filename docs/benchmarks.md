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
about six minutes. `--args` replaces these options, for example to run one benchmark:

```bash
./gradlew -Pcommandlib.typedNms=false :benchmark:jmh --args="-f 1 -wi 3 -i 5 -prof gc -rf json -rff build/results/jmh/results.json SuggestBenchmark"
```

`jmhCompare` compares `results.json` with `benchmark/baseline.json` (another file with `-Pbenchmark.baseline=<path>`),
prints the change of each metric, and fails when one regressed beyond its tolerance. To record a new baseline, copy
`results.json` over `baseline.json` and update the results below.

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

Not recorded yet.

## Future work

- Platform testers (`spigot-testing`, `paper-testing`, `velocity-testing`) wrap the same tree with platform contexts
  and senders; measuring them would add the cost of those adapters and of Paper's Adventure messages.
- Asynchronous executors (`executeAsync`) and suggestion providers that complete later.
- A comparison with other command libraries (Cloud, CommandAPI, Lamp) on the same tree shape. Each needs its own
  server-less harness, so it is left out for now.
- Running `jmhCompare` in CI needs a dedicated runner, since shared runners vary more than these tolerances.
