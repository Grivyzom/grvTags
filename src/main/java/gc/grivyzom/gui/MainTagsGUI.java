package gc.grivyzom.gui;

import gc.grivyzom.grvTags;
import gc.grivyzom.managers.CategoryManager;
import gc.grivyzom.managers.PlayerDataManager;
import gc.grivyzom.managers.TagManager;
import gc.grivyzom.models.Category;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI principal que muestra todas las categorías de tags disponibles
 * Se abre cuando el jugador usa el comando /tags
 *
 * Versión mejorada con:
 * - Posicionamiento automático de categorías
 * - Efectos de sonido
 * - Optimizaciones de rendimiento
 * - Cache de datos para mejor eficiencia
 */
public class MainTagsGUI extends BaseGUI {

    // Cache de datos para optimizar rendimiento
    private final Map<String, Integer> categoryTagCounts = new HashMap<>();
    private final Map<String, Integer> playerTagCounts = new HashMap<>();
    private final Map<Integer, String> slotToCategoryMap = new HashMap<>();

    // Configuración de posicionamiento automático
    private static final int[] CATEGORY_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,  // Fila 3 (7 slots)
            28, 29, 30, 31, 32, 33, 34,  // Fila 4 (7 slots)
            37, 38, 39, 40, 41, 42, 43   // Fila 5 (7 slots)
    };

    // Slots especiales
    private static final int CURRENT_TAG_SLOT = 45;
    private static final int INFO_SLOT = 53;
    private static final int STATS_SLOT = 49;
    private static final int REFRESH_SLOT = 51;

    public MainTagsGUI(grvTags plugin, Player player) {
        super(plugin, player, "&8&lSelecciona una Categoría", 54);
        preloadData(); // Cargar datos una sola vez para optimizar
    }

    /**
     * Pre-carga los datos necesarios para optimizar el rendimiento
     */
    private void preloadData() {
        // Cargar conteos de tags por categoría
        List<Category> categories = CategoryManager.getAllCategories();
        for (Category category : categories) {
            int totalTags = TagManager.getTagsByCategory(category.getName()).size();
            int playerTags = PlayerDataManager.getPlayerTagCountByCategory(player.getUniqueId(), category.getName());

            categoryTagCounts.put(category.getName(), totalTags);
            playerTagCounts.put(category.getName(), playerTags);
        }
    }

    @Override
    public void setupGUI() {
        // Llenar bordes con cristal gris
        fillBorders();

        // Obtener categorías visibles para el jugador
        List<Category> visibleCategories = getVisibleCategories();

        if (visibleCategories.isEmpty()) {
            setupEmptyGUI();
            return;
        }

        // Colocar categorías automáticamente
        setupCategories(visibleCategories);

        // Configurar elementos de utilidad
        setupUtilityItems();
    }

    /**
     * Obtiene las categorías visibles para el jugador (con permisos)
     */
    private List<Category> getVisibleCategories() {
        List<Category> allCategories = CategoryManager.getAllCategories();
        List<Category> visibleCategories = new ArrayList<>();

        for (Category category : allCategories) {
            // Verificar permisos si es necesario
            if (category.isPermissionSeeCategory() &&
                    category.getPermission() != null &&
                    !player.hasPermission(category.getPermission())) {
                continue;
            }

            visibleCategories.add(category);
        }

        return visibleCategories;
    }

    /**
     * Configura las categorías en el GUI con posicionamiento automático
     */
    private void setupCategories(List<Category> categories) {
        slotToCategoryMap.clear();

        // Calcular el número de categorías que caben por fila
        int categoriesPerRow = Math.min(7, categories.size());
        int totalRows = (int) Math.ceil((double) categories.size() / categoriesPerRow);

        // Centrar las categorías si hay pocas
        int startSlotIndex = 0;
        if (categories.size() <= 7) {
            // Una sola fila, centrar
            int emptySlots = 7 - categories.size();
            startSlotIndex = emptySlots / 2;
        }

        // Colocar las categorías
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);

            // Calcular posición
            int row = i / categoriesPerRow;
            int col = i % categoriesPerRow;
            int slotIndex = row * 7 + col + startSlotIndex;

            // Asegurarse de no exceder el array de slots
            if (slotIndex < CATEGORY_SLOTS.length) {
                int slot = CATEGORY_SLOTS[slotIndex];
                ItemStack categoryItem = createCategoryItem(category);
                inventory.setItem(slot, categoryItem);

                // Mapear slot a categoría para el manejo de clics
                slotToCategoryMap.put(slot, category.getName());
            }
        }
    }

    /**
     * Configura los elementos de utilidad del GUI
     */
    private void setupUtilityItems() {
        // Item de tag actual
        inventory.setItem(CURRENT_TAG_SLOT, createCurrentTagItem());

        // Item de información
        inventory.setItem(INFO_SLOT, createInfoItem());

        // Item de estadísticas globales
        inventory.setItem(STATS_SLOT, createGlobalStatsItem());

        // Botón de actualizar
        inventory.setItem(REFRESH_SLOT, createRefreshItem());
    }

    /**
     * Crea el item que representa una categoría (optimizado)
     */
    private ItemStack createCategoryItem(Category category) {
        // Obtener material de forma segura
        Material material = getMaterialSafely(category.getMaterial());

        // Obtener datos pre-cargados
        int totalTags = categoryTagCounts.getOrDefault(category.getName(), 0);
        int unlockedTags = playerTagCounts.getOrDefault(category.getName(), 0);

        // Crear lore personalizado de forma más eficiente
        List<String> lore = new ArrayList<>(category.getFormattedLore(totalTags));
        lore.add("");

        // Información de progreso con colores dinámicos
        double progressPercentage = totalTags > 0 ? (double) unlockedTags / totalTags * 100 : 0;
        String progressColor = getProgressColor(progressPercentage);

        lore.add("&7Tags desbloqueados: " + progressColor + unlockedTags + "&7/&f" + totalTags);

        // Barra de progreso mejorada
        String progressBar = createEnhancedProgressBar(progressPercentage);
        lore.add("&7Progreso: " + progressBar + " &f" + String.format("%.1f", progressPercentage) + "%");

        lore.add("");

        // Información de acceso con iconos
        if (totalTags > 0) {
            if (unlockedTags > 0) {
                lore.add("&a✓ Click para ver tus tags");
                if (unlockedTags == totalTags) {
                    lore.add("&6★ ¡Categoría completada!");
                }
            } else {
                lore.add("&e▶ Click para explorar tags");
            }
        } else {
            lore.add("&c✗ Categoría vacía");
        }

        // Información de permisos si es necesaria
        if (category.isPermissionSeeCategory() && category.getPermission() != null) {
            if (!player.hasPermission(category.getPermission())) {
                lore.add("");
                lore.add("&c⚠ Requiere: " + category.getPermission());
            }
        }

        return createItem(material, category.getFormattedDisplayName(), lore);
    }

    /**
     * Obtiene el color basado en el porcentaje de progreso
     */
    private String getProgressColor(double percentage) {
        if (percentage >= 100) return "&6"; // Dorado para completado
        if (percentage >= 75) return "&a";  // Verde para alto
        if (percentage >= 50) return "&e";  // Amarillo para medio
        if (percentage >= 25) return "&6";  // Naranja para bajo
        return "&c"; // Rojo para muy bajo
    }

    /**
     * Crea una barra de progreso visual mejorada
     */
    private String createEnhancedProgressBar(double percentage) {
        int totalBars = 10;
        int filledBars = (int) Math.round(percentage / 100.0 * totalBars);

        StringBuilder progressBar = new StringBuilder("&8[");

        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                // Color dinámico basado en progreso
                if (percentage >= 100) {
                    progressBar.append("&6■"); // Dorado para completado
                } else if (percentage >= 75) {
                    progressBar.append("&a■"); // Verde
                } else if (percentage >= 50) {
                    progressBar.append("&e■"); // Amarillo
                } else if (percentage >= 25) {
                    progressBar.append("&6■"); // Naranja
                } else {
                    progressBar.append("&c■"); // Rojo
                }
            } else {
                progressBar.append("&7■");
            }
        }

        progressBar.append("&8]");
        return progressBar.toString();
    }

    /**
     * Crea el item que muestra el tag actual del jugador
     */
    private ItemStack createCurrentTagItem() {
        String currentTag = PlayerDataManager.getPlayerTag(player.getUniqueId());
        int totalUnlockedTags = PlayerDataManager.getPlayerTagCount(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("&7Tu tag actual:");
        lore.add(currentTag);
        lore.add("");
        lore.add("&7Total desbloqueados: &a" + totalUnlockedTags);
        lore.add("");
        lore.add("&8💡 Explora las categorías para");
        lore.add("&8encontrar y cambiar tu tag");

        return createItem(Material.PLAYER_HEAD, "&6&lTu Tag Actual", lore);
    }

    /**
     * Crea el item de estadísticas globales
     */
    private ItemStack createGlobalStatsItem() {
        int totalTags = TagManager.getTagCount();
        int totalCategories = CategoryManager.getCategoryCount();
        int playerTotalTags = PlayerDataManager.getPlayerTagCount(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("&7Estadísticas del servidor:");
        lore.add("&8▪ &7Total de tags: &f" + totalTags);
        lore.add("&8▪ &7Categorías: &f" + totalCategories);
        lore.add("");
        lore.add("&7Tus estadísticas:");
        lore.add("&8▪ &7Tags desbloqueados: &a" + playerTotalTags);

        double globalProgress = totalTags > 0 ? (double) playerTotalTags / totalTags * 100 : 0;
        lore.add("&8▪ &7Progreso global: &f" + String.format("%.1f", globalProgress) + "%");

        return createItem(Material.ENCHANTED_BOOK, "&b&lEstadísticas", lore);
    }

    /**
     * Crea el botón de actualizar
     */
    private ItemStack createRefreshItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Actualiza la información");
        lore.add("&7de este menú");
        lore.add("");
        lore.add("&e▶ Click para refrescar");

        return createItem(Material.ECHO_SHARD, "&b&l↻ Actualizar", lore);
    }

    /**
     * Configura el GUI cuando no hay categorías disponibles
     */
    private void setupEmptyGUI() {
        fillBorders();

        List<String> lore = new ArrayList<>();
        lore.add("&7No hay categorías disponibles");
        lore.add("&7para tu nivel de permisos");
        lore.add("");
        lore.add("&8Contacta con un administrador");
        lore.add("&8si crees que es un error");

        ItemStack emptyItem = createItem(Material.BARRIER, "&c&lSin Categorías", lore);
        inventory.setItem(22, emptyItem); // Centro del GUI

        inventory.setItem(INFO_SLOT, createInfoItem());
    }

    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Reproducir sonido base para cualquier clic válido
        playClickSound();

        // Manejar clics en slots especiales
        if (handleSpecialSlots(slot)) {
            return;
        }

        // Manejar clic en categorías usando el mapa optimizado
        String categoryName = slotToCategoryMap.get(slot);
        if (categoryName != null) {
            handleCategoryClick(categoryName);
        }
    }

    /**
     * Maneja los clics en slots especiales
     */
    private boolean handleSpecialSlots(int slot) {
        switch (slot) {
            case CURRENT_TAG_SLOT:
            case INFO_SLOT:
            case STATS_SLOT:
                // Estos slots solo muestran información
                playInfoSound();
                return true;

            case REFRESH_SLOT:
                // Refrescar el GUI
                playSuccessSound();
                player.sendMessage("§a✓ Menú actualizado");

                // Recargar datos y refrescar GUI
                preloadData();
                refresh();
                return true;

            default:
                return false;
        }
    }

    /**
     * Maneja el clic en una categoría específica
     */
    private void handleCategoryClick(String categoryName) {
        Category category = CategoryManager.getCategory(categoryName);
        if (category == null) {
            playErrorSound();
            player.sendMessage("§c✗ Error: Categoría no encontrada");
            return;
        }

        // Verificar permisos
        if (category.getPermission() != null && !player.hasPermission(category.getPermission())) {
            playErrorSound();
            player.sendMessage("§c✗ No tienes permisos para esta categoría");
            player.sendMessage("§7Requiere: §f" + category.getPermission());
            return;
        }

        // Verificar que la categoría tenga tags
        int totalTags = categoryTagCounts.getOrDefault(categoryName, 0);
        if (totalTags == 0) {
            playWarningSound();
            player.sendMessage("§e⚠ Esta categoría está vacía");
            return;
        }

        // Abrir GUI de la categoría
        playSuccessSound();
        GUIManager.openCategoryGUI(player, categoryName);
    }

    // =================== EFECTOS DE SONIDO ===================

    /**
     * Reproduce sonido de clic básico
     */
    private void playClickSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Reproduce sonido de éxito
     */
    private void playSuccessSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f);
    }

    /**
     * Reproduce sonido de error
     */
    private void playErrorSound() {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    /**
     * Reproduce sonido de advertencia
     */
    private void playWarningSound() {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 0.8f);
    }

    /**
     * Reproduce sonido informativo
     */
    private void playInfoSound() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.2f);
    }

    // =================== GETTERS PARA DEBUGGING ===================

    /**
     * Obtiene el mapeo de slots a categorías (útil para debugging)
     */
    public Map<Integer, String> getSlotToCategoryMap() {
        return new HashMap<>(slotToCategoryMap);
    }

    /**
     * Obtiene los conteos de tags por categoría
     */
    public Map<String, Integer> getCategoryTagCounts() {
        return new HashMap<>(categoryTagCounts);
    }
}