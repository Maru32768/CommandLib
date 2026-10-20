package dev.marulabs.commandlib.integration;

import java.util.ArrayList;
import java.util.List;

/** Packets inspected by the runner, received on the protocol client's network thread. */
final class BotPacketInbox {
    private List<Object> packets = new ArrayList<>();

    synchronized void add(Object packet) {
        switch (packet.getClass()
                      .getSimpleName()) {
            case "ClientboundCommandsPacket", "ServerDeclareCommandsPacket", "ClientboundCommandSuggestionsPacket",
                    "ServerTabCompletePacket", "ClientboundSystemChatPacket", "ClientboundChatPacket",
                    "ServerChatPacket" ->
                packets.add(packet);
            default -> {
                // Chunk, entity and movement packets are handled by MCProtocolLib, not by the assertions.
            }
        }
    }

    synchronized List<Object> snapshot() {
        return List.copyOf(packets);
    }

    synchronized List<Object> drain() {
        List<Object> drained = packets;
        packets = new ArrayList<>();
        return drained;
    }

    synchronized void clear() {
        packets.clear();
    }

    synchronized boolean hasReceived(String... names) {
        List<String> expected = List.of(names);
        return packets.stream()
                      .anyMatch(packet -> expected.contains(packet.getClass()
                                                                  .getSimpleName()));
    }
}
