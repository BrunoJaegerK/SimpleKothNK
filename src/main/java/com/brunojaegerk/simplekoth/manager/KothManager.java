package com.brunojaegerk.simplekoth.manager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.AngryVillagerParticle;
import cn.nukkit.level.particle.ExplodeParticle;
import cn.nukkit.level.particle.FlameParticle;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.DummyBossBar;
import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.model.Region;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Administra el temporizador global del KOTH, la BossBar y los efectos de la zona.
 * Toda la lógica se ejecuta desde el hilo principal del servidor.
 */
public final class KothManager {
    private static final String BOSSBAR_TITLE = "§6Tiempo de captura restante: §e";

    private final SimpleKothNK plugin;
    private final RegionManager regions;
    private final Map<UUID, DummyBossBar> bossBars = new HashMap<>();

    private boolean running;
    private int captureTime;
    private int remainingSeconds;

    public KothManager(SimpleKothNK plugin, RegionManager regions) {
        this.plugin = plugin;
        this.regions = regions;
        this.captureTime = Math.max(1, plugin.getConfig().getInt("capture-time-seconds", 180));
        this.remainingSeconds = captureTime;
    }

    public void start() {
        if (running || !regions.isComplete()) return;

        running = true;
        captureTime = Math.max(1, plugin.getConfig().getInt("capture-time-seconds", 180));
        remainingSeconds = captureTime;

        broadcast(message("messages.start", "§6¡El KOTH ha comenzado!"));
        updateBossBars(getPlayersInside(regions.getRegion()).size());
    }

    public void stop(boolean announce) {
        if (!running) return;

        running = false;
        remainingSeconds = captureTime;
        removeAllBossBars();

        if (announce) {
            broadcast(message("messages.stop", "§cEl KOTH ha terminado."));
        }
    }

    /** Se ejecuta una vez por segundo: el tiempo avanza aunque la zona esté vacía o disputada. */
    public void tick() {
        if (!running) return;

        Region region = regions.getRegion();
        if (region == null) {
            stop(true);
            return;
        }

        List<Player> inside = getPlayersInside(region);

        // Los efectos siempre se originan en la región, nunca sobre los jugadores.
        spawnZoneParticles(region, inside.size() > 1);

        if (remainingSeconds > 0) {
            remainingSeconds--;
        }

        updateBossBars(inside.size());

        // Si el tiempo llega a cero sin participantes, se queda en cero.
        // El primer jugador (o uno elegido al azar si hay varios) que esté dentro gana.
        if (remainingSeconds <= 0 && !inside.isEmpty()) {
            Player winner = inside.get(ThreadLocalRandom.current().nextInt(inside.size()));
            win(winner, region);
        }
    }

    private List<Player> getPlayersInside(Region region) {
        List<Player> inside = new ArrayList<>();
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (player.isOnline() && player.isAlive() && player.getLevel() != null
                    && region.contains(player.getLevel().getName(),
                    player.getFloorX(), player.getFloorY(), player.getFloorZ())) {
                inside.add(player);
            }
        }
        return inside;
    }

    private void win(Player winner, Region region) {
        String winnerName = winner.getName();

        // Se detiene primero para evitar que el evento pueda premiar dos veces.
        running = false;
        remainingSeconds = 0;
        removeAllBossBars();

        broadcast(message("messages.winner", "§a¡{player} ha conquistado el KOTH!")
                .replace("{player}", winnerName));

        playVictoryEffects(region);

        for (String command : plugin.getConfig().getStringList("winner-commands")) {
            if (command == null || command.isBlank()) continue;
            String parsed = command.replace("{player}", winnerName);
            Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(),
                    parsed.startsWith("/") ? parsed.substring(1) : parsed);
        }
    }

    /**
     * Sincroniza la BossBar para cada jugador que ya terminó de entrar al mundo.
     * PlayerJoinEvent puede ocurrir antes del spawn: por eso no se crea la barra
     * hasta que player.spawned sea true. En cada tick se comprueba que siga registrada.
     */
    private void updateBossBars() {
        Region region = regions.getRegion();
        updateBossBars(region == null ? 0 : getPlayersInside(region).size());
    }

    private void updateBossBars(int playersInside) {
        String status = playersInside == 0
                ? "§eEsperando jugador"
                : playersInside > 1 ? "§c¡KOTH disputado!" : "§a¡KOTH en curso!";
        String title = status + " §8| §6Tiempo restante: §e" + formatTime(remainingSeconds);
        float percentage = Math.max(0, Math.min(100,
                (float) remainingSeconds / Math.max(1, captureTime) * 100));

        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (!player.isOnline() || !player.spawned) {
                continue;
            }

            UUID uuid = player.getUniqueId();
            DummyBossBar bar = bossBars.get(uuid);

            // La referencia guardada debe coincidir con la barra que el propio jugador conserva.
            if (bar == null || player.getDummyBossBar(bar.getBossBarId()) != bar) {
                if (bar != null) {
                    player.removeBossBar(bar.getBossBarId());
                }

                bar = new DummyBossBar.Builder(player)
                        .text(title)
                        .length(percentage)
                        .build();
                player.createBossBar(bar);
                bossBars.put(uuid, bar);
            } else {
                bar.setText(title);
                bar.setLength(percentage);
            }

            bar.updateBossEntityPosition();
        }

        // Limpia referencias de jugadores que ya no están conectados.
        bossBars.keySet().removeIf(uuid -> Server.getInstance().getPlayer(uuid) == null);
    }

    /**
     * Partículas distribuidas por el perímetro y el interior de la zona.
     * La disputa usa partículas de enfado en puntos de la región, no en los jugadores.
     */
    private void spawnZoneParticles(Region region, boolean contested) {
        Level level = regionLevel(region);
        if (level == null) return;

        double y = region.getMaxY() + 1.05;
        double minX = region.getMinX() + 0.5;
        double maxX = region.getMaxX() + 0.5;
        double minZ = region.getMinZ() + 0.5;
        double maxZ = region.getMaxZ() + 0.5;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Una nube amplia de partículas cubre el área completa, no solo nueve puntos.
        // En disputa se envían exclusivamente partículas de furia para que el cambio sea evidente.
        int particleCount = contested ? 48 : 36;
        for (int i = 0; i < particleCount; i++) {
            double x = minX + (maxX <= minX ? 0 : random.nextDouble(maxX - minX));
            double z = minZ + (maxZ <= minZ ? 0 : random.nextDouble(maxZ - minZ));
            double particleY = y + random.nextDouble(0.0, 0.65);
            Vector3 point = new Vector3(x, particleY, z);

            if (contested) {
                level.addParticle(new AngryVillagerParticle(point));
            } else {
                level.addParticle(new HappyVillagerParticle(point));
            }
        }

        // Refuerza visualmente el límite de la zona con partículas adicionales.
        int edgeSamples = 12;
        for (int i = 0; i < edgeSamples; i++) {
            double ratio = (double) i / (edgeSamples - 1);
            double x = minX + (maxX - minX) * ratio;
            double z = minZ + (maxZ - minZ) * ratio;

            Vector3[] edgePoints = {
                    new Vector3(x, y, minZ),
                    new Vector3(x, y, maxZ),
                    new Vector3(minX, y, z),
                    new Vector3(maxX, y, z)
            };

            for (Vector3 point : edgePoints) {
                if (contested) {
                    level.addParticle(new AngryVillagerParticle(point));
                } else {
                    level.addParticle(new HappyVillagerParticle(point));
                }
            }
        }
    }

    /** Efectos de fuegos artificiales y sonido reproducidos desde la zona de victoria. */
    private void playVictoryEffects(Region region) {
        Level level = regionLevel(region);
        if (level == null) return;

        double centerX = (region.getMinX() + region.getMaxX() + 1) / 2.0;
        double centerZ = (region.getMinZ() + region.getMaxZ() + 1) / 2.0;
        double baseY = region.getMaxY() + 2.0;

        // Varias explosiones visuales a diferentes alturas para simular una salva.
        for (int i = 0; i < 5; i++) {
            double x = centerX + (i - 2) * 1.8;
            double z = centerZ + ((i % 2 == 0) ? 1.2 : -1.2);
            Vector3 burst = new Vector3(x, baseY + (i % 3) * 2.0, z);

            level.addParticle(new HugeExplodeSeedParticle(burst));
            level.addParticle(new ExplodeParticle(burst));
            level.addParticle(new FlameParticle(burst));
            level.addSound(burst, Sound.FIREWORK_BLAST, 1.8f, 1.0f);
            level.addSound(burst, Sound.FIREWORK_TWINKLE, 1.4f, 1.2f);
        }

        level.addSound(new Vector3(centerX, baseY, centerZ), Sound.RANDOM_LEVELUP, 1.5f, 1.0f);
    }

    private Level regionLevel(Region region) {
        return Server.getInstance().getLevelByName(region.getWorld());
    }

    public void onJoin(Player player) {
        // PlayerJoinEvent puede ejecutarse antes de que el cliente termine de cargar.
        // tick() sincronizará la BossBar cuando player.spawned sea true.
    }

    public void onQuit(Player player) {
        DummyBossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) player.removeBossBar(bar.getBossBarId());
        // Salir o desconectarse no pausa ni reinicia el temporizador global.
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

    public boolean isRunning() {
        return running;
    }

    public int getRemainingSeconds() {
        return running ? Math.max(0, remainingSeconds) : 0;
    }

    public String getRemainingFormatted() {
        return formatTime(getRemainingSeconds());
    }

    public String getStatus() {
        if (!running) return "stopped";
        Region region = regions.getRegion();
        if (region == null) return "waiting";
        int participants = getPlayersInside(region).size();
        return participants > 1 ? "contested" : participants == 1 ? "capturing" : "waiting";
    }

    public void reloadSettings() {
        captureTime = Math.max(1, plugin.getConfig().getInt("capture-time-seconds", 180));
        remainingSeconds = captureTime;
    }

    /** Conserva el placeholder anterior: devuelve el único jugador dentro, si lo hay. */
    public String getCapturerName() {
        Region region = regions.getRegion();
        if (!running || region == null) return "";
        List<Player> inside = getPlayersInside(region);
        return inside.size() == 1 ? inside.get(0).getName() : "";
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%02d:%02d", safe / 60, safe % 60);
    }
}
