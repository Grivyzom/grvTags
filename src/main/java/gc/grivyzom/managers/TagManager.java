package gc.grivyzom.managers;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.models.Tag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TagManager {

    private static grvTags plugin;
    private static Map<String, Tag> loadedTags = new HashMap<>();

    /**
     * Inicializa el TagManager
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        createTablesIfNotExist();
        loadAllTags();
    }

    /**
     * Crea las tablas necesarias si no existen
     */
    private static void createTablesIfNotExist() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("No se pudo obtener conexión para crear tablas de tags");
                return;
            }

            // Crear tabla de tags
            String createTagsTable = """
                CREATE TABLE IF NOT EXISTS grvtags_tags (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(20) NOT NULL UNIQUE,
                    display_tag TEXT NOT NULL,
                    permission VARCHAR(100),
                    description TEXT,
                    category VARCHAR(15) NOT NULL,
                    display_order INT DEFAULT 1,
                    display_name VARCHAR(100),
                    display_item VARCHAR(50) DEFAULT 'NAME_TAG',
                    cost INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;

            PreparedStatement stmt = conn.prepareStatement(createTagsTable);
            stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("Tabla 'grvtags_tags' verificada/creada correctamente");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tablas de tags:", e);
        }
    }

    /**
     * Carga todos los tags desde la base de datos
     */
    public static void loadAllTags() {
        loadedTags.clear();

        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            String query = "SELECT * FROM grvtags_tags ORDER BY category, display_order";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            int tagCount = 0;
            while (rs.next()) {
                Tag tag = new Tag(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("display_tag"),
                        rs.getString("permission"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getInt("display_order"),
                        rs.getString("display_name"),
                        rs.getString("display_item"),
                        rs.getInt("cost")
                );

                loadedTags.put(tag.getName().toLowerCase(), tag);
                tagCount++;
            }

            rs.close();
            stmt.close();

            plugin.getLogger().info("Cargados " + tagCount + " tags desde la base de datos");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar tags desde la base de datos:", e);
        }
    }

    /**
     * Crea un nuevo tag en la base de datos
     */
    public static boolean createTag(String name, String category, String displayTag) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            // Verificar que la categoría existe
            if (!CategoryManager.categoryExists(category)) {
                plugin.getLogger().warning("Intento de crear tag '" + name + "' en categoría inexistente: " + category);
                return false;
            }

            String query = """
                INSERT INTO grvtags_tags (name, display_tag, permission, description, category, display_order, display_name, display_item, cost)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, displayTag != null ? displayTag : "&8[&7" + name + "&8]");
            stmt.setString(3, "grvtags.tag." + name.toLowerCase());
            stmt.setString(4, "Tag creado automáticamente");
            stmt.setString(5, category);
            stmt.setInt(6, getNextOrderInCategory(category));
            stmt.setString(7, "&7Tag: %tag%");
            stmt.setString(8, "NAME_TAG");
            stmt.setInt(9, 0);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                plugin.getLogger().info("Tag '" + name + "' creado exitosamente en categoría '" + category + "'");
                loadAllTags(); // Recargar tags
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tag '" + name + "':", e);
        }

        return false;
    }

    /**
     * Verifica si un tag existe
     */
    public static boolean tagExists(String name) {
        return loadedTags.containsKey(name.toLowerCase());
    }

    /**
     * Obtiene un tag por su nombre
     */
    public static Tag getTag(String name) {
        return loadedTags.get(name.toLowerCase());
    }

    /**
     * Obtiene todos los tags de una categoría
     */
    public static List<Tag> getTagsByCategory(String category) {
        List<Tag> categoryTags = new ArrayList<>();

        for (Tag tag : loadedTags.values()) {
            if (tag.getCategory().equalsIgnoreCase(category)) {
                categoryTags.add(tag);
            }
        }

        // Ordenar por display_order
        categoryTags.sort((t1, t2) -> Integer.compare(t1.getDisplayOrder(), t2.getDisplayOrder()));

        return categoryTags;
    }

    /**
     * Obtiene todos los tags agrupados por categoría
     */
    public static Map<String, List<Tag>> getAllTagsGroupedByCategory() {
        Map<String, List<Tag>> groupedTags = new HashMap<>();

        for (Tag tag : loadedTags.values()) {
            String category = tag.getCategory();
            groupedTags.computeIfAbsent(category, k -> new ArrayList<>()).add(tag);
        }

        // Ordenar cada lista por display_order
        for (List<Tag> tagList : groupedTags.values()) {
            tagList.sort((t1, t2) -> Integer.compare(t1.getDisplayOrder(), t2.getDisplayOrder()));
        }

        return groupedTags;
    }

    /**
     * Obtiene el siguiente número de orden en una categoría
     */
    private static int getNextOrderInCategory(String category) {
        int maxOrder = 0;

        for (Tag tag : loadedTags.values()) {
            if (tag.getCategory().equalsIgnoreCase(category)) {
                maxOrder = Math.max(maxOrder, tag.getDisplayOrder());
            }
        }

        return maxOrder + 1;
    }

    /**
     * Actualiza un tag en la base de datos
     */
    public static boolean updateTag(Tag tag) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                UPDATE grvtags_tags SET 
                    display_tag = ?, 
                    permission = ?, 
                    description = ?, 
                    category = ?, 
                    display_order = ?, 
                    display_name = ?, 
                    display_item = ?, 
                    cost = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE name = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, tag.getDisplayTag());
            stmt.setString(2, tag.getPermission());
            stmt.setString(3, tag.getDescription());
            stmt.setString(4, tag.getCategory());
            stmt.setInt(5, tag.getDisplayOrder());
            stmt.setString(6, tag.getDisplayName());
            stmt.setString(7, tag.getDisplayItem());
            stmt.setInt(8, tag.getCost());
            stmt.setString(9, tag.getName());

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                loadAllTags(); // Recargar tags
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar tag '" + tag.getName() + "':", e);
        }

        return false;
    }

    /**
     * Elimina un tag
     */
    public static boolean deleteTag(String name) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = "DELETE FROM grvtags_tags WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                loadedTags.remove(name.toLowerCase());
                plugin.getLogger().info("Tag '" + name + "' eliminado exitosamente");
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al eliminar tag '" + name + "':", e);
        }

        return false;
    }

    /**
     * Obtiene el número total de tags
     */
    public static int getTagCount() {
        return loadedTags.size();
    }

    /**
     * Obtiene todos los nombres de tags
     */
    public static List<String> getAllTagNames() {
        return new ArrayList<>(loadedTags.keySet());
    }
}