package gc.grivyzom.commands;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.managers.CategoryManager;
import gc.grivyzom.managers.PlayerDataManager;
import gc.grivyzom.managers.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
            case "give":
                handleGiveTag(sender, args);
                break;
            case "take":
                handleTakeTag(sender, args);
                break;
            case "set":
                handleSetTag(sender, args);
                break;
            case "check":
                handleCheckPlayer(sender, args);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Solo OP puede usar autocompletado
        if (!sender.isOp()) {
            return completions;
        }

        // Primer argumento: subcomandos principales
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "reload", "info", "database", "create", "createcategory", "editor",
                    "give", "take", "set", "check", "help"
            );

            String partial = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
            return completions;
        }

        // Autocompletado específico por subcomando
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreateTabComplete(args);
            case "createcategory":
                return handleCreateCategoryTabComplete(args);
            case "editor":
                return handleEditorTabComplete(args);
            case "give":
            case "take":
            case "set":
                return handlePlayerTagTabComplete(args);
            case "check":
                return handleCheckTabComplete(args);
            default:
                return completions;
        }
    }

    /**
     * Autocompletado para comandos que requieren jugador y tag
     */
    private List<String> handlePlayerTagTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Segundo argumento: nombres de jugadores
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            // Tercer argumento: nombres de tags
            List<String> tagNames = TagManager.getAllTagNames();
            String partial = args[2].toLowerCase();

            for (String tagName : tagNames) {
                if (tagName.toLowerCase().startsWith(partial)) {
                    completions.add(tagName);
                }
            }
        }

        return completions;
    }

    /**
     * Autocompletado para el comando check
     */
    private List<String> handleCheckTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Segundo argumento: nombres de jugadores
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    /**
     * Maneja el comando give para dar un tag a un jugador
     */
    private void handleGiveTag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags give <jugador> <tag>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags give Brocolitx example"));
            return;
        }

        String playerName = args[1];
        String tagName = args[2];

        // Verificar que el tag existe
        if (!TagManager.tagExists(tagName)) {
            sender.sendMessage(colorize(PREFIX + "&cEl tag '&f" + tagName + "&c' no existe."));
            return;
        }

        // Obtener el jugador (puede estar offline)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(colorize(PREFIX + "&cEl jugador '&f" + playerName + "&c' nunca ha jugado en el servidor."));
            return;
        }

        // Dar el tag al jugador
        if (PlayerDataManager.unlockTagForPlayer(offlinePlayer.getUniqueId(), tagName)) {
            sender.sendMessage(colorize(PREFIX + "&a¡Tag '&f" + tagName + "&a' dado exitosamente a &f" + playerName + "&a!"));

            // Notificar al jugador si está online
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(colorize(PREFIX + "&a¡Has desbloqueado el tag '&f" + tagName + "&a'!"));
            }

            plugin.getLogger().info("Tag '" + tagName + "' dado a " + playerName + " por " + sender.getName());
        } else {
            sender.sendMessage(colorize(PREFIX + "&cError al dar el tag. El jugador puede que ya lo tenga."));
        }
    }

    /**
     * Maneja el comando take para quitar un tag a un jugador
     */
    private void handleTakeTag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags take <jugador> <tag>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags take Brocolitx example"));
            return;
        }

        String playerName = args[1];
        String tagName = args[2];

        // Obtener el jugador (puede estar offline)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(colorize(PREFIX + "&cEl jugador '&f" + playerName + "&c' nunca ha jugado en el servidor."));
            return;
        }

        // Quitar el tag al jugador
        if (PlayerDataManager.removeUnlockedTag(offlinePlayer.getUniqueId(), tagName)) {
            sender.sendMessage(colorize(PREFIX + "&a¡Tag '&f" + tagName + "&a' quitado exitosamente de &f" + playerName + "&a!"));

            // Si el jugador tenía este tag activo, quitárselo
            String currentTag = PlayerDataManager.getPlayerTag(offlinePlayer.getUniqueId());
            if (TagManager.getTag(tagName) != null &&
                    TagManager.getTag(tagName).getDisplayTag().equals(currentTag)) {
                PlayerDataManager.setPlayerTag(offlinePlayer.getUniqueId(), null);

                // Notificar al jugador si está online
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    onlinePlayer.sendMessage(colorize(PREFIX + "&cTu tag '&f" + tagName + "&c' ha sido removido."));
                }
            }

            plugin.getLogger().info("Tag '" + tagName + "' quitado de " + playerName + " por " + sender.getName());
        } else {
            sender.sendMessage(colorize(PREFIX + "&cError al quitar el tag. El jugador puede que no lo tenga."));
        }
    }

    /**
     * Maneja el comando set para establecer el tag activo de un jugador
     */
    private void handleSetTag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags set <jugador> <tag|none>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags set Brocolitx example"));
            sender.sendMessage(colorize(PREFIX + "&7Para quitar tag: &f/grvtags set Brocolitx none"));
            return;
        }

        String playerName = args[1];
        String tagName = args[2];

        // Obtener el jugador (puede estar offline)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(colorize(PREFIX + "&cEl jugador '&f" + playerName + "&c' nunca ha jugado en el servidor."));
            return;
        }

        // Si el tag es "none", quitar el tag activo
        if (tagName.equalsIgnoreCase("none")) {
            if (PlayerDataManager.setPlayerTag(offlinePlayer.getUniqueId(), null)) {
                sender.sendMessage(colorize(PREFIX + "&a¡Tag removido de &f" + playerName + "&a! Ahora usa el tag default."));

                // Notificar al jugador si está online
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    onlinePlayer.sendMessage(colorize(PREFIX + "&7Tu tag ha sido removido. Ahora usas el tag por defecto."));
                }
            } else {
                sender.sendMessage(colorize(PREFIX + "&cError al remover el tag."));
            }
            return;
        }

        // Verificar que el tag existe
        if (!TagManager.tagExists(tagName)) {
            sender.sendMessage(colorize(PREFIX + "&cEl tag '&f" + tagName + "&c' no existe."));
            return;
        }

        // Verificar que el jugador tiene el tag desbloqueado
        if (!PlayerDataManager.hasPlayerUnlockedTag(offlinePlayer.getUniqueId(), tagName)) {
            sender.sendMessage(colorize(PREFIX + "&cEl jugador '&f" + playerName + "&c' no tiene el tag '&f" + tagName + "&c' desbloqueado."));
            sender.sendMessage(colorize(PREFIX + "&7Usa &f/grvtags give " + playerName + " " + tagName + " &7para dárselo primero."));
            return;
        }

        // Establecer el tag activo
        if (PlayerDataManager.setPlayerTag(offlinePlayer.getUniqueId(), tagName)) {
            sender.sendMessage(colorize(PREFIX + "&a¡Tag '&f" + tagName + "&a' establecido para &f" + playerName + "&a!"));

            // Notificar al jugador si está online
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(colorize(PREFIX + "&a¡Tu tag ha sido cambiado a '&f" + tagName + "&a'!"));
            }

            plugin.getLogger().info("Tag '" + tagName + "' establecido para " + playerName + " por " + sender.getName());
        } else {
            sender.sendMessage(colorize(PREFIX + "&cError al establecer el tag."));
        }
    }

    /**
     * Maneja el comando check para ver información de un jugador
     */
    private void handleCheckPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize(PREFIX + "&cUso incorrecto. Sintaxis:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags check <jugador>"));
            sender.sendMessage(colorize(PREFIX + "&7Ejemplo: &f/grvtags check Brocolitx"));
            return;
        }

        String playerName = args[1];

        // Obtener el jugador (puede estar offline)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(colorize(PREFIX + "&cEl jugador '&f" + playerName + "&c' nunca ha jugado en el servidor."));
            return;
        }

        // Mostrar información del jugador
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&6&l         Información de " + playerName));
        sender.sendMessage(colorize("&8&m----------------------------------------"));

        // Tag actual
        String currentTag = PlayerDataManager.getPlayerTag(offlinePlayer.getUniqueId());
        sender.sendMessage(colorize("&7Tag actual: " + currentTag));

        // Total de tags desbloqueados
        int totalTags = PlayerDataManager.getPlayerTagCount(offlinePlayer.getUniqueId());
        sender.sendMessage(colorize("&7Tags desbloqueados: &f" + totalTags));

        // Tags por categoría
        List<String> categoryNames = CategoryManager.getAllCategoryNames();
        for (String categoryName : categoryNames) {
            int categoryTagCount = PlayerDataManager.getPlayerTagCountByCategory(offlinePlayer.getUniqueId(), categoryName);
            if (categoryTagCount > 0) {
                sender.sendMessage(colorize("&7- " + categoryName + ": &f" + categoryTagCount + " tags"));
            }
        }

        // Estado de conexión
        sender.sendMessage(colorize("&7Estado: " + (offlinePlayer.isOnline() ? "&aConectado" : "&cDesconectado")));

        sender.sendMessage(colorize("&8&m----------------------------------------"));
    }

    // [Resto de métodos sin cambios...]

    /**
     * Autocompletado para el comando create
     * /grvtags create <nombre> <categoria>
     */
    private List<String> handleCreateTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Segundo argumento: nombre del tag (sugerencias)
            List<String> tagSuggestions = Arrays.asList(
                    "Example", "Grivyzom"
            );

            String partial = args[1].toLowerCase();
            for (String suggestion : tagSuggestions) {
                if (suggestion.toLowerCase().startsWith(partial)) {
                    completions.add(suggestion);
                }
            }
        } else if (args.length == 3) {
            // Tercer argumento: categorías existentes
            List<String> categoryNames = CategoryManager.getAllCategoryNames();
            String partial = args[2].toLowerCase();

            for (String categoryName : categoryNames) {
                if (categoryName.toLowerCase().startsWith(partial)) {
                    completions.add(categoryName);
                }
            }
        }

        return completions;
    }

    /**
     * Autocompletado para el comando createcategory
     * /grvtags createcategory <nombre>
     */
    private List<String> handleCreateCategoryTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Segundo argumento: nombre de la categoría (sugerencias comunes)
            List<String> categorySuggestions = Arrays.asList(
                    "premium", "ranks", "staff", "donators", "seasonal",
                    "special", "event", "custom", "limited", "exclusive",
                    "halloween", "christmas", "easter", "anniversary",
                    "admin", "moderator", "helper", "builder"
            );

            String partial = args[1].toLowerCase();
            for (String suggestion : categorySuggestions) {
                if (suggestion.startsWith(partial)) {
                    completions.add(suggestion);
                }
            }
        }

        return completions;
    }

    /**
     * Autocompletado para el comando editor
     * /grvtags editor <category|tag|tags>
     */
    private List<String> handleEditorTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Segundo argumento: tipos de editor
            List<String> editorTypes = Arrays.asList("category", "tag", "tags");
            String partial = args[1].toLowerCase();

            for (String editorType : editorTypes) {
                if (editorType.startsWith(partial)) {
                    completions.add(editorType);
                }
            }
        } else if (args.length == 3) {
            String editorType = args[1].toLowerCase();

            if ("tag".equals(editorType)) {
                // Para editor de tag específico: mostrar tags existentes
                List<String> tagNames = TagManager.getAllTagNames();
                String partial = args[2].toLowerCase();

                for (String tagName : tagNames) {
                    if (tagName.toLowerCase().startsWith(partial)) {
                        completions.add(tagName);
                    }
                }
            } else if ("category".equals(editorType)) {
                // Para editor de categoría específica: mostrar categorías existentes
                List<String> categoryNames = CategoryManager.getAllCategoryNames();
                String partial = args[2].toLowerCase();

                for (String categoryName : categoryNames) {
                    if (categoryName.toLowerCase().startsWith(partial)) {
                        completions.add(categoryName);
                    }
                }
            }
        }

        return completions;
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
                return;
            }

            // Mostrar estadísticas antes del reload
            int categoriesBeforeReload = CategoryManager.getCategoryCount();
            int tagsBeforeReload = TagManager.getTagCount();

            // MEJORADO: Recargar desde archivos YAML primero, luego desde BD
            sender.sendMessage(colorize(PREFIX + "&7Sincronizando categories.yml..."));
            CategoryManager.reloadCategoriesFromYaml();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7categories.yml sincronizado con base de datos"));

            sender.sendMessage(colorize(PREFIX + "&7Sincronizando tags.yml..."));
            TagManager.reloadTagsFromYaml();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7tags.yml sincronizado con base de datos"));

            // Recargar tag por defecto
            PlayerDataManager.reloadDefaultTag();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Tag por defecto recargado"));

            // Mostrar estadísticas después del reload
            int categoriesAfterReload = CategoryManager.getCategoryCount();
            int tagsAfterReload = TagManager.getTagCount();

            long endTime = System.currentTimeMillis();
            long reloadTime = endTime - startTime;

            // Resumen final mejorado
            sender.sendMessage(colorize("&8&m----------------------------------------"));
            sender.sendMessage(colorize(PREFIX + "&a¡Recarga completada exitosamente!"));
            sender.sendMessage(colorize("&8&m----------------------------------------"));

            // Información detallada
            sender.sendMessage(colorize("&6&lESTADÍSTICAS DE RECARGA:"));
            sender.sendMessage(colorize("&7⏱ Tiempo de recarga: &f" + reloadTime + "ms"));
            sender.sendMessage(colorize(""));

            // Categorías
            sender.sendMessage(colorize("&7📁 Categorías:"));
            sender.sendMessage(colorize("&7  Antes: &f" + categoriesBeforeReload + " &7→ Después: &f" + categoriesAfterReload));

            int categoryDifference = categoriesAfterReload - categoriesBeforeReload;
            if (categoryDifference > 0) {
                sender.sendMessage(colorize("&7  &a+" + categoryDifference + " &7nuevas categorías añadidas"));
            } else if (categoryDifference < 0) {
                sender.sendMessage(colorize("&7  &c" + Math.abs(categoryDifference) + " &7categorías eliminadas"));
            } else {
                sender.sendMessage(colorize("&7  &eSin cambios en categorías"));
            }

            sender.sendMessage(colorize(""));

            // Tags
            sender.sendMessage(colorize("&7🏷 Tags:"));
            sender.sendMessage(colorize("&7  Antes: &f" + tagsBeforeReload + " &7→ Después: &f" + tagsAfterReload));

            int tagDifference = tagsAfterReload - tagsBeforeReload;
            if (tagDifference > 0) {
                sender.sendMessage(colorize("&7  &a+" + tagDifference + " &7nuevos tags añadidos"));
            } else if (tagDifference < 0) {
                sender.sendMessage(colorize("&7  &c" + Math.abs(tagDifference) + " &7tags eliminados"));
            } else {
                sender.sendMessage(colorize("&7  &eSin cambios en tags"));
            }

            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize("&7🔄 Sistema: &aOperativo y sincronizado"));
            sender.sendMessage(colorize("&7💾 Base de datos: &aActualizada"));
            sender.sendMessage(colorize("&8&m----------------------------------------"));

            plugin.getLogger().info("Configuraciones recargadas por " + sender.getName() + " (" + reloadTime + "ms)");
            plugin.getLogger().info("Resultado: " + categoriesAfterReload + " categorías, " + tagsAfterReload + " tags");

        } catch (Exception e) {
            sender.sendMessage(colorize(PREFIX + "&c¡Error durante la recarga!"));
            sender.sendMessage(colorize(PREFIX + "&cError: &f" + e.getMessage()));
            plugin.getLogger().severe("Error durante la recarga ejecutada por " + sender.getName() + ": " + e.getMessage());
            e.printStackTrace();
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
        sender.sendMessage(colorize("&7Tag default: " + PlayerDataManager.getDefaultTag()));

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
            case "cleanup":
                handleCleanup(sender, args);
                break;
            default:
                sender.sendMessage(colorize(PREFIX + "&cTipo de editor desconocido: &f" + editorType));
                sender.sendMessage(colorize(PREFIX + "&7Tipos disponibles: &fcategory&7, &ftag&7, &ftags"));
                break;
        }
    }

    private void cleanupCategoriesNotInYaml() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Eliminar todas las categorías que no están marcadas como from_yaml
            String deleteQuery = """
            DELETE FROM grvtags_categories 
            WHERE id NOT IN (
                SELECT id FROM (
                    SELECT id FROM grvtags_categories 
                    WHERE is_from_yaml = TRUE OR is_from_yaml IS NULL
                ) AS temp
            )
        """;

            PreparedStatement stmt = conn.prepareStatement(deleteQuery);
            int deletedCount = stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("Limpieza: " + deletedCount + " categorías eliminadas de la BD");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al limpiar categorías:", e);
        }
    }


    private void handleCleanup(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(colorize(PREFIX + "&c&l⚠ COMANDO DE LIMPIEZA TOTAL"));
            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize(PREFIX + "&7Este comando eliminará TODAS las categorías y tags"));
            sender.sendMessage(colorize(PREFIX + "&7de la base de datos que NO estén en los archivos YAML."));
            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize(PREFIX + "&c&lADVERTENCIA:"));
            sender.sendMessage(colorize(PREFIX + "&7- Se eliminará cualquier categoría/tag no presente en YAML"));
            sender.sendMessage(colorize(PREFIX + "&7- Los datos de jugadores se mantendrán seguros"));
            sender.sendMessage(colorize(PREFIX + "&7- Esta acción NO se puede deshacer"));
            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize(PREFIX + "&7Para confirmar, ejecuta:"));
            sender.sendMessage(colorize(PREFIX + "&f/grvtags cleanup confirm"));
            return;
        }

        sender.sendMessage(colorize(PREFIX + "&7Iniciando limpieza completa de la base de datos..."));

        try {
            long startTime = System.currentTimeMillis();

            // Estadísticas antes de la limpieza
            int categoriesBeforeCleanup = CategoryManager.getCategoryCount();
            int tagsBeforeCleanup = TagManager.getTagCount();

            sender.sendMessage(colorize(PREFIX + "&7Estado actual:"));
            sender.sendMessage(colorize(PREFIX + "&7- Categorías en BD: &f" + categoriesBeforeCleanup));
            sender.sendMessage(colorize(PREFIX + "&7- Tags en BD: &f" + tagsBeforeCleanup));
            sender.sendMessage(colorize(""));

            // Paso 1: Limpiar categorías que no están en el YAML
            sender.sendMessage(colorize(PREFIX + "&71. Limpiando categorías..."));
            cleanupCategoriesNotInYaml();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Categorías limpiadas"));

            // Paso 2: Limpiar tags que no están en el YAML
            sender.sendMessage(colorize(PREFIX + "&72. Limpiando tags..."));
            cleanupTagsNotInYaml();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Tags limpiados"));

            // Paso 3: Forzar recarga completa desde YAML
            sender.sendMessage(colorize(PREFIX + "&73. Recargando desde YAML..."));
            CategoryManager.reloadCategoriesFromYaml();
            TagManager.reloadTagsFromYaml();
            PlayerDataManager.reloadDefaultTag();
            sender.sendMessage(colorize(PREFIX + "&a✓ &7Recarga desde YAML completada"));

            // Estadísticas después de la limpieza
            int categoriesAfterCleanup = CategoryManager.getCategoryCount();
            int tagsAfterCleanup = TagManager.getTagCount();

            long endTime = System.currentTimeMillis();
            long cleanupTime = endTime - startTime;

            // Resumen final
            sender.sendMessage(colorize("&8&m----------------------------------------"));
            sender.sendMessage(colorize(PREFIX + "&a&l¡LIMPIEZA COMPLETADA!"));
            sender.sendMessage(colorize("&8&m----------------------------------------"));
            sender.sendMessage(colorize("&7⏱ Tiempo: &f" + cleanupTime + "ms"));
            sender.sendMessage(colorize(""));

            sender.sendMessage(colorize("&7📁 Categorías:"));
            sender.sendMessage(colorize("&7  Antes: &f" + categoriesBeforeCleanup + " &7→ Después: &f" + categoriesAfterCleanup));

            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize("&7🏷 Tags:"));
            sender.sendMessage(colorize("&7  Antes: &f" + tagsBeforeCleanup + " &7→ Después: &f" + tagsAfterCleanup));

            int categoriesRemoved = categoriesBeforeCleanup - categoriesAfterCleanup;
            int tagsRemoved = tagsBeforeCleanup - tagsAfterCleanup;

            sender.sendMessage(colorize(""));
            if (categoriesRemoved > 0 || tagsRemoved > 0) {
                sender.sendMessage(colorize("&c📊 Elementos eliminados:"));
                if (categoriesRemoved > 0) {
                    sender.sendMessage(colorize("&7  - &c" + categoriesRemoved + " &7categorías eliminadas"));
                }
                if (tagsRemoved > 0) {
                    sender.sendMessage(colorize("&7  - &c" + tagsRemoved + " &7tags eliminados"));
                }
            } else {
                sender.sendMessage(colorize("&a✅ No se encontraron elementos para eliminar"));
            }

            sender.sendMessage(colorize(""));
            sender.sendMessage(colorize("&7🔄 La base de datos ahora coincide exactamente con"));
            sender.sendMessage(colorize("&7   los archivos categories.yml y tags.yml"));
            sender.sendMessage(colorize("&8&m----------------------------------------"));

            plugin.getLogger().info("Limpieza completa ejecutada por " + sender.getName() + " (" + cleanupTime + "ms)");
            plugin.getLogger().info("Elementos eliminados: " + categoriesRemoved + " categorías, " + tagsRemoved + " tags");

        } catch (Exception e) {
            sender.sendMessage(colorize(PREFIX + "&c¡Error durante la limpieza!"));
            sender.sendMessage(colorize(PREFIX + "&cError: &f" + e.getMessage()));
            plugin.getLogger().severe("Error durante la limpieza ejecutada por " + sender.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&6&l            grvTags Admin"));
        sender.sendMessage(colorize("&8&m----------------------------------------"));
        sender.sendMessage(colorize("&f/grvTags reload &8- &7Recarga las configuraciones"));
        sender.sendMessage(colorize("&f/grvTags info &8- &7Información del plugin"));
        sender.sendMessage(colorize("&f/grvTags database &8- &7Estado de la base de datos"));
        sender.sendMessage(colorize("&f/grvTags create <nombre> <categoria> &8- &7Crear un tag"));
        sender.sendMessage(colorize("&f/grvTags createcategory <nombre> &8- &7Crear una categoría"));
        sender.sendMessage(colorize("&f/grvTags editor <type> &8- &7Abrir editores GUI"));
        sender.sendMessage(colorize("&f/grvTags cleanup confirm &8- &7Limpiar BD completamente"));
        sender.sendMessage(colorize(""));
        sender.sendMessage(colorize("&6&lComandos de Jugadores:"));
        sender.sendMessage(colorize("&f/grvTags give <jugador> <tag> &8- &7Dar un tag a un jugador"));
        sender.sendMessage(colorize("&f/grvTags take <jugador> <tag> &8- &7Quitar un tag de un jugador"));
        sender.sendMessage(colorize("&f/grvTags set <jugador> <tag|none> &8- &7Establecer tag activo"));
        sender.sendMessage(colorize("&f/grvTags check <jugador> &8- &7Ver información de un jugador"));
        sender.sendMessage(colorize("&f/grvTags help &8- &7Muestra esta ayuda"));
        sender.sendMessage(colorize("&8&m----------------------------------------"));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Métodos auxiliares para el editor (a implementar)
    private void openCategoryEditor(Player player) {
        player.sendMessage(colorize(PREFIX + "&e⚠ &7Editor de categorías en desarrollo"));
    }

    private void openTagEditor(Player player) {
        player.sendMessage(colorize(PREFIX + "&e⚠ &7Editor de tags en desarrollo"));
    }

    private void openTagsOverview(Player player) {
        player.sendMessage(colorize(PREFIX + "&e⚠ &7Vista general de tags en desarrollo"));
    }

    // Métodos de validación mejorados
    private boolean isValidTagName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Longitud entre 2 y 20 caracteres
        if (name.length() < 2 || name.length() > 20) {
            return false;
        }

        // Solo letras, números y guiones bajos
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    private void cleanupTagsNotInYaml() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Primero, resetear tags activos de jugadores que van a ser eliminados
            String resetPlayerTagsQuery = """
            UPDATE grvtags_player_data 
            SET current_tag = NULL 
            WHERE current_tag IN (
                SELECT name FROM grvtags_tags 
                WHERE (is_from_yaml = FALSE OR is_from_yaml IS NULL) 
                AND name != 'default'
            )
        """;

            PreparedStatement resetStmt = conn.prepareStatement(resetPlayerTagsQuery);
            int resetCount = resetStmt.executeUpdate();
            resetStmt.close();

            if (resetCount > 0) {
                plugin.getLogger().info("Limpieza: " + resetCount + " jugadores tuvieron sus tags reseteados al default");
            }

            // Eliminar tags desbloqueados de jugadores para tags que van a ser eliminados
            String deleteUnlockedQuery = """
            DELETE FROM grvtags_unlocked_tags 
            WHERE tag_name IN (
                SELECT name FROM grvtags_tags 
                WHERE (is_from_yaml = FALSE OR is_from_yaml IS NULL) 
                AND name != 'default'
            )
        """;

            PreparedStatement deleteUnlockedStmt = conn.prepareStatement(deleteUnlockedQuery);
            int unlockedCount = deleteUnlockedStmt.executeUpdate();
            deleteUnlockedStmt.close();

            if (unlockedCount > 0) {
                plugin.getLogger().info("Limpieza: " + unlockedCount + " tags desbloqueados eliminados");
            }

            // Finalmente, eliminar los tags que no están en YAML
            String deleteTagsQuery = """
            DELETE FROM grvtags_tags 
            WHERE (is_from_yaml = FALSE OR is_from_yaml IS NULL) 
            AND name != 'default'
        """;

            PreparedStatement deleteTagsStmt = conn.prepareStatement(deleteTagsQuery);
            int deletedCount = deleteTagsStmt.executeUpdate();
            deleteTagsStmt.close();

            plugin.getLogger().info("Limpieza: " + deletedCount + " tags eliminados de la BD");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al limpiar tags:", e);
        }
    }

    private boolean isValidCategoryName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Longitud entre 2 y 15 caracteres
        if (name.length() < 2 || name.length() > 15) {
            return false;
        }

        // Solo letras, números y guiones bajos
        return name.matches("^[a-zA-Z0-9_]+$");
    }
}