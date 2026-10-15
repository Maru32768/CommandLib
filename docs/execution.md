# Execution: Middleware and Async Commands

This page describes what runs around a command's executor: middlewares, which wrap the execution, and async
executors, whose work finishes after the command has returned to the server. The design is in
[design/execution-pipeline.md](design/execution-pipeline.md).

## Where the executor runs

A command input that reaches an executor goes through these steps (see [errors](errors.md) for each failure):

1. The permission of the command node, the options, each argument branch's permission and arguments, and the
   prerequisites (`addPrerequisite`, `require(...)`, `requirePlayer()`). These also run before the generated help.
2. The middlewares, outermost first: the cooldown check, the confirmation, the middlewares and preprocesses of the
   command and its parents in the order they were added, and the start of the cooldown. The generated help skips them.
3. The executor, set with `execute(...)` or `executeAsync(...)`.

## Middleware

A middleware wraps steps 2 and 3. It gets the context and `next`, and returns the stage that completes when the
execution does:

```java
public final class AuditedCommand extends Command {
    public AuditedCommand() {
        super("ban");

        addMiddleware((ctx, next) -> {
            // Before: a check that fails the command with its message.
            if (Maintenance.isActive()) {
                throw new CommandPrerequisiteException("The server is in maintenance.");
            }
            long start = System.nanoTime();
            return next.proceed()
                       // After: runs when the executor is done, also for an async executor.
                       .whenComplete((ignored, e) -> log(ctx.getSender(), System.nanoTime() - start, e));
        });
        argument(new PlayerArgument("target")).execute((target, ctx) -> ctx.sendSuccess("Banned " + target.getName()));
    }
}
```

- **Pre-execution hooks** run before `next.proceed()`. Throw `CommandPrerequisiteException` to fail the command with
  its message, as a prerequisite does. Return a stage without calling `proceed()`, such as
  `CompletableFuture.completedFuture(null)`, to skip the executor without failing.
- **Post-execution hooks** chain on the stage `proceed()` returns: `whenComplete`, `thenRun`, `handle`.
- **Results**: `ctx.setResult(int)` before or after `proceed()` sets the value `/execute store result` stores, as long
  as the execution completes before the dispatch returns.
- **Exception mapping**: `proceed()` never throws. An exception of the executor or of an inner middleware completes its
  stage exceptionally, wrapped in a `CompletionException` once it passed through a stage. Map it with `exceptionally`
  or `handle`:

  ```java
  addMiddleware((ctx, next) -> next.proceed()
                                   .exceptionally(e -> {
                                       Throwable cause = e instanceof CompletionException ? e.getCause() : e;
                                       if (cause instanceof StorageUnavailableException) {
                                           throw new CompletionException(new CommandPrerequisiteException("The database is down."));
                                       }
                                       throw new CompletionException(cause);
                                   }));
  ```

  What reaches the outermost stage is reported as in [errors](errors.md): a `CommandPrerequisiteException` sends its
  message, any other exception is printed, sends the generic error, and reaches the `UncaughtExceptionHandler`s.

A command's middlewares apply to its argument branches and its child commands, the parent's before the child's. A child
that calls `disableParentMiddleware()` inherits none of them, nor the parent's confirmation and cooldown. A middleware
typed for the common context, `CommandMiddleware<CommonCommandContext<?, ?>>`, works on every platform. To apply one to
every command of a plugin, add it to each top-level command.

`addPreprocess(...)`, `confirm(...)` and `cooldown(...)` are middlewares. A preprocess that returns false fails the
command with the messages it sent; one that returns true sends them and continues.

Middlewares and the executor always run on the sync thread: when a middleware calls `proceed()` from another thread,
for example after an async permission lookup, the rest of the execution is scheduled back.

## Async executors

`executeAsync` takes an executor that returns a `CompletionStage`. It runs on the sync thread like `execute`, starts
the work, and the command is done when the stage completes. Every place that takes `execute(...)` also takes
`executeAsync(...)`, with the same typed arguments:

```java
public final class StatsCommand extends Command {
    public StatsCommand(Database database) {
        super("stats");

        argument(new OfflinePlayerArgument("player")).executeAsync((player, ctx) ->
                // Runs on the async executor; the future completes back on the sync thread.
                ctx.supplyAsync(() -> database.loadStats(player.getUniqueId()))
                   // So this runs on the sync thread and may send messages and use the Bukkit API.
                   .thenAccept(stats -> ctx.sendSuccess(player.getName() + " has " + stats.kills() + " kills")));
    }
}
```

- `ctx.supplyAsync(supplier)` and `ctx.runAsync(runnable)` run the work on the async executor and return a future that
  completes on the sync thread. The stages chained on it with the non-async methods (`thenAccept`, `thenApply`,
  `whenComplete`) then run on the sync thread too.
- `ctx.syncExecutor()` and `ctx.asyncExecutor()` are the executors themselves, for a future from another API:
  `luckPerms.getUserManager().loadUser(id).thenAcceptAsync(user -> ctx.sendSuccess(...), ctx.syncExecutor())`.
- `executeAsync(ctx -> ...)` may also throw `CommandPrerequisiteException` before it returns the stage, which fails
  the command at once like a synchronous executor. The typed forms (`(player, ctx) -> ...`) cannot throw checked
  exceptions; return `CompletableFuture.failedFuture(new CommandPrerequisiteException(...))` instead.

### Threads

| Platform | Sync executor | Async executor |
|---|---|---|
| Spigot, Paper | The main thread, through the Bukkit scheduler of the registering plugin; a task given on the main thread runs at once | The Bukkit scheduler's async pool |
| Forge, NeoForge | The server of the command's source (`MinecraftServer#execute`), which runs a task at once on the server thread | A pool of daemon threads owned by CommandLib, growing with the work |
| Velocity | The calling thread: the proxy has no main thread and its API may be used from any thread | The proxy's scheduler, under the registering plugin |
| Testers | `Runnable::run` unless the builder's `scheduler(...)` sets another | `Runnable::run` unless set |

The Bukkit scheduler refuses the tasks of a disabled plugin, so the follow-up of a command still running when its
plugin is disabled is dropped. The scheduler is an interface, `CommandScheduler`, whose sync executor is chosen per
command context, so that Folia's region and entity threads can be supported later; Folia itself is not supported yet.

### The result for `/execute store`

Brigadier needs a command's result when the dispatch returns, so an async command cannot report the outcome of its
work there:

- When the stage is still running as the dispatch returns, the command gives the result set so far, 1 unless the
  executor called `ctx.setResult(...)` before returning the stage. `store result` stores it and `store success` stores
  1. Setting the result later has no effect.
- A failure after that point cannot change what was stored. Its message goes to the sender directly, as
  `ctx.sendFailure` sends it: a `CommandPrerequisiteException` sends its message, any other exception the generic
  error, after the stack trace is printed and the `UncaughtExceptionHandler`s are called on the sync thread.
- When the stage has already completed as the dispatch returns, for example with the testers' default executors or
  when the work found its answer in a cache, the command behaves exactly like a synchronous one: its failure stores 0
  and its result is the one set at the end.

A function or command block that needs the outcome of the work must not depend on an async command.

### Timeout and cancellation

CommandLib sets no timeout, since it cannot stop the work and a slow database is not an error by itself. Bound the work
yourself with `orTimeout`:

```java
executeAsync(ctx -> ctx.supplyAsync(() -> api.fetch(...))
                       .orTimeout(5, TimeUnit.SECONDS)
                       .thenAccept(result -> ctx.sendSuccess(result)));
```

A stage that fails with a `TimeoutException` sends "The command timed out." in red, does not print a stack trace, and
reaches the `UncaughtExceptionHandler`s. A stage that is cancelled (`CancellationException`) reports nothing: cancel
it when the work is no longer wanted. Cancelling a `CompletableFuture` does not interrupt the thread doing the work.
CommandLib does not cancel a command when its sender leaves; messages to a player who has left are dropped by the
platform.

### Thread safety of the context

The parsed arguments, options and inputs of the context do not change once the executor runs, so the async work may
read them. Everything else belongs to the sync thread: sending messages, `setResult`, the sender, the world, the
location and the platform objects (`getSender()`, `getPlayer()`, `getHandle()`). Read what the work needs before
starting it, or return to the sync thread first. `ArgRef#get()` works only on the sync thread while the command runs;
pass the values to the async work instead.

## Testing

The testers run both executors at once by default, so an async command has finished, with its messages sent, when
`CommandTester#execute` returns. To test the state in between, give the builder a scheduler that queues the async
tasks:

```java
Queue<Runnable> asyncTasks = new ArrayDeque<>();
try (CommandTester tester = CommandTester.builder()
                                         .command(new StatsCommand(database))
                                         .permissionPrefix("myplugin.command")
                                         .scheduler(CommandScheduler.of(Runnable::run, asyncTasks::add))
                                         .build()) {
    FakeSender sender = FakeSender.player("Steve");
    tester.execute("stats Alex", sender);
    assertThat(sender.getSentMessageTexts()).isEmpty();

    asyncTasks.forEach(Runnable::run);
    assertThat(sender.getSentMessageTexts()).containsExactly("Alex has 3 kills");
}
```

Work submitted to an executor that the scheduler does not control, such as `CompletableFuture.supplyAsync` without an
executor, finishes on its own thread after `execute` returns; use `ctx.supplyAsync` or `ctx.asyncExecutor()` so the test
controls it.
