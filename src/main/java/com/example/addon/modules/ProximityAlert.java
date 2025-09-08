// File: src/main/java/com/example/addon/modules/ProximityAlert.java
package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
// NEW: Import the correct, reliable ChatUtils
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ProximityAlert extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to detect players at.")
        .defaultValue(10.0)
        .min(1)
        .sliderMax(50)
        .build()
    );

    private final Setting<String> onEnterCommand = sgGeneral.add(new StringSetting.Builder()
        .name("on-enter-command")
        .description("Command to run when a player enters the range.")
        // CORRECTED: The default command is now correct.
        .defaultValue("#setting smoothLook true")
        .build()
    );

    private final Setting<String> onLeaveCommand = sgGeneral.add(new StringSetting.Builder()
        .name("on-leave-command")
        .description("Command to run when a player leaves the range.")
        // CORRECTED: The default command is now correct.
        .defaultValue("#setting smoothLook false")
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Ignores players that are on your friends list.")
        .defaultValue(true)
        .build()
    );

    private final Set<PlayerEntity> playersInRange = new HashSet<>();

    public ProximityAlert() {
        super(PlayerWatchAddon.CATEGORY, "proximity-alert", "Runs commands when players enter or leave a specified range.");
    }

    @Override
    public void onDeactivate() {
        // When the module is turned off, clear the list of players and run the onLeave command
        // to ensure settings are reset to their default state.
        if (!playersInRange.isEmpty()) {
            runCommand(onLeaveCommand.get());
        }
        playersInRange.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        // --- Step 1: Get all players currently within range ---
        Set<PlayerEntity> currentPlayers = mc.world.getPlayers().stream()
            .filter(player -> !player.equals(mc.player))
            .filter(player -> mc.player.distanceTo(player) <= range.get())
            .filter(player -> !ignoreFriends.get() || !Friends.get().isFriend(player))
            .collect(Collectors.toSet());

        // --- Step 2: Check for new players who have entered the range ---
        for (PlayerEntity player : currentPlayers) {
            if (!playersInRange.contains(player)) {
                info("%s has entered the range.", player.getGameProfile().getName());
                runCommand(onEnterCommand.get());
                playersInRange.add(player);
            }
        }

        // --- Step 3: Check for players who have left the range ---
        Set<PlayerEntity> playersWhoLeft = new HashSet<>(playersInRange);
        playersWhoLeft.removeAll(currentPlayers);

        // Only run the onLeave command if there is no one left in the zone
        // This prevents the command from spamming if multiple people are in the zone and one leaves.
        if (!playersWhoLeft.isEmpty() && currentPlayers.isEmpty()) {
            info("All players have left the range.");
            runCommand(onLeaveCommand.get());
        }
        // Update our tracked set
        playersInRange.retainAll(currentPlayers);
    }

    // A helper method to run commands cleanly.
    private void runCommand(String command) {
        if (command.isEmpty()) return;

        // CORRECTED: This now uses the reliable ChatUtils.sendPlayerMsg,
        // which works for Baritone, server commands, and plain chat.
        ChatUtils.sendPlayerMsg(command);
    }
}
