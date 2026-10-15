package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;

import java.util.List;

final class RegistryArgumentTestSupport {
    private RegistryArgumentTestSupport() {
    }

    // Vanilla's resource argument rejects an unknown key while parsing on 1.19.3-1.20.4 and when the command runs from
    // 1.20.5, so the failure is either the syntax error or the last message sent.
    static String failure(CommandTester tester, String input, FakeSender sender) {
        try {
            tester.execute(input, sender);
        } catch (RuntimeException e) {
            return String.valueOf(e.getMessage());
        }
        List<String> messages = sender.getSentMessageTexts();
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1);
    }
}
