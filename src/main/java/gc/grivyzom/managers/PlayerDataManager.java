package gc.grivyzom.managers;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.models.Tag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private static grvTags plugin;
    private static String defaultTag = "&8[&7?&8]";

    /**
     * Inicializa el PlayerDataManager
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        createTablesIfNotExist();
        loadDefaultTag();
    }

    /**
     * Crea las tablas necesarias si no existen
     */
    private static void createTablesIfNotExist() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("No se pudo obtener conexión para crear tablas de datos de jugadores");
                return;
            }

            // Crear tabla de datos de jugadores
            String createPlayerDataTable = """
                CREATE TABLE IF NOT EXISTS grvtags_player_data (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL UNIQUE,
                    username VARCHAR(16) NOT NULL,
                    current_tag VARCHAR(20),
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // Crear tabla de tags desbloqueados
            String createUnlockedTagsTable = """
                CREATE TABLE IF NOT EXISTS grvtags_unlocked_tags (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    tag_name VARCHAR(20) NOT NULL,
                    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_player_tag (player_uuid, tag_name),
                    FOREIGN KEY (player_uuid) REFERENCES grvtags_player_data(uuid) ON DELETE CASCADE
                )
            """;

            PreparedStatement stmt1 = conn.prepareStatement(createPlayerDataTable);
            stmt1.executeUpdate();
            stmt1.close();

            PreparedStatement stmt2 = conn.prepareStatement(createUnlockedTagsTable);
            stmt2.executeUpdate();
            stmt2.close();

            plugin.getLogger().info("Tablas de datos de jugadores verificadas/creadas correctamente");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tablas de datos de jugadores:", e);
        }
    }

    /**
     * Carga el tag default desde tags.yml
     */
    private static void loadDefaultTag() {
        try {
            File tagsFile = new File(plugin.getDataFolder(), "tags.yml");

            if (!tagsFile.exists()) {
                plugin.saveResource("tags.yml", false);
            }

            YamlConfiguration tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);

            if (tagsConfig.contains("tags.default.tag")) {
                defaultTag = tagsConfig.getString("tags.default.tag", "&8[&7?&8]");
                plugin.getLogger().info("Tag default cargado: " + defaultTag);
            } else {
                plugin.getLogger().warning("No se encontró tag default en tags.yml, usando valor por defecto");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar tag default:", e);
        }
    }

    /**
     * Obtiene o crea los datos de un jugador
     */
    public static void createPlayerDataIfNotExists(Player player) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            String query = """
                INSERT IGNORE INTO grvtags_player_data (uuid, username, current_tag) 
                VALUES (?, ?, NULL)
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());

            stmt.executeUpdate();
            stmt.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear datos de jugador para " + player.getName() + ":", e);
        }
    }

    /**
     * Actualiza el nombre de usuario del jugador
     */
    public static void updatePlayerName(Player player) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            String query = "UPDATE grvtags_player_data SET username = ? WHERE uuid = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, player.getName());
            stmt.setString(2, player.getUniqueId().toString());

            stmt.executeUpdate();
            stmt.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar nombre de jugador para " + player.getName() + ":", e);
        }
    }

    /**
     * Obtiene el tag actual del jugador
     */
    public static String getPlayerTag(UUID playerUUID) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return defaultTag;

            String query = "SELECT current_tag FROM grvtags_player_data WHERE uuid = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());

            ResultSet rs = stmt.executeQuery();

            String currentTag = null;
            if (rs.next()) {
                currentTag = rs.getString("current_tag");
            }

            rs.close();
            stmt.close();

            // Si no tiene tag asignado, devolver el default
            if (currentTag == null) {
                return defaultTag;
            }

            // Verificar que el tag existe y obtener su display_tag
            Tag tag = TagManager.getTag(currentTag);
            if (tag != null) {
                return tag.getDisplayTag();
            } else {
                return defaultTag;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al obtener tag del jugador " + playerUUID + ":", e);
            return defaultTag;
        }
    }

    /**
     * Obtiene el tag actual del jugador por nombre
     */
    public static String getPlayerTag(String playerName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return defaultTag;

            String query = "SELECT current_tag FROM grvtags_player_data WHERE username = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();

            String currentTag = null;
            if (rs.next()) {
                currentTag = rs.getString("current_tag");
            }

            rs.close();
            stmt.close();

            // Si no tiene tag asignado, devolver el default
            if (currentTag == null) {
                return defaultTag;
            }

            // Verificar que el tag existe y obtener su display_tag
            Tag tag = TagManager.getTag(currentTag);
            if (tag != null) {
                return tag.getDisplayTag();
            } else {
                return defaultTag;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al obtener tag del jugador " + playerName + ":", e);
            return defaultTag;
        }
    }

    /**
     * Establece el tag actual del jugador
     */
    public static boolean setPlayerTag(UUID playerUUID, String tagName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            // Verificar que el jugador tiene el tag desbloqueado
            if (tagName != null && !hasPlayerUnlockedTag(playerUUID, tagName)) {
                return false;
            }

            String query = "UPDATE grvtags_player_data SET current_tag = ? WHERE uuid = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, tagName);
            stmt.setString(2, playerUUID.toString());

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al establecer tag para jugador " + playerUUID + ":", e);
            return false;
        }
    }

    /**
     * Desbloquea un tag para un jugador
     */
    public static boolean unlockTagForPlayer(UUID playerUUID, String tagName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                INSERT IGNORE INTO grvtags_unlocked_tags (player_uuid, tag_name) 
                VALUES (?, ?)
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, tagName);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al desbloquear tag " + tagName + " para jugador " + playerUUID + ":", e);
            return false;
        }
    }

    /**
     * Verifica si un jugador tiene un tag desbloqueado
     */
    public static boolean hasPlayerUnlockedTag(UUID playerUUID, String tagName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = "SELECT 1 FROM grvtags_unlocked_tags WHERE player_uuid = ? AND tag_name = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, tagName);

            ResultSet rs = stmt.executeQuery();
            boolean hasTag = rs.next();

            rs.close();
            stmt.close();

            return hasTag;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al verificar tag desbloqueado " + tagName + " para jugador " + playerUUID + ":", e);
            return false;
        }
    }

    /**
     * Obtiene todos los tags desbloqueados de un jugador
     */
    public static List<String> getPlayerUnlockedTags(UUID playerUUID) {
        List<String> unlockedTags = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return unlockedTags;

            String query = "SELECT tag_name FROM grvtags_unlocked_tags WHERE player_uuid = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                unlockedTags.add(rs.getString("tag_name"));
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al obtener tags desbloqueados para jugador " + playerUUID + ":", e);
        }

        return unlockedTags;
    }

    /**
     * Obtiene el número total de tags desbloqueados por un jugador
     */
    public static int getPlayerTagCount(UUID playerUUID) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return 0;

            String query = "SELECT COUNT(*) FROM grvtags_unlocked_tags WHERE player_uuid = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            rs.close();
            stmt.close();

            return count;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al contar tags de jugador " + playerUUID + ":", e);
            return 0;
        }
    }

    /**
     * Obtiene el número de tags desbloqueados por categoría
     */
    public static int getPlayerTagCountByCategory(UUID playerUUID, String categoryName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return 0;

            String query = """
                SELECT COUNT(*) FROM grvtags_unlocked_tags u
                JOIN grvtags_tags t ON u.tag_name = t.name
                WHERE u.player_uuid = ? AND t.category = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, categoryName);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            rs.close();
            stmt.close();

            return count;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al contar tags por categoría " + categoryName + " para jugador " + playerUUID + ":", e);
            return 0;
        }
    }

    /**
     * Obtiene el número de tags desbloqueados por nombre de jugador
     */
    public static int getPlayerTagCount(String playerName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return 0;

            String query = """
                SELECT COUNT(*) FROM grvtags_unlocked_tags u
                JOIN grvtags_player_data p ON u.player_uuid = p.uuid
                WHERE p.username = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            rs.close();
            stmt.close();

            return count;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al contar tags de jugador " + playerName + ":", e);
            return 0;
        }
    }

    /**
     * Obtiene el número de tags desbloqueados por categoría y nombre de jugador
     */
    public static int getPlayerTagCountByCategory(String playerName, String categoryName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return 0;

            String query = """
                SELECT COUNT(*) FROM grvtags_unlocked_tags u
                JOIN grvtags_player_data p ON u.player_uuid = p.uuid
                JOIN grvtags_tags t ON u.tag_name = t.name
                WHERE p.username = ? AND t.category = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            stmt.setString(2, categoryName);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            rs.close();
            stmt.close();

            return count;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al contar tags por categoría " + categoryName + " para jugador " + playerName + ":", e);
            return 0;
        }
    }

    /**
     * Quita un tag desbloqueado de un jugador
     */
    public static boolean removeUnlockedTag(UUID playerUUID, String tagName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = "DELETE FROM grvtags_unlocked_tags WHERE player_uuid = ? AND tag_name = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, tagName);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            return rowsAffected > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al quitar tag desbloqueado " + tagName + " de jugador " + playerUUID + ":", e);
            return false;
        }
    }

    /**
     * Obtiene el tag default
     */
    public static String getDefaultTag() {
        return defaultTag;
    }

    /**
     * Recarga el tag default desde tags.yml
     */
    public static void reloadDefaultTag() {
        loadDefaultTag();
    }
}