# Execution Pipeline

This note describes how CommandLib runs a command once Brigadier has matched it: the middleware chain around the
executor, async executors, and the thread abstraction they share. User documentation is in
[`docs/execution.md`](../execution.md); failure messages are in [`docs/errors.md`](../errors.md).

## Goals

- Cross-cutting behavior (checks, confirmation, cooldown, logging, metrics, exception mapping, result handling) is
  written once as a middleware and inherited down the command tree, instead of being a special case in the runner.
- Heavy work runs off the server thread, and its follow-up (messages, platform API) comes back to the thread that owns
  the sender without the command author managing schedulers.
- One mechanism for both: a middleware's post-execution hook sees the end of an async executor's work, not the moment
  it was started.
- The thread abstraction is narrow enough for Folia's region and entity threads to implement later.
- Synchronous commands keep their exact behavior, including `/execute store result|success` and failure messages.

## Stages

`CommandRunner` (one per Brigadier node that executes) runs:

1. **Checks**: the node's permission, options, each argument branch's permission and parsing, and prerequisites. They
   run before the generated help too, which is why prerequisites stay a separate mechanism (`Prerequisite`,
   `require(...)`) rather than becoming middlewares: a middleware wraps only an executor, and a sender who may not run
   a command should not get its help either. Their failures are reported as before.
2. **Middleware chain**, only when the node has an executor:
   `[cooldown check] [confirmation] [inherited and own middlewares, in order] [cooldown start]`.
   `CommonCommand#middlewares()` builds the list for the command that owns the node; a child inherits the parent's list
   (outer first) unless it calls `disableParentMiddleware()`. The cooldown and the confirmation keep their replace
   semantics (a child's own `confirm(...)` replaces the inherited one) and their fixed positions, so a sender on
   cooldown is not asked to confirm and the cooldown starts only when everything before the executor passed.
3. **Executor**: every executor is stored as an `AsyncCommandExecutor`. `execute(CommandExecutor)` adapts the
   synchronous form with `AsyncCommandExecutor.of`, which runs it and returns a completed stage.

`addPreprocess(Predicate)` is a middleware built by `CommonCommand.preprocessMiddleware`: it collects the messages the
predicate sends (`captureFailure`), fails with them when it returns false, and sends them otherwise. The confirmation
and the cooldown check use the same adapter, so their messages still fail the command like vanilla errors.

## Middleware API

```java
public interface CommandMiddleware<C extends CommonCommandContext<?, ?>> {
    CompletionStage<?> handle(C ctx, Next next) throws CommandPrerequisiteException;

    interface Next {
        CompletableFuture<Void> proceed();
    }
}
```

`handle` returns a stage rather than `void` so that one shape covers synchronous and async executors: a synchronous
chain completes its stages before `handle` returns, an async one completes them later. Alternatives considered:

- `void handle(C ctx, Runnable next)`: simple for pre-hooks, but a post-hook would run when an async executor has only
  started, and exception mapping would need a second mechanism for async failures.
- Separate pre-processor and post-processor lists (Cloud's model): cannot express "around" behavior such as timing,
  retries, or mapping an exception into a result, and splits one concern across two registrations.

`proceed()` never throws: `CommandRunner#step` turns an exception of a middleware or the executor into a failed stage.
A middleware therefore maps exceptions with the standard `CompletionStage` methods only. A middleware may itself throw
`CommandPrerequisiteException` before proceeding, which is the existing way to fail with a message.

`proceed()` schedules the rest of the chain with the sync executor (`supplyAsync(step, sync).thenCompose(identity)`).
Because the sync executors run a task at once on their own thread, this costs nothing during the dispatch, and a
middleware that proceeds after an async lookup still runs the remaining middlewares and the executor on the sync
thread. After the executor (and after each middleware whose stage is not complete yet), `step` brings the completion
back to the sync thread with `whenCompleteAsync(noop, sync)`, so every post-hook runs there. `whenCompleteAsync` is used
rather than `thenApplyAsync`, since the latter completes a failed stage on the completing thread without the executor.

`addMiddleware` takes `CommandMiddleware<? super C>`, so a middleware written for `CommonCommandContext<?, ?>` works on
every platform. A global registration on the platforms' `CommandLib` was left out: each `CommandLib.register` takes a
list of top-level commands, and adding the middleware to each of them has the same effect without new API on three
platforms.

## Completion and results

After the chain returns its stage:

- **Completed** (every synchronous command, and an async one whose work finished already): the runner reports a
  failure exactly as before, by throwing `CommandFailure` to Brigadier, and returns `ctx.getResult()` otherwise.
  Testers use inline executors by default, so async commands are fully testable through `CommandTester#execute`.
- **Pending**: the runner returns the result set so far, 1 unless set, and attaches a handler on the sync executor that
  reports a failure later. `CommandFailure#sendMessages()` sends the collected messages straight to the sender, since
  the dispatch is over and Brigadier can no longer report them.

Vanilla needs a command's result synchronously (`/execute store`, functions, command blocks), so the async outcome
cannot be stored. The options were: return 0 (store success 0), return 1, or block. Blocking the server thread defeats
the feature. Returning 0 would make `store success` report failure for every async command, and `execute if function`
treat them as failed. CommandLib returns what a synchronous command returns when it does not set a result (1), plus any
`setResult` before the stage was returned, so an author can still choose. The documentation tells function authors not
to depend on async outcomes.

Failure mapping (`CommandRunner#toFailure`), the same for both paths:

| Cause (after unwrapping `CompletionException` / `ExecutionException`) | Reported as |
|---|---|
| `CommandFailure` (preprocess, confirmation, cooldown) | its collected messages |
| `CommandPrerequisiteException` | its message |
| `CancellationException` | nothing; the work was cancelled on purpose |
| `TimeoutException` | "The command timed out."; the uncaught exception handlers are called, no stack trace |
| anything else | stack trace, the generic error lines, the uncaught exception handlers |

`CommandFailure#toString()` does not resolve the platform message, since `CompletionException` builds its message from
the cause's `toString()` and the testers have no server to resolve it.

A consequence of running middlewares inside the chain: an unchecked exception of a preprocess is now reported like an
executor's exception (generic error message and handlers) instead of being rethrown to Brigadier's own generic error.

## Timeout and cancellation

CommandLib sets no default timeout. It cannot interrupt the work, so a timeout would only stop waiting, and the right
bound depends on the work. Authors use `CompletableFuture#orTimeout`, whose `TimeoutException` gets the message above.
Cancellation is the author's: cancelling the returned stage (or one the middleware chain sees) reports nothing.
CommandLib does not cancel on sender quit, plugin disable, or server stop; the Bukkit scheduler rejects tasks of a
disabled plugin, which drops the follow-up, and a message to a player who left is dropped by the platform.

## Threads

```java
public interface CommandScheduler {
    Executor sync(CommonCommandContext<?, ?> ctx);
    Executor async(CommonCommandContext<?, ?> ctx);
    static CommandScheduler of(Executor sync, Executor async);
}
```

- **Spigot / Paper**: `BukkitCommandScheduler` (one copy per module, as the modules share no Bukkit code). Sync runs
  inline when `Bukkit.isPrimaryThread()`, else `runTask(plugin, task)`; async is `runTaskAsynchronously`. Paper's
  bootstrap registration has no plugin yet, so the Paper scheduler looks the plugin up by name when it schedules.
- **Forge / NeoForge**: `ServerCommandScheduler`. Sync is the source's `MinecraftServer`, whose `execute` runs inline on
  the server thread and queues otherwise. Async is a cached pool of daemon threads, since command work such as file or
  database access usually blocks and vanilla's worker pool is sized for CPU-bound world work.
- **Velocity**: `VelocityCommandScheduler`. The proxy has no main thread, so sync runs on the calling thread; async
  is the proxy scheduler's `buildTask(plugin, task)`.
- **Testers**: `CommandScheduler.of(Runnable::run, Runnable::run)` unless the builder's `scheduler(...)` sets one.

The scheduler is passed to `CommandNodeCreator` by each platform's `CommandLib` and the testers, and from there to each
`CommandRunner`, which hands it to the context (`CommonCommandContext#scheduler`). The context exposes
`syncExecutor()`, `asyncExecutor()`, `supplyAsync(Supplier)` and `runAsync(Runnable)`; the last two complete their
future on the sync executor, so non-async dependent stages (`thenAccept`) run on the sync thread whether they were
attached before or after completion.

**Folia**: `sync(ctx)` takes the context so that an implementation can pick the sender's entity scheduler (players,
entities) or the region / global scheduler (console, command blocks), and the contract "runs inline when already on
that thread" maps to Folia's ownership checks such as `Bukkit.isOwnedByCurrentRegion(entity)`. Nothing else in the pipeline assumes a single
server thread. Folia itself is out of scope.

## Thread safety of the context

The context is not synchronized. Its parsed arguments, options and inputs are written before the executor runs and
never after, so async work may read them (the future hand-off gives the happens-before edge). Sending messages,
`setResult`, the failure capture, the sender and platform objects are sync-thread only; `ArgRef#get()` reads a
thread-local that is set only while the runner runs on the sync thread. Sending from another thread is not hopped
automatically: doing so would hide the threading from authors in one place while every platform API they call next
still requires the sync thread.

## Testing

- `common` tests use `FakeScheduler`, which queues sync tasks only when they come from an async task or another
  thread, mirroring the platforms' "inline on the sync thread" contract, and lets a test run the async and sync queues
  step by step (`AsyncExecutionTest`, `CommandMiddlewareTest`).
- `spigot` and `paper-testing` mock `Bukkit`'s scheduler to check `BukkitCommandScheduler`; `modded-testing` checks
  `ServerCommandScheduler` with a mocked source and server; `velocity-testing` checks `VelocityCommandScheduler` with
  a mocked proxy.
- Each tester has an `AsyncCommandTest` with a queued async executor. `spigot-testing` passes the sender as the
  Brigadier source, so the mock NMS wrapper still finds it in a follow-up that runs after `execute` returned.

## Not done

- Folia scheduling.
- A platform-wide middleware registration.
- Dependency injection hooks, sender mappers and context enrichment beyond what a middleware can do with the existing
  context.
