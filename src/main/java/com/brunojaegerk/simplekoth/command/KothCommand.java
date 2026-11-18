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
            sender.sendMessage("§cNo tienes permiso.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/koth set pos1 §7- Selecciona la primera esquina.");
            sender.sendMessage("§e/koth set pos2 §7- Selecciona la segunda esquina.");
            sender.sendMessage("§e/koth start §7- Inicia el evento.");
            sender.sendMessage("§e/koth stop §7- Detiene el evento.");
            sender.sendMessage("§e/koth status §7- Muestra el estado.");
            sender.sendMessage("§e/koth reload §7- Recarga la configuración.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage("§cUso: /koth set pos1|pos2 (desde el juego).");
                    return true;
                }
                int position = args[1].equalsIgnoreCase("pos1") ? 1
                        : args[1].equalsIgnoreCase("pos2") ? 2 : 0;
                if (position == 0) {
                    sender.sendMessage("§cUso: /koth set pos1|pos2");
                    return true;
                }
                regions.setPosition(player, position);
                sender.sendMessage(plugin.getConfig().getString(
                        position == 1 ? "messages.pos1" : "messages.pos2",
                        "§aPosición establecida."));
                return true;
            }
            case "start" -> {
                if (koth.isRunning()) {
                    sender.sendMessage(plugin.getConfig().getString("messages.already-running", "§cYa está activo."));
                } else if (!regions.isComplete()) {
                    sender.sendMessage(plugin.getConfig().getString("messages.no-region", "§cConfigura la región primero."));
                } else {
                    koth.start();
                    sender.sendMessage(plugin.getConfig().getString("messages.started", "§aKOTH iniciado."));
                }
                return true;
            }
            case "stop" -> {
                if (!koth.isRunning()) {
                    sender.sendMessage(plugin.getConfig().getString("messages.not-running", "§cNo hay evento activo."));
                } else {
                    koth.stop(true);
                    sender.sendMessage(plugin.getConfig().getString("messages.stopped", "§aKOTH detenido."));
                }
                return true;
            }
            case "status" -> {
                sender.sendMessage("§eKOTH: " + (koth.isRunning() ? "§aActivo" : "§cInactivo"));
                sender.sendMessage("§eTiempo restante: §f" + koth.getRemainingFormatted());
                sender.sendMessage("§eCapturador: §f" + (koth.getCapturerName().isEmpty() ? "Ninguno" : koth.getCapturerName()));
                return true;
            }
            case "reload" -> {
                if (koth.isRunning()) koth.stop(false);
                plugin.reloadConfig();
                regions.load();
                koth.reloadSettings();
                sender.sendMessage("§aConfiguración recargada.");
                return true;
            }
            default -> {
                sender.sendMessage("§cSubcomando desconocido. Usa /koth.");
                return true;
            }
        }
    }
}
