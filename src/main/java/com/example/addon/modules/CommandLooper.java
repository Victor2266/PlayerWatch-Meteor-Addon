package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.Arrays;
import java.util.List;

public class CommandLooper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in game ticks between sending each command.")
        .defaultValue(200)
        .min(0)
        .sliderMax(10000)
        .build()
    );

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("A list of commands to execute in a loop.")
        .defaultValue(Arrays.asList(
            "/home",
            "Welcome to my base!",
            "/kit daily"
        ))
        .build()
    );

    private int commandIndex = 0;
    private int timer = 0;

    public CommandLooper() {
        super(PlayerWatchAddon.CATEGORY, "command-looper", "Sends a list of commands in a loop with a variable delay.");
    }

    @Override
    public void onActivate() {
        commandIndex = 0;
        timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || commands.get().isEmpty()) {
            return;
        }

        if (timer <= 0) {
            // Send the command
            ChatUtils.sendPlayerMsg(commands.get().get(commandIndex));

            // Move to the next command
            commandIndex++;
            if (commandIndex >= commands.get().size()) {
                commandIndex = 0; // Loop back to the start
            }

            // Reset the timer
            timer = delay.get();
        } else {
            timer--;
        }
    }
}
