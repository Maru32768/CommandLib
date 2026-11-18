package dev.marulabs.cadence;

class TestCommand extends CommonCommand<TestCommandContext, TestCommand> {
    TestCommand(String name) {
        super(name);
    }
}
