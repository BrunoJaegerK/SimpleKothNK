package com.brunojaegerk.simplekoth.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.brunojaegerk.simplekoth.SimpleKothNK;
import com.brunojaegerk.simplekoth.manager.KothManager;
import com.brunojaegerk.simplekoth.manager.RegionManager;

/** Comandos administrativos del KOTH. */
public final class KothCommand extends Command {
    private final SimpleKothNK plugin;
    private final RegionManager regions;
    private final KothManager koth;

    public KothCommand(SimpleKothNK plugin, RegionManager regions, KothManager koth) {
        super("koth", "Administra el evento KOTH", "/koth <set|start|stop|status|reload>");
        this.plugin = plugin;
        this.regions = regions;
        this.koth = koth;
        setPermission("simplekoth.admin");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("simplekoth.admin")) {
            sender.sendMessage(plugin.getLang().getString("command.no-permission", "§cNo tienes permiso."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.getLang().getString("help.pos1", "§e/koth set pos1 §7- Selecciona la primera esquina."));
            sender.sendMessage(plugin.getLang().getString("help.pos2", "§e/koth set pos2 §7- Selecciona la segunda esquina."));
            sender.sendMessage(plugin.getLang().getString("help.start", "§e/koth start §7- Programa el inicio del evento."));
            sender.sendMessage(plugin.getLang().getString("help.stop", "§e/koth stop §7- Detiene el evento."));
            sender.sendMessage(plugin.getLang().getString("help.status", "§e/koth status §7- Muestra el estado."));
            sender.sendMessage(plugin.getLang().getString("help.reload", "§e/koth reload §7- Recarga la configuración."));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(plugin.getLang().getString("command.set-usage", "§cUso: /koth set pos1|pos2 (desde el juego)."));
                    return true;
                }
                int position = args[1].equalsIgnoreCase("pos1") ? 1
                        : args[1].equalsIgnoreCase("pos2") ? 2 : 0;
                if (position == 0) {
                    sender.sendMessage(plugin.getLang().getString("command.set-usage", "§cUso: /koth set pos1|pos2"));
                    return true;
                }
                regions.setPosition(player, position);
                sender.sendMessage(plugin.getLang().getString(
                        position == 1 ? "region.pos1-set" : "region.pos2-set",
                        "§aPosición establecida."));
                return true;
            }
            case "start" -> {
                if (koth.isRunning() || koth.isStarting()) {
                    sender.sendMessage(plugin.getLang().getString("command.already-active",
                            "§cEl KOTH ya está activo o tiene una cuenta regresiva."));
                } else if (!regions.isComplete()) {
                    sender.sendMessage(plugin.getLang().getString("command.no-region",
                            "§cConfigura la región primero."));
                } else if (koth.start()) {
                    sender.sendMessage(plugin.getLang().getString("command.countdown-queued",
                            "§aCuenta regresiva del KOTH programada."));
                }
                return true;
            }
            case "stop" -> {
                if (!koth.isRunning() && !koth.isStarting()) {
                    sender.sendMessage(plugin.getLang().getString("command.not-active",
                            "§cNo hay evento ni cuenta regresiva activa."));
                } else {
                    koth.stop(true);
                    sender.sendMessage(plugin.getLang().getString("command.stopped",
                            "§aKOTH detenido."));
                }
                return true;
            }
            case "status" -> {
                String state = koth.isRunning()
                        ? plugin.getLang().getString("status.running", "§aActivo")
                        : koth.isStarting()
                        ? plugin.getLang().getString("status.starting", "§eIniciando en {seconds}s")
                                .replace("{seconds}", String.valueOf(koth.getCountdownRemaining()))
                        : plugin.getLang().getString("status.stopped", "§cInactivo");
                sender.sendMessage(plugin.getLang().getString("command.status", "§eKOTH: ") + state);
                if (koth.isRunning()) {
                    sender.sendMessage(plugin.getLang().getString("command.remaining", "§eTiempo restante: §f") + koth.getRemainingFormatted());
                }
                sender.sendMessage(plugin.getLang().getString("command.capturer", "§eCapturador: §f")
                        + (koth.getCapturerName().isEmpty()
                        ? plugin.getLang().getString("status.none", "Ninguno")
                        : koth.getCapturerName()));
                return true;
            }
            case "reload" -> {
                if (koth.isRunning() || koth.isStarting()) koth.stop(false);
                plugin.reloadConfig();
                plugin.reloadLang();
                regions.load();
                koth.reloadSettings();
                sender.sendMessage(plugin.getLang().getString("command.reloaded", "§aConfiguración recargada."));
                return true;
            }
            default -> {
                sender.sendMessage(plugin.getLang().getString("command.unknown", "§cSubcomando desconocido. Usa /koth."));
                return true;
            }
        }
    }
}
