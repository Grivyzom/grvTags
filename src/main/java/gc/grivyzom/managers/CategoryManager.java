package gc.grivyzom.managers;

import gc.grivyzom.database.DatabaseManager;
import gc.grivyzom.grvTags;
import gc.grivyzom.models.Category;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class CategoryManager {

    private static grvTags plugin;
    private static Map<String, Category> loadedCategories = new HashMap<>();

    // Cache para optimización
    private static long lastLoadTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 segundos

    /**
     * Inicializa el CategoryManager
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        createTablesIfNotExist();

        // CORREGIDO: Cargar desde YAML primero en la inicialización
        loadCategoriesFromYaml();
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
                    is_from_yaml BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;

            PreparedStatement stmt = conn.prepareStatement(createCategoriesTable);
            stmt.executeUpdate();
            stmt.close();

            plugin.getLogger().info("Tabla 'grvtags_categories' verificada/creada correctamente");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tablas de categorías:", e);
        }
    }

    /**
     * Carga todas las categorías desde la base de datos
     */
    public static void loadAllCategories() {
        // Cache simple para evitar cargas innecesarias
        if (System.currentTimeMillis() - lastLoadTime < CACHE_DURATION && !loadedCategories.isEmpty()) {
            return;
        }

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

            lastLoadTime = System.currentTimeMillis();

            plugin.getLogger().info("Cargadas " + categoryCount + " categorías desde la base de datos");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar categorías desde la base de datos:", e);
        }
    }

    /**
     * Recarga todas las categorías desde los archivos YAML y sincroniza con la base de datos
     */
    public static void reloadCategoriesFromYaml() {
        try {
            plugin.getLogger().info("Recargando categorías desde categories.yml...");

            // Cargar desde YAML y sincronizar con BD
            loadCategoriesFromYaml();

            // Luego cargar desde BD para obtener los datos actualizados
            lastLoadTime = 0; // Forzar recarga
            loadAllCategories();

            plugin.getLogger().info("Categorías recargadas exitosamente desde YAML");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al recargar categorías desde YAML:", e);
        }
    }

    /**
     * MEJORADO: Carga categorías desde categories.yml con limpieza de BD
     */
    private static void loadCategoriesFromYaml() {
        try {
            File categoriesFile = new File(plugin.getDataFolder(), "categories.yml");

            if (!categoriesFile.exists()) {
                plugin.saveResource("categories.yml", false);
            }

            YamlConfiguration categoriesConfig = YamlConfiguration.loadConfiguration(categoriesFile);
            ConfigurationSection categoriesSection = categoriesConfig.getConfigurationSection("categories");

            if (categoriesSection == null) {
                plugin.getLogger().warning("No se encontró sección 'categories' en categories.yml");
                return;
            }

            // NUEVO: Marcar todas las categorías como no-yaml primero
            markAllCategoriesAsNonYaml();

            // Obtener todas las categorías del YAML
            Set<String> yamlCategories = categoriesSection.getKeys(false);

            int syncedCategories = 0;
            for (String categoryName : yamlCategories) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryName);
                if (categorySection == null) continue;

                String title = categorySection.getString("title", "&8" + categoryName + " Tags (Page %page%)");
                String material = categorySection.getString("material", "NAME_TAG");
                String displayName = categorySection.getString("id_display", "&7&l" + categoryName + " Tags");
                int slot = categorySection.getInt("slot", 11);
                String permission = categorySection.getString("permission", "grvtags.category." + categoryName.toLowerCase());
                boolean permissionSeeCategory = categorySection.getBoolean("permission-see-category", false);

                // Construir lore desde la lista
                List<String> loreList = categorySection.getStringList("lore");
                String lore = String.join("||", loreList);

                // Sincronizar con base de datos
                syncCategoryToDatabase(categoryName, title, material, displayName, slot, lore, permission, permissionSeeCategory);
                syncedCategories++;
            }

            // NUEVO: Eliminar categorías que ya no están en el YAML
            cleanupRemovedCategories();

            plugin.getLogger().info("Sincronizadas " + syncedCategories + " categorías desde categories.yml a la base de datos");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar categorías desde categories.yml:", e);
        }
    }

    /**
     * NUEVO: Marca todas las categorías como no-yaml para identificar cuáles eliminar
     */
    private static void markAllCategoriesAsNonYaml() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            String query = "UPDATE grvtags_categories SET is_from_yaml = FALSE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al marcar categorías como no-yaml:", e);
        }
    }

    /**
     * NUEVO: Elimina categorías que ya no están en el YAML
     */
    private static void cleanupRemovedCategories() {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Verificar si hay tags en categorías a eliminar
            String checkTagsQuery = """
                SELECT c.name, COUNT(t.id) as tag_count 
                FROM grvtags_categories c 
                LEFT JOIN grvtags_tags t ON c.name = t.category 
                WHERE c.is_from_yaml = FALSE 
                GROUP BY c.name
            """;

            PreparedStatement checkStmt = conn.prepareStatement(checkTagsQuery);
            ResultSet rs = checkStmt.executeQuery();

            List<String> categoriesToDelete = new ArrayList<>();

            while (rs.next()) {
                String categoryName = rs.getString("name");
                int tagCount = rs.getInt("tag_count");

                if (tagCount > 0) {
                    plugin.getLogger().warning("No se puede eliminar la categoría '" + categoryName +
                            "' porque tiene " + tagCount + " tags asociados. Considere mover o eliminar los tags primero.");
                } else {
                    categoriesToDelete.add(categoryName);
                }
            }

            rs.close();
            checkStmt.close();

            // Eliminar categorías sin tags
            if (!categoriesToDelete.isEmpty()) {
                String deleteQuery = "DELETE FROM grvtags_categories WHERE name = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);

                for (String categoryName : categoriesToDelete) {
                    deleteStmt.setString(1, categoryName);
                    deleteStmt.addBatch();
                }

                int[] results = deleteStmt.executeBatch();
                deleteStmt.close();

                plugin.getLogger().info("Eliminadas " + results.length + " categorías que ya no están en categories.yml");

                for (String categoryName : categoriesToDelete) {
                    plugin.getLogger().info("- Categoría eliminada: " + categoryName);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al limpiar categorías removidas:", e);
        }
    }

    /**
     * MEJORADO: Sincroniza una categoría con la base de datos
     */
    private static void syncCategoryToDatabase(String name, String title, String material,
                                               String displayName, int slot, String lore,
                                               String permission, boolean permissionSeeCategory) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return;

            // Verificar si la categoría ya existe
            String checkQuery = "SELECT id FROM grvtags_categories WHERE name = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, name);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Categoría existe, actualizar y marcar como del YAML
                String updateQuery = """
                UPDATE grvtags_categories SET 
                    title = ?, material = ?, display_name = ?, 
                    slot_position = ?, lore = ?, permission = ?, 
                    permission_see_category = ?, is_from_yaml = TRUE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE name = ?
            """;

                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setString(1, title);
                updateStmt.setString(2, material);
                updateStmt.setString(3, displayName);
                updateStmt.setInt(4, slot);
                updateStmt.setString(5, lore);
                updateStmt.setString(6, permission);
                updateStmt.setBoolean(7, permissionSeeCategory);
                updateStmt.setString(8, name);

                updateStmt.executeUpdate();
                updateStmt.close();

            } else {
                // Categoría no existe, insertar
                String insertQuery = """
                INSERT INTO grvtags_categories (name, title, material, display_name, 
                                              slot_position, lore, permission, permission_see_category, is_from_yaml)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)
            """;

                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setString(1, name);
                insertStmt.setString(2, title);
                insertStmt.setString(3, material);
                insertStmt.setString(4, displayName);
                insertStmt.setInt(5, slot);
                insertStmt.setString(6, lore);
                insertStmt.setString(7, permission);
                insertStmt.setBoolean(8, permissionSeeCategory);

                insertStmt.executeUpdate();
                insertStmt.close();
            }

            rs.close();
            checkStmt.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al sincronizar categoría '" + name + "' con la base de datos:", e);
        }
    }

    // =================== MÉTODOS EXISTENTES (sin cambios) ===================

    public static boolean createCategory(String name, String displayName) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                INSERT INTO grvtags_categories (name, title, material, display_name, slot_position, lore, permission, permission_see_category, is_from_yaml)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE)
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
                lastLoadTime = 0; // Forzar recarga
                loadAllCategories(); // Recargar categorías
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear categoría '" + name + "':", e);
        }

        return false;
    }

    public static boolean categoryExists(String name) {
        return loadedCategories.containsKey(name.toLowerCase());
    }

    public static Category getCategory(String name) {
        return loadedCategories.get(name.toLowerCase());
    }

    public static List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>(loadedCategories.values());
        categories.sort((c1, c2) -> Integer.compare(c1.getSlotPosition(), c2.getSlotPosition()));
        return categories;
    }

    private static int getNextAvailableSlot() {
        int maxSlot = 10; // Empezar desde slot 11
        for (Category category : loadedCategories.values()) {
            maxSlot = Math.max(maxSlot, category.getSlotPosition());
        }
        return maxSlot + 2; // Dejar un espacio entre categorías
    }

    public static boolean updateCategory(Category category) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            String query = """
                UPDATE grvtags_categories SET 
                    title = ?, material = ?, display_name = ?, 
                    slot_position = ?, lore = ?, permission = ?, 
                    permission_see_category = ?, updated_at = CURRENT_TIMESTAMP
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
                lastLoadTime = 0; // Forzar recarga
                loadAllCategories(); // Recargar categorías
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar categoría '" + category.getName() + "':", e);
        }

        return false;
    }

    public static boolean deleteCategory(String name) {
        try {
            Connection conn = DatabaseManager.getConnection();
            if (conn == null) return false;

            plugin.getLogger().info("Iniciando eliminación de categoría '" + name + "'...");

            // 1. Obtener lista de tags en la categoría antes de eliminar
            String getTagsQuery = "SELECT name FROM grvtags_tags WHERE category = ?";
            PreparedStatement getTagsStmt = conn.prepareStatement(getTagsQuery);
            getTagsStmt.setString(1, name);
            ResultSet tagsRs = getTagsStmt.executeQuery();

            List<String> tagsToDelete = new ArrayList<>();
            while (tagsRs.next()) {
                tagsToDelete.add(tagsRs.getString("name"));
            }
            tagsRs.close();
            getTagsStmt.close();

            plugin.getLogger().info("Tags a eliminar: " + tagsToDelete.size());

            // 2. Para cada tag, resetear jugadores que lo tengan activo
            if (!tagsToDelete.isEmpty()) {
                plugin.getLogger().info("Reseteando tags activos de jugadores...");

                for (String tagName : tagsToDelete) {
                    // Resetear tags activos
                    String resetActiveQuery = "UPDATE grvtags_player_data SET current_tag = NULL WHERE current_tag = ?";
                    PreparedStatement resetStmt = conn.prepareStatement(resetActiveQuery);
                    resetStmt.setString(1, tagName);
                    int resetCount = resetStmt.executeUpdate();
                    resetStmt.close();

                    if (resetCount > 0) {
                        plugin.getLogger().info("- Tag '" + tagName + "': " + resetCount + " jugadores reseteados");
                    }

                    // Eliminar desbloqueos del tag
                    String deleteUnlocksQuery = "DELETE FROM grvtags_unlocked_tags WHERE tag_name = ?";
                    PreparedStatement deleteUnlocksStmt = conn.prepareStatement(deleteUnlocksQuery);
                    deleteUnlocksStmt.setString(1, tagName);
                    int unlocksDeleted = deleteUnlocksStmt.executeUpdate();
                    deleteUnlocksStmt.close();

                    if (unlocksDeleted > 0) {
                        plugin.getLogger().info("- Tag '" + tagName + "': " + unlocksDeleted + " desbloqueos eliminados");
                    }
                }
            }

            // 3. Eliminar todos los tags de la categoría
            String deleteTagsQuery = "DELETE FROM grvtags_tags WHERE category = ?";
            PreparedStatement deleteTagsStmt = conn.prepareStatement(deleteTagsQuery);
            deleteTagsStmt.setString(1, name);
            int tagsDeleted = deleteTagsStmt.executeUpdate();
            deleteTagsStmt.close();

            plugin.getLogger().info("Tags eliminados de la BD: " + tagsDeleted);

            // 4. Eliminar la categoría
            String deleteCategoryQuery = "DELETE FROM grvtags_categories WHERE name = ?";
            PreparedStatement deleteCategoryStmt = conn.prepareStatement(deleteCategoryQuery);
            deleteCategoryStmt.setString(1, name);
            int categoryDeleted = deleteCategoryStmt.executeUpdate();
            deleteCategoryStmt.close();

            if (categoryDeleted > 0) {
                plugin.getLogger().info("Categoría '" + name + "' eliminada de la BD");

                // 5. NUEVO: Actualizar archivos YAML
                updateYamlFilesAfterCategoryDeletion(name, tagsToDelete);

                // 6. Actualizar cache
                loadedCategories.remove(name.toLowerCase());

                // Forzar recarga del TagManager para actualizar cache
                TagManager.forceReload();

                plugin.getLogger().info("Eliminación de categoría '" + name + "' completada exitosamente");
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al eliminar categoría '" + name + "':", e);
        }

        return false;
    }

    public static int getCategoryCount() {
        return loadedCategories.size();
    }

    public static List<String> getAllCategoryNames() {
        return new ArrayList<>(loadedCategories.keySet());
    }


    private static void updateYamlFilesAfterCategoryDeletion(String categoryName, List<String> deletedTags) {
        try {
            plugin.getLogger().info("Actualizando archivos YAML después de eliminar categoría '" + categoryName + "'...");

            // Actualizar categories.yml
            updateCategoriesYamlFile(categoryName);

            // Actualizar tags.yml
            updateTagsYamlFile(deletedTags);

            plugin.getLogger().info("Archivos YAML actualizados correctamente");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar archivos YAML:", e);
        }
    }

    /**
     * NUEVO: Actualiza el archivo categories.yml eliminando la categoría especificada
     */
    private static void updateCategoriesYamlFile(String categoryName) {
        try {
            File categoriesFile = new File(plugin.getDataFolder(), "categories.yml");
            if (!categoriesFile.exists()) {
                plugin.getLogger().warning("Archivo categories.yml no encontrado, no se puede actualizar");
                return;
            }

            YamlConfiguration categoriesConfig = YamlConfiguration.loadConfiguration(categoriesFile);
            ConfigurationSection categoriesSection = categoriesConfig.getConfigurationSection("categories");

            if (categoriesSection != null && categoriesSection.contains(categoryName)) {
                // Eliminar la categoría del YAML
                categoriesSection.set(categoryName, null);

                // Guardar el archivo
                categoriesConfig.save(categoriesFile);
                plugin.getLogger().info("Categoría '" + categoryName + "' eliminada de categories.yml");

            } else {
                plugin.getLogger().info("Categoría '" + categoryName + "' no encontrada en categories.yml");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar categories.yml:", e);
        }
    }

    /**
     * NUEVO: Actualiza el archivo tags.yml eliminando los tags especificados
     */
    private static void updateTagsYamlFile(List<String> tagsToDelete) {
        try {
            File tagsFile = new File(plugin.getDataFolder(), "tags.yml");
            if (!tagsFile.exists()) {
                plugin.getLogger().warning("Archivo tags.yml no encontrado, no se puede actualizar");
                return;
            }

            YamlConfiguration tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
            ConfigurationSection tagsSection = tagsConfig.getConfigurationSection("tags");

            if (tagsSection != null) {
                int tagsRemoved = 0;

                for (String tagName : tagsToDelete) {
                    if (tagsSection.contains(tagName)) {
                        // Eliminar el tag del YAML
                        tagsSection.set(tagName, null);
                        tagsRemoved++;
                        plugin.getLogger().info("Tag '" + tagName + "' eliminado de tags.yml");
                    }
                }

                if (tagsRemoved > 0) {
                    // Guardar el archivo
                    tagsConfig.save(tagsFile);
                    plugin.getLogger().info("Total de tags eliminados de tags.yml: " + tagsRemoved);
                } else {
                    plugin.getLogger().info("No se encontraron tags para eliminar en tags.yml");
                }

            } else {
                plugin.getLogger().warning("Sección 'tags' no encontrada en tags.yml");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al actualizar tags.yml:", e);
        }
    }
}