package com.brunojaegerk.simplekoth;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.utils.Config;
import com.brunojaegerk.simplekoth.command.KothCommand;
import com.brunojaegerk.simplekoth.listener.PlayerConnectionListener;
import com.brunojaegerk.simplekoth.integration.PlaceholderIntegration;
import com.brunojaegerk.simplekoth.manager.KothManager;
import com.brunojaegerk.simplekoth.manager.RegionManager;

import java.io.File;

/**
 * Punto de entrada del plugin. La lógica del evento, la región y los comandos
 * se mantienen separados para que cada parte tenga una responsabilidad clara.
 */
public final class SimpleKothNK extends PluginBase {

    private RegionManager regionManager;
    private KothManager kothManager;
    private Config lang;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("lang.yml", false);
        this.lang = new Config(new File(getDataFolder(), "lang.yml"), Config.YAML);

        this.regionManager = new RegionManager(this);
        this.kothManager = new KothManager(this, regionManager);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderIntegration.register(this, kothManager);
        } else {
            getLogger().info("PlaceholderAPI no está instalado; integración omitida.");
        }

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(kothManager), this
        );
        getServer().getCommandMap().register("koth", new KothCommand(this, regionManager, kothManager));

        // Cuenta regresiva y evento automático se procesan en el hilo principal.
        getServer().getScheduler().scheduleRepeatingTask(this, new PluginTask<SimpleKothNK>(this) {
            @Override
            public void onRun(int currentTick) {
                kothManager.tick();
            }
        }, 20);

        kothManager.initializeAutoSchedule();
        getLogger().info("SimpleKothNK habilitado.");
    }

    @Override
    public void onDisable() {
        if (kothManager != null) {
            kothManager.shutdown();
        }
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public Config getLang() {
        return lang;
    }

    public void reloadLang() {
        this.lang = new Config(new File(getDataFolder(), "lang.yml"), Config.YAML);
    }
}