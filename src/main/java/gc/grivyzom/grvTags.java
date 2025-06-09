package gc.grivyzom;

import gc.grivyzom.commands.AdminCommand;
import gc.grivyzom.commands.CategoryCommand;
import gc.grivyzom.commands.TagCommand;
import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.listeners.PlayerListener;
import gc.grivyzom.managers.CategoryManager;
import gc.grivyzom.managers.PlayerDataManager;
import gc.grivyzom.managers.TagManager;
import gc.grivyzom.placeholders.GrvTagsPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class grvTags extends JavaPlugin {

    private static final String PREFIX = "&8[&6grvTags&8] ";
    private static final String PLUGIN_NAME = "grvTags";
    private static final String VERSION = "1.0";
    private static final String AUTHOR = "Brocolitx";

    private GrvTagsPlaceholders placeholderHook;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // Banner de inicio
        sendColoredMessage("&8&m----------------------------------------");
        sendColoredMessage("&6  _____ _______      _______ ");
        sendColoredMessage("&6 / ____|  __ \\ \\    / /_   _|");
        sendColoredMessage("&6| |  __| |__) \\ \\  / /  | |  &7 Tags");
        sendColoredMessage("&6| | |_ |  _  / \\ \\/ /   | |  ");
        sendColoredMessage("&6| |__| | | \\ \\  \\  /   _| |_ ");
        sendColoredMessage("&6 \\_____|_|  \\_\\  \\/   |_____|");
        sendColoredMessage("");
        sendColoredMessage("&7Plugin: &f" + PLUGIN_NAME);
        sendColoredMessage("&7Versión: &f" + VERSION);
        sendColoredMessage("&7Autor: &f" + AUTHOR);
        sendColoredMessage("&8&m----------------------------------------");

        try {
            // Inicialización de componentes
            sendColoredMessage(PREFIX + "&7Iniciando componentes...");

            // Cargar configuraciones
            loadConfigurations();
            sendColoredMessage(PREFIX + "&a✓ &7Configuraciones cargadas");

            // Inicializar base de datos
            initializeDatabase();
            sendColoredMessage(PREFIX + "&a✓ &7Base de datos inicializada");

            // Inicializar managers
            initializeManagers();
            sendColoredMessage(PREFIX + "&a✓ &7Managers inicializados");

            // Registrar comandos
            registerCommands();
            sendColoredMessage(PREFIX + "&a✓ &7Comandos registrados");

            // Registrar eventos
            registerEvents();
            sendColoredMessage(PREFIX + "&a✓ &7Eventos registrados");

            // Verificar dependencias
            checkDependencies();
            sendColoredMessage(PREFIX + "&a✓ &7Dependencias verificadas");

            // Inicializar PlaceholderAPI hook
            initializePlaceholders();

            long endTime = System.currentTimeMillis();
            long loadTime = endTime - startTime;

            sendColoredMessage("&8&m----------------------------------------");
            sendColoredMessage(PREFIX + "&a¡Plugin habilitado correctamente!");
            sendColoredMessage(PREFIX + "&7Tiempo de carga: &f" + loadTime + "ms");
            sendColoredMessage("&8&m----------------------------------------");

        } catch (Exception e) {
            sendColoredMessage(PREFIX + "&c¡Error durante la inicialización!");
            sendColoredMessage(PREFIX + "&cError: &f" + e.getMessage());
            sendColoredMessage(PREFIX + "&cDeshabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        sendColoredMessage("&8&m----------------------------------------");
        sendColoredMessage(PREFIX + "&eDeshabilitando plugin...");

        try {
            // Desregistrar PlaceholderAPI hook
            if (placeholderHook != null) {
                placeholderHook.unregister();
                sendColoredMessage(PREFIX + "&a✓ &7PlaceholderAPI hook desregistrado");
            }

            // Guardar datos pendientes
            saveData();
            sendColoredMessage(PREFIX + "&a✓ &7Datos guardados");

            // Cerrar conexiones de base de datos
            closeDatabase();
            sendColoredMessage(PREFIX + "&a✓ &7Conexiones cerradas");

            // Limpiar tareas asíncronas
            cleanupTasks();
            sendColoredMessage(PREFIX + "&a✓ &7Tareas limpiadas");

        } catch (Exception e) {
            sendColoredMessage(PREFIX + "&cError durante el apagado: &f" + e.getMessage());
        }

        sendColoredMessage(PREFIX + "&c¡Plugin deshabilitado!");
        sendColoredMessage("&7¡Gracias por usar &6" + PLUGIN_NAME + "&7!");
        sendColoredMessage("&8&m----------------------------------------");
    }

    /**
     * Carga todas las configuraciones del plugin
     */
    private void loadConfigurations() {
        // Guardar configuraciones por defecto si no existen
        saveDefaultConfig();

        // Cargar config.yml
        reloadConfig();

        // Guardar tags.yml y categories.yml si no existen
        saveResource("tags.yml", false);
        saveResource("categories.yml", false);

        getLogger().info("Configuraciones cargadas correctamente");
    }

    /**
     * Inicializa la conexión a la base de datos
     */
    private void initializeDatabase() {
        try {
            DatabaseManager.initialize(this);

            // Probar la conexión
            if (DatabaseManager.testConnection()) {
                getLogger().info("Conexión a la base de datos verificada correctamente");
            } else {
                getLogger().severe("Error: No se pudo verificar la conexión a la base de datos");
            }

        } catch (Exception e) {
            getLogger().severe("Error al inicializar la base de datos: " + e.getMessage());
            throw e; // Re-lanzar la excepción para que se maneje en onEnable
        }
    }

    /**
     * Inicializa los managers del plugin
     */
    private void initializeManagers() {
        try {
            // Inicializar CategoryManager
            CategoryManager.initialize(this);
            getLogger().info("CategoryManager inicializado correctamente");

            // Inicializar TagManager (con carga desde YAML)
            TagManager.initialize(this);
            getLogger().info("TagManager inicializado correctamente");

            // Inicializar PlayerDataManager
            PlayerDataManager.initialize(this);
            getLogger().info("PlayerDataManager inicializado correctamente");

        } catch (Exception e) {
            getLogger().severe("Error al inicializar los managers: " + e.getMessage());
            throw e; // Re-lanzar la excepción para que se maneje en onEnable
        }
    }

    /**
     * Registra los comandos del plugin
     */
    private void registerCommands() {
        // Registrar comando /tags
        TagCommand tagCommand = new TagCommand(this);
        getCommand("tag").setExecutor(tagCommand);

        // Registrar comando /categories
        CategoryCommand categoryCommand = new CategoryCommand(this);
        getCommand("category").setExecutor(categoryCommand);

        // Registrar comando admin /grvTags
        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("grvTags").setExecutor(adminCommand);
        getCommand("grvTags").setTabCompleter(adminCommand);

        getLogger().info("Comandos registrados correctamente");
        getLogger().info("- /tags - Comando para abrir GUI de tags (Default)");
        getLogger().info("- /categories - Comando para abrir GUI de categorías (OP)");
        getLogger().info("- /grvTags - Comando de administración (OP)");
    }

    /**
     * Registra todos los eventos del plugin
     */
    private void registerEvents() {
        // Registrar PlayerListener
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getLogger().info("Eventos registrados correctamente");
    }

    /**
     * Verifica las dependencias del plugin
     */
    private void checkDependencies() {
        // Verificar PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            sendColoredMessage(PREFIX + "&a✓ &7PlaceholderAPI encontrado");
        } else {
            sendColoredMessage(PREFIX + "&e⚠ &7PlaceholderAPI no encontrado (opcional)");
        }

        // Verificar Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            sendColoredMessage(PREFIX + "&a✓ &7Vault encontrado");
        } else {
            sendColoredMessage(PREFIX + "&e⚠ &7Vault no encontrado (opcional para economía)");
        }
    }

    /**
     * Inicializa los placeholders de PlaceholderAPI
     */
    private void initializePlaceholders() {
        // Verificar si PlaceholderAPI está disponible
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderHook = new GrvTagsPlaceholders(this);
                placeholderHook.register();
                sendColoredMessage(PREFIX + "&a✓ &7PlaceholderAPI hook inicializado");
            } catch (Exception e) {
                sendColoredMessage(PREFIX + "&c✗ &7Error al inicializar PlaceholderAPI hook: " + e.getMessage());
                getLogger().warning("Error al inicializar PlaceholderAPI: " + e.getMessage());
            }
        } else {
            sendColoredMessage(PREFIX + "&7PlaceholderAPI no disponible - placeholders deshabilitados");
        }
    }

    /**
     * Guarda datos pendientes antes del apagado
     */
    private void saveData() {
        // Placeholder para guardar datos adicionales si es necesario
        getLogger().info("Datos guardados correctamente");
    }

    /**
     * Cierra las conexiones de base de datos
     */
    private void closeDatabase() {
        DatabaseManager.disconnect();
        getLogger().info("Conexiones de base de datos cerradas");
    }

    /**
     * Limpia las tareas asíncronas
     */
    private void cleanupTasks() {
        // Cancelar todas las tareas del plugin
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("Tareas asíncronas limpiadas");
    }

    /**
     * Envía un mensaje con colores a la consola
     * @param message Mensaje con códigos de color
     */
    private void sendColoredMessage(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Obtiene la instancia del plugin (útil para otras clases)
     * @return Instancia del plugin
     */
    public static grvTags getInstance() {
        return getPlugin(grvTags.class);
    }
}