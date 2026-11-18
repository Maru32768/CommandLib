package dev.marulabs.cadence.argument;

import dev.marulabs.cadence.Command;
import dev.marulabs.cadence.CommandTester;
import dev.marulabs.cadence.FakeSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class EntitiesArgumentTest {
    @Test
    void resolves_fake_entity_as_singleton_list() {
        Entity fakeEntity = Mockito.mock(Entity.class);
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("select") {{
            argument(new EntitiesArgument("targets")).execute((ctx, targets) -> {
                ctx.sendMessage("count=" + targets.size());
            });
        }}, "test.command")) {
            tester.withFakeEntity("mob1", fakeEntity);
            tester.execute("select mob1", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("count=1");
    }

    @Test
    void player_can_be_resolved_as_entity() {
        FakeSender steve = FakeSender.player("Steve");
        FakeSender admin = FakeSender.player("Admin");

        try (CommandTester tester = new CommandTester(() -> new Command("select") {{
            argument(new EntitiesArgument("targets")).execute((ctx, targets) -> {
                ctx.sendMessage((targets.get(0) instanceof Player ? "player" : "entity") + " selected");
            });
        }}, "test.command")) {
            tester.withFakePlayer((Player) steve.asSender());
            tester.execute("select Steve", admin);
        }

        assertThat(admin.getSentMessageTexts()).containsExactly("player selected");
    }
}
