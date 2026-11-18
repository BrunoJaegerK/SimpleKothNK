package com.brunojaegerk.simplekoth.manager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.BossBarColor;
import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.model.Region;

import java.util.*;

/**
 * Controla el estado de captura. Una segunda persona disputa la zona y pausa
 * el contador; salir de la región elimina por completo el progreso del capturador.
 */
public final class KothManager {
    private final SimpleKothNK plugin;
    private final RegionManager regions;
    private final Map<UUID, Long> bossBars = new HashMap<>();
    private boolean running;
    private UUID capturer;
    private int capturedSeconds;
    private final int captureTime;

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
        broadcast(message("messages.start", "§6¡El KOTH ha comenzado!"));
        updateBossBars();
    }

    public void stop(boolean announce) {
        if (!running) return;
        running = false;
        capturer = null;
        capturedSeconds = 0;
        removeAllBossBars();
        if (announce) broadcast(message("messages.stop", "§cEl KOTH ha terminado."));
    }

    /** Se ejecuta cada segundo desde el hilo principal del servidor. */
    public void tick() {
        if (!running) return;

        Region region = regions.getRegion();
        if (region == null) {
            stop(true);
            return;
        }

        List<Player> inside = new ArrayList<>();
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (player.isOnline() && player.isAlive()
                    && region.contains(player.getLevel().getName(),
                    player.getFloorX(), player.getFloorY(), player.getFloorZ())) {
                inside.add(player);
            }
        }

        Player current = findCapturer();
        if (current != null && !inside.contains(current)) {
            // Salir de la zona reinicia, no conserva, el tiempo de captura.
            capturer = null;
            capturedSeconds = 0;
            current = null;
        }

        if (inside.size() > 1) {
            // La disputa pausa el contador. No se asigna un capturador nuevo.
            updateBossBars();
            return;
        }

        if (inside.isEmpty()) {
            capturer = null;
            capturedSeconds = 0;
            updateBossBars();
            return;
        }

        Player onlyPlayer = inside.get(0);
        if (current == null) {
            capturer = onlyPlayer.getUniqueId();
            capturedSeconds = 0;
            broadcast(message("messages.capture-start", "§e{player} está capturando.")
                    .replace("{player}", onlyPlayer.getName()));
        } else if (!current.getUniqueId().equals(onlyPlayer.getUniqueId())) {
            // El anterior capturador ya no está; el jugador restante empieza desde cero.
            capturer = onlyPlayer.getUniqueId();
            capturedSeconds = 0;
        }

        capturedSeconds++;
        if (capturedSeconds >= captureTime) {
            win(onlyPlayer);
            return;
        }
        updateBossBars();
    }

    private Player findCapturer() {
        if (capturer == null) return null;
        Player player = Server.getInstance().getPlayer(capturer);
        return player != null && player.isOnline() ? player : null;
    }

    private void win(Player winner) {
        String winnerName = winner.getName();
        broadcast(message("messages.winner", "§a¡{player} ha ganado el KOTH!")
                .replace("{player}", winnerName));

        for (String command : plugin.getConfig().getStringList("winner-commands")) {
            if (command == null || command.isBlank()) continue;
            String parsed = command.replace("{player}", winnerName);
            Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(),
                    parsed.startsWith("/") ? parsed.substring(1) : parsed);
        }
        stop(false);
    }

    private void updateBossBars() {
        Player current = findCapturer();
        Region region = regions.getRegion();
        int playersInside = 0;
        if (region != null) {
            for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                if (player.isOnline() && player.isAlive()
                        && region.contains(player.getLevel().getName(),
                        player.getFloorX(), player.getFloorY(), player.getFloorZ())) {
                    playersInside++;
                }
            }
        }

        String title;
        if (playersInside > 1) {
            title = "§cKOTH disputado §7| §e" + formatTime(getRemainingSeconds());
        } else if (current != null) {
            title = "§6KOTH §7| §e" + current.getName() + " §7| §f" + formatTime(getRemainingSeconds());
        } else {
            title = "§6KOTH §7| §fEsperando jugador";
        }
        int percentage = Math.max(0, Math.min(100,
                (int) Math.round((double) getRemainingSeconds() / captureTime * 100)));

        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (!player.isOnline()) continue;
            Long id = bossBars.get(player.getUniqueId());
            if (id == null || player.getDummyBossBar(id) == null) {
                id = player.createBossBar(title, percentage);
                bossBars.put(player.getUniqueId(), id);
            } else {
                player.updateBossBar(title, percentage, id);
            }
        }
    }

    public void onJoin(Player player) {
        if (running) updateBossBars();
    }

    public void onQuit(Player player) {
        Long id = bossBars.remove(player.getUniqueId());
        if (id != null) player.removeBossBar(id);
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
            Long id = bossBars.get(player.getUniqueId());
            if (id != null) player.removeBossBar(id);
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
    public int getRemainingSeconds() { return running ? Math.max(0, captureTime - capturedSeconds) : captureTime; }
    public String getRemainingFormatted() { return formatTime(getRemainingSeconds()); }
    public String getStatus() { return !running ? "stopped" : capturer == null ? "waiting" : "capturing"; }
    public String getCapturerName() {
        Player player = findCapturer();
        return player == null ? "" : player.getName();
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%02d:%02d", safe / 60, safe % 60);
    }
}
