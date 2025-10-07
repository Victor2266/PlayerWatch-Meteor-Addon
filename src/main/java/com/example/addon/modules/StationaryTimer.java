/*
 * StationaryTimer.java
 */

package com.example.addon.modules;

import meteordevelopment.meteorclient.systems.modules.world.Timer;
import com.example.addon.PlayerWatchAddon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;

public class StationaryTimer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("multiplier")
        .description("The timer multiplier to use when you are standing still.")
        .defaultValue(2.0)
        .min(0.1)
        .sliderMin(0.1)
        .sliderMax(10)
        .build()
    );

    public StationaryTimer() {
        super(PlayerWatchAddon.CATEGORY, "stationary-timer", "Increases timer ONLY when you are standing still. Useful for Baritone.");
    }

    @Override
    public void onDeactivate() {
        // Always reset the timer when the module is turned off
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Timer timer = Modules.get().get(Timer.class);

        // Safety check
        if (mc.player == null) {
            timer.setOverride(Timer.OFF);
            return;
        }

        // If the player is not moving horizontally AND is on the ground, apply the timer override.
        // This prevents the timer from activating while jumping or falling in place.
        if (!PlayerUtils.isMoving() && mc.player.isOnGround()) {
            timer.setOverride(multiplier.get());
        } else {
            timer.setOverride(Timer.OFF);
        }
    }
}
