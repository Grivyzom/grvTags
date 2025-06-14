package gc.grivyzom.managers;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.models.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CategoryManager {

    private static grvTags plugin;
    private static Map<String, Category> loadedCategories = new HashMap<>();

    /**
     * Inicializa el CategoryManager
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        createTablesIfNotExist();
        loadAllCategories();
    }

    /**
     * Crea las tablas necesarias si no existen
     */
    private static void createTablesIfNotExist() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) {
                plugin.getLogger().severe("No se pudo obtener conexión para crear tablas de categorías");
                return;
            }

            // Crear tabla de categorías
            String createCategoriesTable = """
                CREATE TABLE IF NOT EXISTS grvtags_categories (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(15) NOT NULL UNIQUE,
                    title VARCHAR(100) NOT NULL,
                    material VARCHAR(50) DEFAULT 'NAME_TAG',
                    display_name VARCHAR(100) NOT NULL,
                    slot_position INT DEFAULT 11,
                    lore TEXT,
                    permission VARCHAR(100),
                    permission_see_category BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;

            PreparedStatement stmt = conn.prepareStatement(createCategoriesTable);
            stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("Tabla 'grvtags_categories' verificada/creada correctamente");

            // Insertar categorías por defecto si no existen
            insertDefaultCategories();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tablas de categorías:", e);
        }
    }

    /**
     * Inserta las categorías por defecto
     */
    private static void insertDefaultCategories() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Verificar si ya existen categorías
            String checkQuery = "SELECT COUNT(*) FROM grvtags_categories";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            ResultSet rs = checkStmt.executeQuery();

            rs.next();
            int count = rs.getInt(1);
            rs.close();
            checkStmt.close();

            if (count > 0) {
                plugin.getLogger().info("Ya existen categorías en la base de datos, omitiendo inserción por defecto");
                return;
            }

            // Insertar categorías por defecto
            String insertQuery = """
                INSERT INTO grvtags_categories (name, title, material, display_name, slot_position, lore, permission, permission_see_category) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = conn.prepareStatement(insertQuery);

            // Categoría Default
            stmt.setString(1, "default");
            stmt.setString(2, "&8Default Tags (Page %page%)");
            stmt.setString(3, "NAME_TAG");
            stmt.setString(4, "&7&lDefault Tags");
            stmt.setInt(5, 11);
            stmt.setString(6, "&8&m-----------------------------||&7Default tags: &7%tags_amount%||&8&m-----------------------------");
            stmt.setString(7, "grvtags.category.default");
            stmt.setBoolean(8, false);
            stmt.addBatch();

            // Categoría VIP
            stmt.setString(1, "vip");
            stmt.setString(2, "&8Rank Tags (Page %page%)");
            stmt.setString(3, "NAME_TAG");
            stmt.setString(4, "&6&lRank Tags");
            stmt.setInt(5, 13);
            stmt.setString(6, "&8&m-----------------------------||&6Rank tags: &7%tags_amount%||&8&m-----------------------------");
            stmt.setString(7, "grvtags.category.rank");
            stmt.setBoolean(8, false);
            stmt.addBatch();

            // Categoría Christmas
            stmt.setString(1, "xmas");
            stmt.setString(2, "&8Christmas Tags (Page %page%)");
            stmt.setString(3, "NAME_TAG");
            stmt.setString(4, "&c&lChristmas Tags");
            stmt.setInt(5, 15);
            stmt.setString(6, "&8&m-----------------------------||&cChristmas tags: &7%tags_amount%||&8&m-----------------------------");
            stmt.setString(7, "grvtags.category.xmas");
            stmt.setBoolean(8, false);
            stmt.addBatch();

            int[] results = stmt.executeBatch();
            stmt.close();

            plugin.getLogger().info("Insertadas " + results.length + " categorías por defecto");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al insertar categorías por defecto:", e);
        }
    }

    /**
     * Carga todas las categorías desde la base de datos
     */
    public static void loadAllCategories() {
        loadedCategories.clear();

        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            String query = "SELECT * FROM grvtags_categories ORDER BY slot_position";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            int categoryCount = 0;
            while (rs.next()) {
                Category category = new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getString("material"),
                        rs.getString("display_name"),
                        rs.getInt("slot_position"),
                        rs.getString("lore"),
                        rs.getString("permission"),
                        rs.getBoolean("permission_see_category")
                );

                loadedCategories.put(category.getName().toLowerCase(), category);
                categoryCount++;
            }

            rs.close();
            stmt.close();

            plugin.getLogger().info("Cargadas " + categoryCount + " categorías desde la base de datos");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar categorías desde la base de datos:", e);
        }
    }

    /**
     * Crea una nueva categoría en la base de datos
     */
    public static boolean createCategory(String name, String displayName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                INSERT INTO grvtags_categories (name, title, material, display_name, slot_position, lore, permission, permission_see_category)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, "&8" + displayName + " Tags (Page %page%)");
            stmt.setString(3, "NAME_TAG");
            stmt.setString(4, "&7&l" + displayName + " Tags");
            stmt.setInt(5, getNextAvailableSlot());
            stmt.setString(6, "&8&m-----------------------------||&7" + displayName + " tags: &7%tags_amount%||&8&m-----------------------------");
            stmt.setString(7, "grvtags.category." + name.toLowerCase());
            stmt.setBoolean(8, false);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                plugin.getLogger().info("Categoría '" + name + "' creada exitosamente");
                loadAllCategories(); // Recargar categorías
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear categoría '" + name + "':", e);
        }

        return false;
    }

    /**
     * Verifica si una categoría existe
     */
    public static boolean categoryExists(String name) {
        return loadedCategories.containsKey(name.toLowerCase());
    }

    /**
     * Obtiene una categoría por su nombre
     */
    public static Category getCategory(String name) {
        return loadedCategories.get(name.toLowerCase());
    }

    /**
     * Obtiene todas las categorías
     */
    public static List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>(loadedCategories.values());
        categories.sort((c1, c2) -> Integer.compare(c1.getSlotPosition(), c2.getSlotPosition()));
        return categories;
    }

    /**
     * Obtiene el siguiente slot disponible
     */
    private static int getNextAvailableSlot() {
        int maxSlot = 10; // Empezar desde slot 11

        for (Category category : loadedCategories.values()) {
            maxSlot = Math.max(maxSlot, category.getSlotPosition());
        }

        return maxSlot + 2; // Dejar un espacio entre categorías
    }

    /**
     * Actualiza una categoría en la base de datos
     */
    public static boolean updateCategory(Category category) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                UPDATE grvtags_categories SET 
                    title = ?, 
                    material = ?, 
                    display_name = ?, 
                    slot_position = ?, 
                    lore = ?, 
                    permission = ?, 
                    permission_see_category = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE name = ?
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, category.getTitle());
            stmt.setString(2, category.getMaterial());
            stmt.setString(3, category.getDisplayName());
            stmt.setInt(4, category.getSlotPosition());
            stmt.setString(5, category.getLore());
            stmt.setString(6, category.getPermission());
            stmt.setBoolean(7, category.isPermissionSeeCategory());
            stmt.setString(8, category.getName());

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                loadAllCategories(); // Recargar categorías
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar categoría '" + category.getName() + "':", e);
        }

        return false;
    }

    /**
     * Elimina una categoría (solo si no tiene tags)
     */
    public static boolean deleteCategory(String name) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            // Verificar si la categoría tiene tags
            String checkQuery = "SELECT COUNT(*) FROM grvtags_tags WHERE category = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();

            rs.next();
            int tagCount = rs.getInt(1);
            rs.close();
            checkStmt.close();

            if (tagCount > 0) {
                plugin.getLogger().warning("No se puede eliminar la categoría '" + name + "' porque tiene " + tagCount + " tags asociados");
                return false;
            }

            // Eliminar la categoría
            String deleteQuery = "DELETE FROM grvtags_categories WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(deleteQuery);
            stmt.setString(1, name);

            int rowsAffected = stmt.executeUpdate();
            stmt.close();

            if (rowsAffected > 0) {
                loadedCategories.remove(name.toLowerCase());
                plugin.getLogger().info("Categoría '" + name + "' eliminada exitosamente");
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al eliminar categoría '" + name + "':", e);
        }

        return false;
    }

    /**
     * Obtiene el número total de categorías
     */
    public static int getCategoryCount() {
        return loadedCategories.size();
    }

    /**
     * Obtiene todos los nombres de categorías
     */
    public static List<String> getAllCategoryNames() {
        return new ArrayList<>(loadedCategories.keySet());
    }
}