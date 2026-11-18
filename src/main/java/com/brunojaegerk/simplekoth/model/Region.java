package com.brunojaegerk.simplekoth.model;

/** Región cúbica delimitada por dos posiciones del mismo mundo. */
public final class Region {
    private final String world;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    public Region(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getWorld() { return world; }

    public boolean contains(String playerWorld, int x, int y, int z) {
        return world.equalsIgnoreCase(playerWorld)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
