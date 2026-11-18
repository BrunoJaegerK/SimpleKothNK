package com.brunojaegerk.simplekoth.integration;

import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.manager.KothManager;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;

/**
 * Registra placeholders en PlaceholderAPI-nukkit (no en la edición Bukkit).
 * La dependencia es opcional: el plugin funciona normalmente sin ella.
 */
public final class PlaceholderIntegration {
    private PlaceholderIntegration() {}

    public static void register(SimpleKothNK plugin, KothManager manager) {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                plugin.getLogger().info("PlaceholderAPI no está instalado; integración omitida.");
                return;
            }

            PlaceholderAPI api = PlaceholderAPI.getInstance();
            api.builder("koth_time", String.class)
                    .loader(entry -> manager.getRemainingFormatted())
                    .build();
            api.builder("koth_time_seconds", Integer.class)
                    .loader(entry -> manager.getRemainingSeconds())
                    .build();
            api.builder("koth_status", String.class)
                    .loader(entry -> manager.getStatus())
                    .build();
            api.builder("koth_capturer", String.class)
                    .loader(entry -> manager.getCapturerName())
                    .build();

            plugin.getLogger().info("Placeholders registrados en PlaceholderAPI.");
        } catch (Throwable error) {
            plugin.getLogger().warning("No se pudo integrar PlaceholderAPI: " + error.getMessage());
        }
    }
}
