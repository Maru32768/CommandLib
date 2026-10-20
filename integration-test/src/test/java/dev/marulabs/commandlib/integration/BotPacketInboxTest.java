package dev.marulabs.commandlib.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BotPacketInboxTest {
    @Test
    void keeps_packets_arriving_while_the_previous_batch_is_processed() {
        BotPacketInbox inbox = new BotPacketInbox();
        Object first = new ClientboundSystemChatPacket(1);
        Object second = new ClientboundSystemChatPacket(2);
        inbox.add(first);
        List<Object> batch = inbox.drain();
        inbox.add(second);
        assertThat(batch).containsExactly(first);
        assertThat(inbox.drain()).containsExactly(second);
        assertThat(inbox.drain()).isEmpty();
    }

    @Test
    void concurrent_receives_and_drains_deliver_every_message_once_in_order() throws Exception {
        BotPacketInbox inbox = new BotPacketInbox();
        List<Object> expected = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            expected.add(new ClientboundSystemChatPacket(i));
        }
        CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> expected.forEach(inbox::add));
        List<Object> received = new ArrayList<>();
        while (!producer.isDone()) {
            received.addAll(inbox.drain());
        }
        producer.get(10, TimeUnit.SECONDS);
        received.addAll(inbox.drain());
        assertThat(received).containsExactlyElementsOf(expected);
    }

    @Test
    void retains_assertion_packets_across_protocol_versions_but_not_world_traffic() {
        BotPacketInbox inbox = new BotPacketInbox();
        List<Object> expected = List.of(new ClientboundSystemChatPacket(1),
                                        new ServerChatPacket(),
                                        new ClientboundChatPacket(),
                                        new ClientboundCommandsPacket(),
                                        new ServerDeclareCommandsPacket(),
                                        new ClientboundCommandSuggestionsPacket(),
                                        new ServerTabCompletePacket());
        expected.forEach(inbox::add);
        inbox.add(new ClientboundLevelChunkWithLightPacket());
        assertThat(inbox.hasReceived("ServerDeclareCommandsPacket")).isTrue();
        assertThat(inbox.hasReceived("ClientboundLevelChunkWithLightPacket")).isFalse();
        List<Object> snapshot = inbox.snapshot();
        inbox.clear();
        assertThat(snapshot).containsExactlyElementsOf(expected);
        assertThat(inbox.snapshot()).isEmpty();
    }

    private record ClientboundSystemChatPacket(int id) {
    }

    private record ServerChatPacket() {
    }

    private record ClientboundChatPacket() {
    }

    private record ClientboundCommandsPacket() {
    }

    private record ServerDeclareCommandsPacket() {
    }

    private record ClientboundCommandSuggestionsPacket() {
    }

    private record ServerTabCompletePacket() {
    }

    private record ClientboundLevelChunkWithLightPacket() {
    }
}
