// File: src/main/java/com/example/addon/modules/AutoTrader.java
package com.example.addon.modules;

import com.example.addon.PlayerWatchAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import java.util.regex.Pattern;

public class AutoTrader extends Module {

    private enum State {
        IDLE,
        OPENING_SHOP,
        NAVIGATING_PAGES,
        SELECTING_ITEM,
        IN_BUY_STACK_SUBMENU,
        IN_SELECT_AMOUNT_SUBMENU,
        SELLING
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // --- General Settings ---
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("action-delay-ticks")
        .description("The delay in game ticks between actions (20 ticks = 1 second).")
        .defaultValue(20)
        .min(1)
        .sliderMax(100)
        .build()
    );

    private final Setting<String> shopCommand = sgGeneral.add(new StringSetting.Builder()
        .name("shop-command")
        .description("The command to open the specific shop category.")
        .defaultValue("/shop blocks")
        .build()
    );

    private final Setting<String> sellCommand = sgGeneral.add(new StringSetting.Builder()
        .name("sell-command")
        .description("The command to sell your inventory.")
        .defaultValue("/sell all")
        .build()
    );

    // --- Navigation Settings ---
    private final Setting<String> shopTitle = sgGeneral.add(new StringSetting.Builder()
        .name("shop-gui-title")
        .description("The text that the main shop GUI title must contain (e.g., 'Blocks').")
        .defaultValue("Blocks")
        .build()
    );

    private final Setting<Integer> targetPage = sgGeneral.add(new IntSetting.Builder()
        .name("target-page")
        .description("The page number you need to navigate to.")
        .defaultValue(4)
        .min(1)
        .build()
    );

    private final Setting<Integer> nextPageSlot = sgGeneral.add(new IntSetting.Builder()
        .name("next-page-slot")
        .description("The slot ID for the 'Next Page' button in the shop.")
        .defaultValue(53)
        .build()
    );

    // --- Buying Settings ---
    private final Setting<Integer> itemSlot = sgGeneral.add(new IntSetting.Builder()
        .name("item-slot")
        .description("The slot ID for the item you want to buy on the target page.")
        .defaultValue(22)
        .build()
    );

    private final Setting<Integer> buyStackSlot = sgGeneral.add(new IntSetting.Builder()
        .name("buy-stack-slot")
        .description("In the first sub-menu, the slot for buying a stack (e.g., the hopper).")
        .defaultValue(31)
        .build()
    );

    private final Setting<Integer> selectAmountSlot = sgGeneral.add(new IntSetting.Builder()
        .name("select-amount-slot")
        .description("In the second sub-menu, the slot for selecting the amount (e.g., 9 stacks).")
        .defaultValue(24)
        .build()
    );

    private final Setting<Integer> purchasesBeforeSell = sgGeneral.add(new IntSetting.Builder()
        .name("purchases-before-sell")
        .description("How many full purchase cycles to complete before selling inventory.")
        .defaultValue(4)
        .min(1)
        .build()
    );


    private State currentState = State.IDLE;
    private int timer = 0;
    private int pageClicks = 0;
    private int purchaseCycles = 0;
    private Pattern pagePattern;

    public AutoTrader() {
        super(PlayerWatchAddon.CATEGORY, "auto-trader", "Automates a complex, multi-menu shop process.");
    }

    @Override
    public void onActivate() {
        resetState("Starting multi-step auto trader...");
        compilePagePattern();
    }

    // REMOVED: The entire onSettingsChanged method that was causing the error.

    @Override
    public void onDeactivate() {
        resetState("Stopping auto trader.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        if (timer > 0) {
            timer--;
            return;
        }

        switch (currentState) {
            case IDLE:
                if (purchaseCycles >= purchasesBeforeSell.get()) {
                    info("Completed %d purchases. Selling inventory...", purchaseCycles);
                    ChatUtils.sendPlayerMsg(sellCommand.get());
                    currentState = State.SELLING;
                    timer = delay.get() * 3;
                    return;
                }
                ChatUtils.sendPlayerMsg(shopCommand.get());
                currentState = State.OPENING_SHOP;
                timer = delay.get();
                break;

            case OPENING_SHOP:
                if (isShopOpen(shopTitle.get())) {
                    info("Shop opened. Navigating to page %d...", targetPage.get());
                    pageClicks = 0;
                    currentState = State.NAVIGATING_PAGES;
                } else {
                    retry("Shop GUI not found.");
                }
                break;

            case NAVIGATING_PAGES:
                if (!isShopOpen(shopTitle.get())) {
                    retry("Shop GUI closed during page navigation.");
                    return;
                }
                if (pageClicks < targetPage.get() - 1) {
                    clickSlot(nextPageSlot.get());
                    pageClicks++;
                    timer = delay.get();
                } else {
                    info("Reached target page. Selecting item...");
                    currentState = State.SELECTING_ITEM;
                    timer = delay.get();
                }
                break;

            case SELECTING_ITEM:
                if (!isShopOnCorrectPage()) {
                    retry("Not on the correct page or shop closed.");
                    return;
                }
                clickSlot(itemSlot.get());
                currentState = State.IN_BUY_STACK_SUBMENU;
                timer = delay.get();
                break;

            case IN_BUY_STACK_SUBMENU:
                clickSlot(buyStackSlot.get());
                currentState = State.IN_SELECT_AMOUNT_SUBMENU;
                timer = delay.get();
                break;

            case IN_SELECT_AMOUNT_SUBMENU:
                clickSlot(selectAmountSlot.get());
                purchaseCycles++;
                info("Purchase cycle %d complete. Returning to shop.", purchaseCycles);
                currentState = State.IDLE;
                timer = delay.get() * 2;
                break;

            case SELLING:
                info("Sell cycle complete. Restarting entire process.");
                resetState("Restarting...");
                break;
        }
    }

    private boolean isShopOpen(String titleSubstring) {
        return mc.currentScreen instanceof GenericContainerScreen screen && screen.getTitle().getString().contains(titleSubstring);
    }

    private boolean isShopOnCorrectPage() {
        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            return pagePattern.matcher(title).find();
        }
        return false;
    }

    private void compilePagePattern() {
        // This regex is built when the module activates, using the current settings.
        String regex = Pattern.quote(shopTitle.get()) + ".*?" + targetPage.get() + "/";
        pagePattern = Pattern.compile(regex);
    }

    private void clickSlot(int slotId) {
        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private void retry(String reason) {
        warning("%s Retrying...", reason);
        resetState(null);
        timer = delay.get() * 2;
    }

    private void resetState(String message) {
        if (message != null) info(message);
        currentState = State.IDLE;
        timer = 0;
        pageClicks = 0;
        purchaseCycles = 0;
    }
}
