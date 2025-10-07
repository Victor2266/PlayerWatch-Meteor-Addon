package com.example.addon;

import com.example.addon.modules.PlayerWatch;
import com.example.addon.modules.AutoLogin;
import com.example.addon.modules.ProximityAlert;
import com.example.addon.modules.AutoTrader;
import com.example.addon.modules.CommandLooper;
import com.example.addon.modules.AutoSeller;
import com.example.addon.modules.StationaryTimer;

import com.mojang.logging.LogUtils;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

import org.slf4j.Logger;

public class PlayerWatchAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Player Watch");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Player Watch Addon");

        // Register the module
        Modules.get().add(new PlayerWatch());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new ProximityAlert());
        Modules.get().add(new AutoTrader());
        Modules.get().add(new CommandLooper());
        Modules.get().add(new AutoSeller());
        Modules.get().add(new StationaryTimer());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        // Remember to change this to your own GitHub repository
        return new GithubRepo("YourUsername", "player-watch");
    }
}
