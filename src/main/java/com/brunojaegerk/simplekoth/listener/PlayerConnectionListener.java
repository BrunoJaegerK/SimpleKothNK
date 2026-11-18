package com.brunojaegerk.simplekoth.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import com.brunojaegerk.simplekoth.manager.KothManager;

/** Mantiene las BossBars sincronizadas al entrar o salir del servidor. */
public final class PlayerConnectionListener implements Listener {
    private final KothManager manager;

    public PlayerConnectionListener(KothManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.onJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.onQuit(event.getPlayer());
    }
}
