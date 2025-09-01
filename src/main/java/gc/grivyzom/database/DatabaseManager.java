package gc.grivyzom.database;

import gc.grivyzom.grvTags;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private static grvTags plugin;
    private static Connection connection;

    // Datos de conexión
    private static String host;
    private static int port;
    private static String database;
    private static String username;
    private static String password;
    private static boolean ssl;

    /**
     * Inicializa el gestor de base de datos
     * @param pluginInstance Instancia del plugin principal
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        loadDatabaseConfig();
        connect();

        // NUEVO: Ejecutar migraciones después de conectar
        if (isConnected()) {
            DatabaseMigrations.runMigrations(plugin);
        }
    }

    /**
     * Carga la configuración de la base de datos desde config.yml
     */
    private static void loadDatabaseConfig() {
        FileConfiguration config = plugin.getConfig();

        host = config.getString("database.host", "localhost");
        port = config.getInt("database.port", 3306);
        database = config.getString("database.database", "survivalcore");
        username = config.getString("database.user", "root");
        password = config.getString("database.password", "");
        ssl = config.getBoolean("database.ssl", false);

        plugin.getLogger().info("Configuración de base de datos cargada:");
        plugin.getLogger().info("- Host: " + host + ":" + port);
        plugin.getLogger().info("- Base de datos: " + database);
        plugin.getLogger().info("- Usuario: " + username);
        plugin.getLogger().info("- SSL: " + ssl);
    }

    /**
     * Establece la conexión con la base de datos
     */
    public static void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("Ya existe una conexión activa a la base de datos");
                return;
            }

            // Construir URL de conexión
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl
                    + "&autoReconnect=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8";

            plugin.getLogger().info("Intentando conectar a la base de datos...");
            plugin.getLogger().info("URL: " + url.replaceAll("password=[^&]*", "password=***"));

            // Establecer conexión
            connection = DriverManager.getConnection(url, username, password);

            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("¡Conexión a la base de datos establecida exitosamente!");
            } else {
                plugin.getLogger().severe("Error: La conexión no se pudo establecer");
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar con la base de datos:", e);
            plugin.getLogger().severe("Detalles del error:");
            plugin.getLogger().severe("- Código de error: " + e.getErrorCode());
            plugin.getLogger().severe("- Estado SQL: " + e.getSQLState());
            plugin.getLogger().severe("- Mensaje: " + e.getMessage());
        }
    }

    /**
     * Obtiene la conexión actual
     * @return Conexión a la base de datos
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Conexión perdida, reintentando...");
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al verificar el estado de la conexión:", e);
        }
        return connection;
    }

    /**
     * Verifica si la conexión está activa
     * @return true si la conexión está activa
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al verificar la conexión:", e);
            return false;
        }
    }

    /**
     * Cierra la conexión con la base de datos
     */
    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexión a la base de datos cerrada correctamente");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cerrar la conexión:", e);
        } finally {
            connection = null;
        }
    }

    /**
     * Prueba la conexión ejecutando una consulta simple
     * @return true si la prueba es exitosa
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                // Ejecutar una consulta simple para probar la conexión
                conn.createStatement().execute("SELECT 1");
                plugin.getLogger().info("Prueba de conexión exitosa");
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error en la prueba de conexión:", e);
        }
        return false;
    }
}