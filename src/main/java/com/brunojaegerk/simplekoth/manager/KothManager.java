package com.brunojaegerk.simplekoth.manager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.particle.AngryVillagerParticle;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.DummyBossBar;
import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.model.Region;

import java.util.*;

/**
 * Controla la captura individual, la bossbar y los efectos visuales del KOTH.
 * Toda esta clase se ejecuta desde el hilo principal del servidor.
 */
public final class KothManager {
    private final SimpleKothNK plugin;
    private final RegionManager regions;
    private final Map<UUID, DummyBossBar> bossBars = new HashMap<>();

    private boolean running;
    private UUID capturer;
    private int capturedSeconds;
    private int captureTime;
    private boolean contested;

    public KothManager(SimpleKothNK plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
        this.captureTime = Math.max(1, plugin.getConfig().getInt("capture-time-seconds", 180));
    }

    public void start() {
        if (running || !regions.isComplete()) return;

        running = true;
        capturer = null;
        capturedSeconds = 0;
        contested = false;

        broadcast(message("messages.start", "§6¡El KOTH ha comenzado!"));
        updateBossBars();
    }

    public void stop(boolean announce) {
        if (!running) return;

        running = false;
        capturer = null;
        capturedSeconds = 0;
        contested = false;
        removeAllBossBars();

        if (announce) {
            broadcast(message("messages.stop", "§cEl KOTH ha terminado."));
        }
    }

    /** Se ejecuta una vez por segundo desde el scheduler principal. */
    public void tick() {
        if (!running) return;

        Region region = regions.getRegion();
        if (region == null) {
            stop(true);
            return;
        }

        List<Player> inside = getPlayersInside(region);
        Player current = findCapturer();

        // Si el capturador abandona la región, pierde todo el progreso.
        if (current != null && !inside.contains(current)) {
            capturer = null;
            capturedSeconds = 0;
            current = null;
        }

        if (inside.size() > 1) {
            // La disputa pausa el contador; no se envían mensajes repetitivos al chat.
            contested = true;
            spawnContestedParticles(inside);
            updateBossBars();
            return;
        }

        contested = false;
        if (inside.isEmpty()) {
            capturer = null;
            capturedSeconds = 0;
            updateBossBars();
            return;
        }

        Player onlyPlayer = inside.get(0);
        if (current == null || !current.getUniqueId().equals(onlyPlayer.getUniqueId())) {
            // Un nuevo jugador comienza siempre desde cero.
            capturer = onlyPlayer.getUniqueId();
            capturedSeconds = 0;
        }

        capturedSeconds++;
        spawnCaptureParticles(region);

        if (capturedSeconds >= captureTime) {
            win(onlyPlayer);
            return;
        }

        updateBossBars();
    }

    private List<Player> getPlayersInside(Region region) {
        List<Player> inside = new ArrayList<>();
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (player.isOnline() && player.isAlive()
                    && region.contains(player.getLevel().getName(),
                    player.getFloorX(), player.getFloorY(), player.getFloorZ())) {
                inside.add(player);
            }
        }
        return inside;
    }

    private Player findCapturer() {
        if (capturer == null) return null;
        return Server.getInstance().getPlayer(capturer)
                .filter(Player::isOnline)
                .orElse(null);
    }

    private void win(Player winner) {
        String winnerName = winner.getName();
        broadcast(message("messages.winner", "§a¡{player} ha conquistado el KOTH!")
                .replace("{player}", winnerName));

        for (String command : plugin.getConfig().getStringList("winner-commands")) {
            if (command == null || command.isBlank()) continue;
            String parsed = command.replace("{player}", winnerName);
            Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(),
                    parsed.startsWith("/") ? parsed.substring(1) : parsed);
        }

        stop(false);
    }

    /**
     * Mantiene una única DummyBossBar por jugador. La entidad invisible que
     * sostiene la barra debe seguir la posición del jugador; si no se actualiza,
     * el cliente puede dejar de mostrarla al alejarse de su posición inicial.
     */
    private void updateBossBars() {
        Region region = regions.getRegion();
        List<Player> inside = region == null ? Collections.emptyList() : getPlayersInside(region);
        Player current = findCapturer();

        String title;
        if (inside.size() > 1 || contested) {
            title = "§cKOTH disputado §7| §e" + formatTime(getRemainingSeconds());
        } else if (current != null) {
            title = "§6KOTH §7| §e" + current.getName()
                    + " §7| §f" + formatTime(getRemainingSeconds());
        } else {
            title = "§6KOTH §7| §fEsperando jugador §7| §e" + formatTime(getRemainingSeconds());
        }

        float percentage = Math.max(0, Math.min(100,
                (float) capturedSeconds / captureTime * 100));

        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (!player.isOnline()) continue;

            DummyBossBar bar = bossBars.get(player.getUniqueId());
            if (bar == null || player.getDummyBossBar(bar.getBossBarId()) == null) {
                bar = new DummyBossBar.Builder(player)
                        .text(title)
                        .length(percentage)
                        .build();
                player.createBossBar(bar);
                bossBars.put(player.getUniqueId(), bar);
            } else {
                bar.setText(title);
                bar.setLength(percentage);
            }

            // Se actualiza también durante espera y disputa para que no desaparezca.
            bar.updateBossEntityPosition();
        }
    }

    /** Brillos verdes alrededor del perímetro mientras un jugador captura. */
    private void spawnCaptureParticles(Region region) {
        double y = region.getMaxY() + 1.15;
        double minX = region.getMinX() + 0.5;
        double maxX = region.getMaxX() + 0.5;
        double minZ = region.getMinZ() + 0.5;
        double maxZ = region.getMaxZ() + 0.5;
        double midX = (minX + maxX) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        addHappyParticle(region, minX, y, minZ);
        addHappyParticle(region, minX, y, maxZ);
        addHappyParticle(region, maxX, y, minZ);
        addHappyParticle(region, maxX, y, maxZ);
        addHappyParticle(region, midX, y, minZ);
        addHappyParticle(region, midX, y, maxZ);
        addHappyParticle(region, minX, y, midZ);
        addHappyParticle(region, maxX, y, midZ);
    }

    private void addHappyParticle(Region region, double x, double y, double z) {
        cn.nukkit.level.Level level = regionLevel(region);
        if (level != null) {
            level.addParticle(new HappyVillagerParticle(new Vector3(x, y, z)));
        }
    }

    /** Partículas de enfado sobre cada jugador cuando la zona está disputada. */
    private void spawnContestedParticles(List<Player> players) {
        for (Player player : players) {
            Vector3 position = new Vector3(player.getX(), player.getY() + 2.15, player.getZ());
            player.getLevel().addParticle(new AngryVillagerParticle(position), player);
        }
    }

    private cn.nukkit.level.Level regionLevel(Region region) {
        return Server.getInstance().getLevelByName(region.getWorld());
    }

    public void onJoin(Player player) {
        if (running) updateBossBars();
    }

    public void onQuit(Player player) {
        DummyBossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) player.removeBossBar(bar.getBossBarId());

        if (capturer != null && capturer.equals(player.getUniqueId())) {
            capturer = null;
            capturedSeconds = 0;
        }
    }

    public void shutdown() {
        stop(false);
        removeAllBossBars();
    }

    private void removeAllBossBars() {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            DummyBossBar bar = bossBars.get(player.getUniqueId());
            if (bar != null) player.removeBossBar(bar.getBossBarId());
        }
        bossBars.clear();
    }

    private void broadcast(String text) {
        Server.getInstance().broadcastMessage(prefix() + text);
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "§6[KOTH] §r");
    }

    private String message(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    public boolean isRunning() { return running; }
    public int getRemainingSeconds() {
        return running ? Math.max(0, captureTime - capturedSeconds) : 0;
    }
    public String getRemainingFormatted() { return formatTime(getRemainingSeconds()); }
    public String getStatus() {
        return !running ? "stopped" : contested ? "contested"
                : capturer == null ? "waiting" : "capturing";
    }

    public void reloadSettings() {
        captureTime = Math.max(1, plugin.getConfig().getInt("capture-time-seconds", 180));
        capturedSeconds = 0;
        capturer = null;
        contested = false;
    }

    public String getCapturerName() {
        Player player = findCapturer();
        return player == null ? "" : player.getName();
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%02d:%02d", safe / 60, safe % 60);
    }
}
