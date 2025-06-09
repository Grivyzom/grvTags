package gc.grivyzom.commands;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.managers.CategoryManager;
import gc.grivyzom.managers.TagManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
            case "create":
                handleCreate(sender, args);
                break;
            case "createcategory":
                handleCreateCategory(sender, args);
                break;
            case "editor":
                handleEditor(sender, args);
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

            // Recargar tags.yml y categories.yml
            TagManager.loadAllTags();
            CategoryManager.loadAllCategories();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Tags y categorías recargados"));

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

        // Información de managers
        sender.sendMessage(colorize("&7Tags cargados: &f" + TagManager.getTagCount()));
        sender.sendMessage(colorize("&7Categorías cargadas: &f" + CategoryManager.getCategoryCount()));

        sender.sendMessage(colorize("&8&m----------------------------------------"));
    }

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

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags create <nombre> <categoria>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags create VIP default"));
            return;
        }

        String tagName = args[1];
        String categoryName = args[2];

        // Validar nombre del tag
        if (!isValidTagName(tagName)) {
            sender.sendMessage(colorize(PREFIX + "&cNombre de tag inválido."));
            sender.sendMessage(colorize(PREFIX + "&7- Solo se permiten letras, números y guiones bajos"));
            sender.sendMessage(colorize(PREFIX + "&7- Longitud: 2-20 caracteres"));
            sender.sendMessage(colorize(PREFIX + "&7- Ejemplo válido: &fVIP_Premium"));
            return;
        }

        // Validar que la categoría existe
        if (!CategoryManager.categoryExists(categoryName)) {
            sender.sendMessage(colorize(PREFIX + "&cLa categoría '&f" + categoryName + "&c' no existe."));
            sender.sendMessage(colorize(PREFIX + "&7Usa &f/grvtags createcategory " + categoryName + " &7para crearla primero."));
            return;
        }

        // Verificar que el tag no existe
        if (TagManager.tagExists(tagName)) {
            sender.sendMessage(colorize(PREFIX + "&cEl tag '&f" + tagName + "&c' ya existe."));
            return;
        }

        try {
            // Crear el tag en la base de datos
            if (TagManager.createTag(tagName, categoryName, null)) {
                sender.sendMessage(colorize(PREFIX + "&a¡Tag '&f" + tagName + "&a' creado exitosamente!"));
                sender.sendMessage(colorize(PREFIX + "&7Categoría: &f" + categoryName));
                sender.sendMessage(colorize(PREFIX + "&7Usa &f/grvtags editor tag &7para configurar el tag."));

                plugin.getLogger().info("Tag '" + tagName + "' creado por " + sender.getName() + " en categoría '" + categoryName + "'");
            } else {
                sender.sendMessage(colorize(PREFIX + "&c¡Error al crear el tag en la base de datos!"));
            }
        } catch (Exception e) {
            sender.sendMessage(colorize(PREFIX + "&c¡Error al crear el tag!"));
            sender.sendMessage(colorize(PREFIX + "&cError: &f" + e.getMessage()));
            plugin.getLogger().severe("Error al crear tag '" + tagName + "': " + e.getMessage());
        }
    }

    private void handleCreateCategory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags createcategory <nombre>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags createcategory premium"));
            return;
        }

        String categoryName = args[1];

        // Validar nombre de la categoría
        if (!isValidCategoryName(categoryName)) {
            sender.sendMessage(colorize(PREFIX + "&cNombre de categoría inválido."));
            sender.sendMessage(colorize(PREFIX + "&7- Solo se permiten letras, números y guiones bajos"));
            sender.sendMessage(colorize(PREFIX + "&7- Longitud: 2-15 caracteres"));
            sender.sendMessage(colorize(PREFIX + "&7- Ejemplo válido: &fpremium_ranks"));
            return;
        }

        // Verificar que la categoría no existe
        if (CategoryManager.categoryExists(categoryName)) {
            sender.sendMessage(colorize(PREFIX + "&cLa categoría '&f" + categoryName + "&c' ya existe."));
            return;
        }

        try {
            // Crear la categoría en la base de datos
            if (CategoryManager.createCategory(categoryName, categoryName)) {
                sender.sendMessage(colorize(PREFIX + "&a¡Categoría '&f" + categoryName + "&a' creada exitosamente!"));
                sender.sendMessage(colorize(PREFIX + "&7Usa &f/grvtags editor category &7para configurar la categoría."));

                plugin.getLogger().info("Categoría '" + categoryName + "' creada por " + sender.getName());
            } else {
                sender.sendMessage(colorize(PREFIX + "&c¡Error al crear la categoría en la base de datos!"));
            }
        } catch (Exception e) {
            sender.sendMessage(colorize(PREFIX + "&c¡Error al crear la categoría!"));
            sender.sendMessage(colorize(PREFIX + "&cError: &f" + e.getMessage()));
            plugin.getLogger().severe("Error al crear categoría '" + categoryName + "': " + e.getMessage());
        }
    }

    private void handleEditor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(PREFIX + "&cEste comando solo puede ser ejecutado por un jugador."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags editor <category|tag|tags>"));
            sender.sendMessage(colorize(PREFIX + "&7- &fcategory &8- &7Editar categorías"));
            sender.sendMessage(colorize(PREFIX + "&7- &ftag &8- &7Editar un tag específico"));
            sender.sendMessage(colorize(PREFIX + "&7- &ftags &8- &7Ver todos los tags por categoría"));
            return;
        }

        Player player = (Player) sender;
        String editorType = args[1].toLowerCase();

        switch (editorType) {
            case "category":
                openCategoryEditor(player);
                break;
            case "tag":
                openTagEditor(player);
                break;
            case "tags":
                openTagsOverview(player);
                break;
            default:
                sender.sendMessage(colorize(PREFIX + "&cTipo de editor desconocido: &f" + editorType));
                sender.sendMessage(colorize(PREFIX + "&7Tipos disponibles: &fcategory&7, &ftag&7, &ftags"));
                break;
        }
    }

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

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Métodos auxiliares para el editor (a implementar)
    private void openCategoryEditor(Player player) {
        // Implementación pendiente
    }

    private void openTagEditor(Player player) {
        // Implementación pendiente
    }

    private void openTagsOverview(Player player) {
        // Implementación pendiente
    }

    // Métodos de validación (a implementar)
    private boolean isValidTagName(String name) {
        // Implementación pendiente
        return true;
    }

    private boolean isValidCategoryName(String name) {
        // Implementación pendiente
        return true;
    }
}