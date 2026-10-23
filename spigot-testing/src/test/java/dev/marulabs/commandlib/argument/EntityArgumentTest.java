package dev.marulabs.commandlib.argument;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandTester;
import dev.marulabs.commandlib.FakeSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class EntityArgumentTest {
    @Test
    void resolves_fake_entity_by_name() {
        Entity fakeEntity = Mockito.mock(Entity.class);
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("tag") {{
            argument(new EntityArgument("target")).execute((ctx, target) -> {
                ctx.sendMessage("Tagged entity");
            });
        }}, "test.command")) {
            tester.withFakeEntity("mob1", fakeEntity);
            tester.execute("tag mob1", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Tagged entity");
    }

    @Test
    void player_can_be_resolved_as_entity() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("tag") {{
            argument(new EntityArgument("target")).execute((ctx, target) -> {
                ctx.sendMessage("Tagged " + (target instanceof Player ? "player" : "entity"));
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("tag Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("Tagged player");
    }
}
