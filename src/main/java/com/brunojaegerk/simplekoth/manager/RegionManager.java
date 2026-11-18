package com.brunojaegerk.simplekoth.manager;

import cn.nukkit.Player;
import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.model.Region;

/** Carga y persistencia de la única zona KOTH configurada. */
public final class RegionManager {
    private final SimpleKothNK plugin;
    private Region region;
    private String pos1World;
    private int x1, y1, z1;
    private String pos2World;
    private int x2, y2, z2;

    public RegionManager(SimpleKothNK plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        var config = plugin.getConfig();
        pos1World = config.getString("region.pos1-world", "");
        pos2World = config.getString("region.pos2-world", "");
        x1 = config.getInt("region.pos1.x");
        y1 = config.getInt("region.pos1.y");
        z1 = config.getInt("region.pos1.z");
        x2 = config.getInt("region.pos2.x");
        y2 = config.getInt("region.pos2.y");
        z2 = config.getInt("region.pos2.z");
        rebuild();
    }

    public boolean setPosition(Player player, int position) {
        String world = player.getLevel().getName();
        int x = player.getFloorX(), y = player.getFloorY(), z = player.getFloorZ();

        if (position == 1) {
            pos1World = world;
            x1 = x; y1 = y; z1 = z;
            plugin.getConfig().set("region.pos1-world", world);
            plugin.getConfig().set("region.pos1.x", x);
            plugin.getConfig().set("region.pos1.y", y);
            plugin.getConfig().set("region.pos1.z", z);
        } else {
            pos2World = world;
            x2 = x; y2 = y; z2 = z;
            plugin.getConfig().set("region.pos2-world", world);
            plugin.getConfig().set("region.pos2.x", x);
            plugin.getConfig().set("region.pos2.y", y);
            plugin.getConfig().set("region.pos2.z", z);
        }
        plugin.saveConfig();
        rebuild();
        return region != null;
    }

    private void rebuild() {
        if (pos1World == null || pos2World == null || pos1World.isEmpty()
                || !pos1World.equalsIgnoreCase(pos2World)) {
            region = null;
            return;
        }
        region = new Region(pos1World, x1, y1, z1, x2, y2, z2);
    }

    public Region getRegion() { return region; }
    public boolean isComplete() { return region != null; }
}
