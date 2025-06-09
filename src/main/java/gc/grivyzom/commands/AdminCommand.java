package gc.grivyzom.commands;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final grvTags plugin;
    private final String PREFIX = "&8[&6grvTags&8] ";

    public AdminCommand(grvTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permisos de OP
        if (!sender.isOp()) {
            sender.sendMessage(colorize(PREFIX + "&cNo tienes permisos para ejecutar este comando."));
            plugin.getLogger().warning("Usuario " + sender.getName() + " intentó ejecutar /grvTags sin permisos");
            return true;
        }

        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        // Procesar subcomandos
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "database":
                handleDatabase(sender);
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                sender.sendMessage(colorize(PREFIX + "&cSubcomando desconocido: &f" + subCommand));
                sender.sendMessage(colorize(PREFIX + "&7Usa &f/grvTags help &7para ver los comandos disponibles."));
                break;
        }

        return true;
    }

    /**
     * Maneja el subcomando reload
     */
    private void handleReload(CommandSender sender) {
        sender.sendMessage(colorize(PREFIX + "&7Recargando configuraciones..."));

        try {
            long startTime = System.currentTimeMillis();

            // Recargar config.yml
            plugin.reloadConfig();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7config.yml recargado"));

            // Reinicializar base de datos con nueva configuración
            DatabaseManager.disconnect();
            DatabaseManager.initialize(plugin);

            if (DatabaseManager.testConnection()) {
                sender.sendMessage(colorize(PREFIX + "&a✓ &7Conexión de base de datos reinicializada"));
            } else {
                sender.sendMessage(colorize(PREFIX + "&c✗ &7Error al reinicializar la base de datos"));
            }

            // TODO: Recargar tags.yml y categories.yml cuando estén implementados
            // TagManager.reloadTags();
            // CategoryManager.reloadCategories();

            long endTime = System.currentTimeMillis();
            long reloadTime = endTime - startTime;

            sender.sendMessage(colorize(PREFIX + "&a¡Recarga completada exitosamente!"));
            sender.sendMessage(colorize(PREFIX + "&7Tiempo de recarga: &f" + reloadTime + "ms"));

            plugin.getLogger().info("Configuraciones recargadas por " + sender.getName() + " (" + reloadTime + "ms)");

        } catch (Exception e) {
            sender.sendMessage(colorize(PREFIX + "&c¡Error durante la recarga!"));
            sender.sendMessage(colorize(PREFIX + "&cError: &f" + e.getMessage()));
            plugin.getLogger().severe("Error durante la recarga ejecutada por " + sender.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Maneja el subcomando info
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&6&l              grvTags Info"));
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&7Plugin: &fgrvTags"));
        sender.sendMessage(colorize("&7Versión: &f1.0"));
        sender.sendMessage(colorize("&7Autor: &fBrocolitx"));
        sender.sendMessage(colorize("&7API: &fSpigot 1.20.1"));
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&7Estado de la base de datos: " +
                (DatabaseManager.isConnected() ? "&a✓ Conectada" : "&c✗ Desconectada")));

        // TODO: Añadir más información cuando esté disponible
        // sender.sendMessage(colorize("&7Tags cargados: &f" + TagManager.getTagCount()));
        // sender.sendMessage(colorize("&7Categorías cargadas: &f" + CategoryManager.getCategoryCount()));

        sender.sendMessage(colorize("&8&m----------------------------------------"));
    }

    /**
     * Maneja el subcomando database
     */
    private void handleDatabase(CommandSender sender) {
        sender.sendMessage(colorize(PREFIX + "&7Información de la base de datos:"));
        sender.sendMessage(colorize("&7- Estado: " +
                (DatabaseManager.isConnected() ? "&a✓ Conectada" : "&c✗ Desconectada")));

        if (DatabaseManager.isConnected()) {
            sender.sendMessage(colorize("&7- Host: &f" + plugin.getConfig().getString("database.host")));
            sender.sendMessage(colorize("&7- Puerto: &f" + plugin.getConfig().getInt("database.port")));
            sender.sendMessage(colorize("&7- Base de datos: &f" + plugin.getConfig().getString("database.database")));
            sender.sendMessage(colorize("&7- Usuario: &f" + plugin.getConfig().getString("database.user")));
            sender.sendMessage(colorize("&7- SSL: &f" + plugin.getConfig().getBoolean("database.ssl")));
        }

        // Probar conexión
        sender.sendMessage(colorize(PREFIX + "&7Probando conexión..."));
        if (DatabaseManager.testConnection()) {
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Conexión exitosa"));
        } else {
            sender.sendMessage(colorize(PREFIX + "&c✗ &7Error en la conexión"));
        }
    }

    /**
     * Muestra la ayuda del comando
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&6&l            grvTags Admin"));
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&f/grvTags reload &8- &7Recarga las configuraciones"));
        sender.sendMessage(colorize("&f/grvTags info &8- &7Información del plugin"));
        sender.sendMessage(colorize("&f/grvTags database &8- &7Estado de la base de datos"));
        sender.sendMessage(colorize("&f/grvTags help &8- &7Muestra esta ayuda"));
        sender.sendMessage(colorize("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.isOp()) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "info", "database", "help");
            String partial = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        }

        return completions;
    }

    /**
     * Convierte códigos de color al formato de Minecraft
     * @param message Mensaje con códigos de color
     * @return Mensaje formateado
     */
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}