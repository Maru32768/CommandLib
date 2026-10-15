package dev.marulabs.commandlib.examples.balance;

import dev.marulabs.commandlib.exception.CommandPrerequisiteException;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

/**
 * The plugin's service the balance commands depend on.
 */
public final class Economy {
    private final Map<String, Integer> balances = new HashMap<>(Map.of("Admin", 100, "Steve", 20));

    /**
     * Maps a sender to its account, failing the command for a sender without one.
     */
    public Account account(CommandSender sender) throws CommandPrerequisiteException {
        if (!balances.containsKey(sender.getName())) {
            throw new CommandPrerequisiteException("You have no account.");
        }
        return new Account(sender.getName());
    }

    public final class Account {
        private final String owner;

        private Account(String owner) {
            this.owner = owner;
        }

        public int balance() {
            return balances.get(owner);
        }

        public void pay(String to, int amount) {
            balances.merge(owner, -amount, Integer::sum);
            balances.merge(to, amount, Integer::sum);
        }
    }
}
