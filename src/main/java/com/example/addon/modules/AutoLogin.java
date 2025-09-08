// File: src/main/java/com/example/addon/modules/AutoLogin.java
package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in milliseconds between each command.")
        .defaultValue(1500)
        .min(0)
        .sliderMax(10000)
        .build()
    );

    private final Setting<String> serverAddress = sgGeneral.add(new StringSetting.Builder()
        .name("server-address")
        .description("The server address to run the commands on (e.g., mc.hypixel.net).")
        .defaultValue("localhost")
        .build()
    );

    // CHANGED: This is now a list, allowing you to add as many commands as you want.
    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("A list of commands or messages to execute in order.")
        // NEW: A default example showing multiple commands.
        .defaultValue(Arrays.asList(
            "/login password123",
            "/survival",
            "#mine diamond_ore"
        ))
        .build()
    );

    private final Timer timer = new Timer();

    public AutoLogin() {
        super(PlayerWatchAddon.CATEGORY, "auto-login", "Runs multiple commands or sends messages when you join a specific server.");
        runInMainMenu = true;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (!isActive()) return;

        String currentServer = Utils.getWorldName();
        String targetServer = serverAddress.get();
        // Get the list of commands from the settings.
        List<String> commandList = commands.get();

        if (currentServer == null || commandList.isEmpty()) {
            return;
        }

        info("Connected to '%s'. Target address is '%s'.", currentServer, targetServer);

        if (currentServer.equalsIgnoreCase(targetServer)) {
            // NEW: This logic now handles the list of commands.

            // This will track the delay for each command, starting with the initial delay.
            long cumulativeDelay = delay.get();

            // Loop through each command you've added in the settings.
            for (String command : commandList) {
                // For each command, schedule a new, separate task.
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mc.player != null) {
                            // Send the specific command for this task.
                            ChatUtils.sendPlayerMsg(command);
                            info("AutoLogin: Sent command/message: '%s'", command);
                        }
                    }
                }, cumulativeDelay); // Schedule it with the current cumulative delay.

                // Increase the delay for the *next* command in the list.
                cumulativeDelay += delay.get();
            }
        }
    }
}
