package com.brunojaegerk.simplekoth;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.PluginTask;
import com.brunojaegerk.simplekoth.command.KothCommand;
import com.brunojaegerk.simplekoth.listener.PlayerConnectionListener;
import com.brunojaegerk.simplekoth.integration.PlaceholderIntegration;
import com.brunojaegerk.simplekoth.manager.KothManager;
import com.brunojaegerk.simplekoth.manager.RegionManager;

/**
 * Punto de entrada del plugin. La lógica del evento, la región y los comandos
 * se mantienen separados para que cada parte tenga una responsabilidad clara.
 */
public final class SimpleKothNK extends PluginBase {

    private RegionManager regionManager;
    private KothManager kothManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.regionManager = new RegionManager(this);
        this.kothManager = new KothManager(this, regionManager);
        PlaceholderIntegration.register(this, kothManager);

        getServer().getPluginManager().registerEvents(
                new PlayerConnectionListener(kothManager), this
        );
        getServer().getCommandMap().register("koth", new KothCommand(this, regionManager, kothManager));

        // Toda la lógica del evento corre en el hilo principal, una vez por segundo.
        getServer().getScheduler().scheduleRepeatingTask(this, new PluginTask<SimpleKothNK>(this) {
            @Override
            public void onRun(int currentTick) {
                kothManager.tick();
            }
        }, 20);

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
}
