// File: src/main/java/com/example/addon/modules/PlayerWatch.java
package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerWatch extends Module {

    public enum DetectionMode {
        TabList,
        Proximity
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<DetectionMode> detectionMode = sgGeneral.add(new EnumSetting.Builder<DetectionMode>()
        .name("detection-mode")
        .description("How to detect players. Proximity detects any loaded player entity.")
        .defaultValue(DetectionMode.TabList)
        .build()
    );

    private final Setting<List<String>> players = sgGeneral.add(new StringListSetting.Builder()
        .name("players")
        .description("Players to disconnect for.")
        .defaultValue(Collections.emptyList())
        .build()
    );

    public PlayerWatch() {
        super(PlayerWatchAddon.CATEGORY, "player-watch", "Disconnects if a specified player is online or loaded in your world.");
        runInMainMenu = true;
    }

    private boolean canScan = true;

    @Override
    public void onActivate() {
        canScan = true;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        canScan = true;
        info("Joined server. PlayerWatch is now active.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!canScan) return;

        if (mc.world == null || mc.player == null || players.get().isEmpty()) {
            return;
        }

        if (detectionMode.get() == DetectionMode.TabList) {
            scanTabList();
        } else {
            scanProximity();
        }
    }

    // UPDATED: This method now finds ALL matching players before disconnecting.
    private void scanTabList() {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null) return;

        Collection<PlayerListEntry> playerList = networkHandler.getPlayerList();
        if (playerList == null) return;

        // Create a temporary list to hold the names of players we find.
        List<String> foundPlayers = new ArrayList<>();

        // For efficiency, create a set of online player names for quick lookups.
        Set<String> onlinePlayerNames = playerList.stream()
            .map(p -> p.getProfile().getName().toLowerCase())
            .collect(Collectors.toSet());

        // Iterate through our watchlist to see who is online.
        for (String targetName : players.get()) {
            if (onlinePlayerNames.contains(targetName.toLowerCase())) {
                // If a player from our list is online, add them to our temporary list.
                foundPlayers.add(targetName);
            }
        }

        // After checking everyone, if our list of found players is not empty, we disconnect.
        if (!foundPlayers.isEmpty()) {
            // Join the names together with a comma for a clean message.
            String playerListString = String.join(", ", foundPlayers);
            disconnect("Found players in tab list: " + playerListString);
        }
    }

    private void scanProximity() {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity.equals(mc.player)) continue;

            String entityName = playerEntity.getGameProfile().getName();
            if (players.get().stream().anyMatch(targetName -> targetName.equalsIgnoreCase(entityName))) {
                disconnect("Player entity loaded: " + entityName);
                return;
            }
        }
    }

    // UPDATED: This helper method now takes a single, complete reason string.
    private void disconnect(String reason) {
        info("%s. Disconnecting.", reason);
        canScan = false;
        mc.world.disconnect(Text.literal("PlayerWatch: " + reason + "."));
    }


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton refreshButton = theme.button("Refresh Player List");
        refreshButton.action = () -> {
            ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
            if (networkHandler != null && mc.player != null && networkHandler.getPlayerList() != null) {
                List<String> currentPlayers = players.get();
                currentPlayers.clear();
                networkHandler.getPlayerList().forEach(p -> {
                    if (p != null && p.getProfile() != null && !p.getProfile().getName().equalsIgnoreCase(mc.player.getGameProfile().getName())) {
                        currentPlayers.add(p.getProfile().getName());
                    }
                });
                info("Refreshed player list from current server.");
            } else {
                error("You must be on a server to refresh the player list.");
            }
        };
        return refreshButton;
    }
}
