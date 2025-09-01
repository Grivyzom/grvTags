package gc.grivyzom.database;

import gc.grivyzom.grvTags;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Clase para manejar migraciones de base de datos
 * Se ejecuta automáticamente cuando se detectan cambios en la estructura
 */
public class DatabaseMigrations {

    private static grvTags plugin;

    public static void runMigrations(grvTags pluginInstance) {
        plugin = pluginInstance;

        plugin.getLogger().info("Verificando migraciones de base de datos...");

        // Ejecutar migraciones en orden
        addYamlColumnToCategories();
        addYamlColumnToTags();

        plugin.getLogger().info("Migraciones de base de datos completadas");
    }

    /**
     * Añade la columna is_from_yaml a la tabla de categorías si no existe
     */
    private static void addYamlColumnToCategories() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Verificar si la columna ya existe
            if (columnExists(conn, "grvtags_categories", "is_from_yaml")) {
                plugin.getLogger().fine("Columna is_from_yaml ya existe en grvtags_categories");
                return;
            }

            // Añadir la columna
            String alterQuery = "ALTER TABLE grvtags_categories ADD COLUMN is_from_yaml BOOLEAN DEFAULT TRUE";
            PreparedStatement stmt = conn.prepareStatement(alterQuery);
            stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("✓ Añadida columna is_from_yaml a tabla grvtags_categories");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al añadir columna is_from_yaml a categorías:", e);
        }
    }

    /**
     * Añade la columna is_from_yaml a la tabla de tags si no existe
     */
    private static void addYamlColumnToTags() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Verificar si la columna ya existe
            if (columnExists(conn, "grvtags_tags", "is_from_yaml")) {
                plugin.getLogger().fine("Columna is_from_yaml ya existe en grvtags_tags");
                return;
            }

            // Añadir la columna
            String alterQuery = "ALTER TABLE grvtags_tags ADD COLUMN is_from_yaml BOOLEAN DEFAULT TRUE";
            PreparedStatement stmt = conn.prepareStatement(alterQuery);
            stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("✓ Añadida columna is_from_yaml a tabla grvtags_tags");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al añadir columna is_from_yaml a tags:", e);
        }
    }

    /**
     * Verifica si una columna existe en una tabla
     */
    private static boolean columnExists(Connection conn, String tableName, String columnName) {
        try {
            String query = "SHOW COLUMNS FROM " + tableName + " LIKE ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, columnName);

            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();

            rs.close();
            stmt.close();

            return exists;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al verificar si existe la columna " + columnName + " en " + tableName + ":", e);
            return false;
        }
    }
}