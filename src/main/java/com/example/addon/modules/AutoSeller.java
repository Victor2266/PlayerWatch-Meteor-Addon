package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class AutoSeller extends Module {

    private enum SellMode {
        COMMAND,
        GUI
    }

    private enum State {
        IDLE,
        WAITING_FOR_GUI,
        SELLING_ITEMS
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- General Settings ---
    private final Setting<SellMode> sellMode = sgGeneral.add(new EnumSetting.Builder<SellMode>()
        .name("sell-mode")
        .description("The method to use for selling items.")
        .defaultValue(SellMode.COMMAND)
        .build()
    );

    private final Setting<String> sellCommand = sgGeneral.add(new StringSetting.Builder()
        .name("sell-command")
        .description("The command to execute to sell items.")
        .defaultValue("/sell all")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("The delay in game ticks between actions in GUI mode.")
        .defaultValue(10)
        .min(1)
        .sliderMax(60)
        .visible(() -> sellMode.get() == SellMode.GUI)
        .build()
    );

    // --- GUI Mode Settings ---
    private final Setting<String> sellGuiTitle = sgGeneral.add(new StringSetting.Builder()
        .name("gui-title")
        .description("The text that the sell GUI title must contain.")
        .defaultValue("Sell GUI")
        .visible(() -> sellMode.get() == SellMode.GUI)
        .build()
    );

    private final Setting<List<Item>> whitelistedItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("whitelisted-items")
        .description("Items to be sold when in GUI mode.")
        .visible(() -> sellMode.get() == SellMode.GUI)
        .build()
    );

    private State currentState = State.IDLE;
    private int timer = 0;
    private int currentSlot = 0;

    public AutoSeller() {
        super(PlayerWatchAddon.CATEGORY, "auto-seller", "Automatically sells items when your inventory is full.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        currentState = State.IDLE;
        timer = 0;
        currentSlot = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        switch (currentState) {
            case IDLE:
                if (isInventoryFull()) {
                    info("Inventory is full, starting sell process...");
                    ChatUtils.sendPlayerMsg(sellCommand.get());

                    if (sellMode.get() == SellMode.GUI) {
                        currentState = State.WAITING_FOR_GUI;
                        timer = 20; // Wait 1 second for the GUI to open
                    } else {
                        timer = 200; // Wait 10 seconds before trying to sell again in command mode
                    }
                }
                break;

            case WAITING_FOR_GUI:
                if (isSellGuiOpen()) {
                    info("Sell GUI detected, starting to sell items.");
                    currentState = State.SELLING_ITEMS;
                    currentSlot = 0;
                } else {
                    warning("Sell GUI did not open. Aborting and returning to idle.");
                    reset();
                }
                break;

            case SELLING_ITEMS:
                if (!isSellGuiOpen()) {
                    info("Sell GUI closed. Process finished.");
                    reset();
                    return;
                }

                GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
                boolean soldItemThisTick = false;

                // Iterate through the player's entire main inventory, including hotbar (36 slots total)
                for (int i = currentSlot; i < 36; i++) {
                    if (whitelistedItems.get().contains(mc.player.getInventory().getStack(i).getItem())) {
                        // **FIXED LOGIC:** Correctly calculate the slotId based on whether it's in the hotbar or main inventory.
                        int slotId;
                        if (i >= 0 && i <= 8) {
                            // Hotbar slots (0-8) are placed after the main inventory rows in the container.
                            slotId = screen.getScreenHandler().slots.size() - 9 + i;
                        } else {
                            // Main inventory slots (9-35) are placed right after the GUI's own slots.
                            slotId = (screen.getScreenHandler().slots.size() - 36) + (i - 9);
                        }

                        info("Found whitelisted item '%s' in player inventory slot %d. Clicking container slot %d.", mc.player.getInventory().getStack(i).getItem().getName().getString(), i, slotId);
                        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);

                        currentSlot = i + 1;
                        timer = delay.get();
                        soldItemThisTick = true;
                        break; // Exit loop for this tick to respect the delay
                    }
                }

                if (!soldItemThisTick && currentSlot >= 35) {
                    info("All whitelisted items sold. Closing GUI.");
                    mc.player.closeHandledScreen();
                    reset();
                }
                break;
        }
    }

    private boolean isInventoryFull() {
        return mc.player.getInventory().getEmptySlot() == -1;
    }

    private boolean isSellGuiOpen() {
        return mc.currentScreen instanceof GenericContainerScreen screen &&
               screen.getTitle().getString().contains(sellGuiTitle.get());
    }
}
