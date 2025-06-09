package gc.grivyzom.gui;

import gc.grivyzom.grvTags;
import gc.grivyzom.managers.CategoryManager;
import gc.grivyzom.managers.PlayerDataManager;
import gc.grivyzom.managers.TagManager;
import gc.grivyzom.models.Category;
import gc.grivyzom.models.Tag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI que muestra todos los tags de una categoría específica
 * Permite al jugador navegar, ver y seleccionar tags
 */
public class CategoryTagsGUI extends BaseGUI {

    private final String categoryName;
    private final int currentPage;
    private final int tagsPerPage = 21; // 3 filas de 7 tags cada una

    public CategoryTagsGUI(grvTags plugin, Player player, String categoryName, int page) {
        super(plugin, player, generateTitle(categoryName, page), 54);
        this.categoryName = categoryName;
        this.currentPage = Math.max(1, page);
    }

    /**
     * Genera el título del GUI dinámicamente
     */
    private static String generateTitle(String categoryName, int page) {
        Category category = CategoryManager.getCategory(categoryName);
        if (category != null) {
            return category.getFormattedTitle(page);
        }
        return "&8" + categoryName + " Tags (Página " + page + ")";
    }

    @Override
    public void setupGUI() {
        // Llenar bordes
        fillBorders();

        // Obtener la categoría
        Category category = CategoryManager.getCategory(categoryName);
        if (category == null) {
            setupErrorGUI("Categoría no encontrada");
            return;
        }

        // Obtener todos los tags de la categoría
        List<Tag> categoryTags = TagManager.getTagsByCategory(categoryName);

        if (categoryTags.isEmpty()) {
            setupEmptyGUI();
            return;
        }

        // Calcular paginación
        int totalPages = calculateTotalPages(categoryTags.size(), tagsPerPage);
        int startIndex = (currentPage - 1) * tagsPerPage;
        int endIndex = Math.min(startIndex + tagsPerPage, categoryTags.size());

        // Obtener tags para la página actual
        List<Tag> tagsToShow = categoryTags.subList(startIndex, endIndex);

        // Obtener slots de contenido
        List<Integer> contentSlots = getContentSlots();

        // Colocar tags en el GUI
        for (int i = 0; i < tagsToShow.size() && i < contentSlots.size(); i++) {
            Tag tag = tagsToShow.get(i);
            ItemStack tagItem = createTagItem(tag);
            inventory.setItem(contentSlots.get(i), tagItem);
        }

        // Añadir navegación si hay múltiples páginas
        if (totalPages > 1) {
            setupNavigation(totalPages);
        }

        // Añadir botones de utilidad
        setupUtilityButtons(category);
    }

    /**
     * Crea el item que representa un tag
     */
    private ItemStack createTagItem(Tag tag) {
        // Determinar el material basado en si el jugador tiene el tag
        boolean hasTag = PlayerDataManager.hasPlayerUnlockedTag(player.getUniqueId(), tag.getName());
        boolean hasPermission = tag.getPermission() == null || player.hasPermission(tag.getPermission());
        String currentPlayerTag = PlayerDataManager.getPlayerTag(player.getUniqueId());
        boolean isCurrentTag = tag.getDisplayTag().equals(currentPlayerTag);

        Material material;
        if (isCurrentTag) {
            material = Material.DIAMOND; // Tag actual
        } else if (hasTag) {
            material = getMaterialSafely(tag.getDisplayItem()); // Tag desbloqueado
        } else {
            material = Material.GRAY_DYE; // Tag bloqueado
        }

        // Crear lore del tag
        List<String> lore = new ArrayList<>();

        // Mostrar el tag
        lore.add("&7Vista previa:");
        lore.add(tag.getFormattedTag());
        lore.add("");

        // Descripción del tag
        if (tag.getDescription() != null && !tag.getDescription().isEmpty()) {
            lore.add("&7Descripción:");
            lore.add("&f" + tag.getDescription());
            lore.add("");
        }

        // Estado del tag
        if (isCurrentTag) {
            lore.add("&a✓ &lEste es tu tag actual");
        } else if (hasTag) {
            lore.add("&a✓ Tag desbloqueado");
            lore.add("&e▶ Click para equipar este tag");
        } else {
            lore.add("&c✗ Tag bloqueado");

            // Información de cómo obtenerlo
            if (!hasPermission) {
                lore.add("&c⚠ Necesitas el permiso: " + tag.getPermission());
            } else {
                if (tag.getCost() > 0) {
                    lore.add("&7Costo: &6" + tag.getCost() + " monedas");
                    lore.add("&e▶ Click para comprar");
                } else {
                    lore.add("&7Este tag es gratis");
                    lore.add("&e▶ Contacta con un administrador");
                }
            }
        }

        // Información adicional
        lore.add("");
        if (tag.getPermission() != null) {
            lore.add("&8Permiso: " + tag.getPermission());
        }

        return createItem(material, tag.getFormattedDisplayName(), lore);
    }

    /**
     * Configura la navegación entre páginas
     */
    private void setupNavigation(int totalPages) {
        // Botón página anterior
        if (currentPage > 1) {
            ItemStack previousItem = createNavigationItem(
                    Material.ARROW,
                    "&a◀ Página Anterior",
                    "previous",
                    currentPage,
                    totalPages
            );
            inventory.setItem(45, previousItem);
        }

        // Indicador de página actual
        List<String> pageLore = new ArrayList<>();
        pageLore.add("&7Página actual: &f" + currentPage + "&7/&f" + totalPages);
        pageLore.add("");
        pageLore.add("&7Total de tags en esta categoría: &f" + TagManager.getTagsByCategory(categoryName).size());

        ItemStack pageItem = createItem(Material.BOOK, "&6Página " + currentPage, pageLore);
        inventory.setItem(49, pageItem);

        // Botón página siguiente
        if (currentPage < totalPages) {
            ItemStack nextItem = createNavigationItem(
                    Material.ARROW,
                    "&aPágina Siguiente ▶",
                    "next",
                    currentPage,
                    totalPages
            );
            inventory.setItem(53, nextItem);
        }
    }

    /**
     * Configura los botones de utilidad del GUI
     */
    private void setupUtilityButtons(Category category) {
        // Botón volver al menú principal
        List<String> backLore = new ArrayList<>();
        backLore.add("&7Volver al menú principal");
        backLore.add("&7de categorías");

        ItemStack backItem = createItem(Material.BARRIER, "&c&l← Volver", backLore);
        inventory.setItem(47, backItem);

        // Botón de información de la categoría
        List<String> categoryLore = new ArrayList<>();
        categoryLore.add("&7Categoría: &f" + category.getName());
        categoryLore.add("");
        categoryLore.addAll(category.getLoreAsList());

        ItemStack categoryItem = createItem(
                getMaterialSafely(category.getMaterial()),
                "&6&lInfo de Categoría",
                categoryLore
        );
        inventory.setItem(46, categoryItem);

        // Botón para quitar tag actual (si tiene uno)
        String currentTag = PlayerDataManager.getPlayerTag(player.getUniqueId());
        if (!currentTag.equals(PlayerDataManager.getDefaultTag())) {
            List<String> removeLore = new ArrayList<>();
            removeLore.add("&7Tu tag actual:");
            removeLore.add(currentTag);
            removeLore.add("");
            removeLore.add("&e▶ Click para quitar tu tag");
            removeLore.add("&7(Volverás al tag por defecto)");

            ItemStack removeItem = createItem(Material.LAVA_BUCKET, "&c&lQuitar Tag", removeLore);
            inventory.setItem(51, removeItem);
        }

        // Estadísticas del jugador
        List<String> statsLore = new ArrayList<>();
        int totalUnlocked = PlayerDataManager.getPlayerTagCount(player.getUniqueId());
        int categoryUnlocked = PlayerDataManager.getPlayerTagCountByCategory(player.getUniqueId(), categoryName);
        int categoryTotal = TagManager.getTagsByCategory(categoryName).size();

        statsLore.add("&7Tus estadísticas:");
        statsLore.add("");
        statsLore.add("&7En esta categoría: &a" + categoryUnlocked + "&7/&f" + categoryTotal);
        statsLore.add("&7Total global: &a" + totalUnlocked + " tags");

        ItemStack statsItem = createItem(Material.ENCHANTED_BOOK, "&b&lTus Estadísticas", statsLore);
        inventory.setItem(52, statsItem);
    }

    /**
     * Configura el GUI cuando la categoría está vacía
     */
    private void setupEmptyGUI() {
        fillBorders();

        List<String> lore = new ArrayList<>();
        lore.add("&7Esta categoría no tiene tags disponibles");
        lore.add("&7Vuelve más tarde o contacta con un administrador");

        ItemStack emptyItem = createItem(Material.BARRIER, "&c&lCategoría Vacía", lore);
        inventory.setItem(22, emptyItem);

        // Botón volver
        List<String> backLore = new ArrayList<>();
        backLore.add("&7Volver al menú principal");

        ItemStack backItem = createItem(Material.ARROW, "&c&l← Volver", backLore);
        inventory.setItem(49, backItem);
    }

    /**
     * Configura el GUI cuando hay un error
     */
    private void setupErrorGUI(String errorMessage) {
        fillBorders();

        List<String> lore = new ArrayList<>();
        lore.add("&c" + errorMessage);
        lore.add("&7Contacta con un administrador");

        ItemStack errorItem = createItem(Material.BARRIER, "&c&lError", lore);
        inventory.setItem(22, errorItem);

        // Botón volver
        List<String> backLore = new ArrayList<>();
        backLore.add("&7Volver al menú principal");

        ItemStack backItem = createItem(Material.ARROW, "&c&l← Volver", backLore);
        inventory.setItem(49, backItem);
    }

    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Manejar navegación
        if (slot == 45 && currentPage > 1) {
            // Página anterior
            GUIManager.openCategoryGUI(player, categoryName, currentPage - 1);
            return;
        }

        if (slot == 53 && currentPage < calculateTotalPages(TagManager.getTagsByCategory(categoryName).size(), tagsPerPage)) {
            // Página siguiente
            GUIManager.openCategoryGUI(player, categoryName, currentPage + 1);
            return;
        }

        // Botón volver
        if (slot == 47) {
            GUIManager.openTagsGUI(player);
            return;
        }

        // Botón quitar tag
        if (slot == 51) {
            handleRemoveTag();
            return;
        }

        // Botones de información (no hacer nada)
        if (slot == 46 || slot == 49 || slot == 52) {
            return;
        }

        // Manejar clic en tags
        List<Integer> contentSlots = getContentSlots();
        if (!contentSlots.contains(slot)) {
            return; // Clic en slot no válido
        }

        // Obtener el tag correspondiente al slot
        Tag clickedTag = getTagFromSlot(slot);
        if (clickedTag != null) {
            handleTagClick(clickedTag);
        }
    }

    /**
     * Obtiene el tag correspondiente a un slot específico
     */
    private Tag getTagFromSlot(int slot) {
        List<Integer> contentSlots = getContentSlots();
        int tagIndex = contentSlots.indexOf(slot);

        if (tagIndex == -1) return null;

        List<Tag> categoryTags = TagManager.getTagsByCategory(categoryName);
        int startIndex = (currentPage - 1) * tagsPerPage;
        int actualIndex = startIndex + tagIndex;

        if (actualIndex >= 0 && actualIndex < categoryTags.size()) {
            return categoryTags.get(actualIndex);
        }

        return null;
    }

    /**
     * Maneja el clic en un tag específico
     */
    private void handleTagClick(Tag tag) {
        boolean hasTag = PlayerDataManager.hasPlayerUnlockedTag(player.getUniqueId(), tag.getName());
        boolean hasPermission = tag.getPermission() == null || player.hasPermission(tag.getPermission());
        String currentPlayerTag = PlayerDataManager.getPlayerTag(player.getUniqueId());
        boolean isCurrentTag = tag.getDisplayTag().equals(currentPlayerTag);

        // Si ya es el tag actual, no hacer nada
        if (isCurrentTag) {
            player.sendMessage("§e¡Ya tienes este tag equipado!");
            return;
        }

        // Si tiene el tag, equiparlo
        if (hasTag) {
            if (PlayerDataManager.setPlayerTag(player.getUniqueId(), tag.getName())) {
                player.sendMessage("§a¡Tag equipado exitosamente! " + tag.getFormattedTag());
                refresh(); // Actualizar el GUI para mostrar el cambio
            } else {
                player.sendMessage("§cError al equipar el tag. Inténtalo de nuevo.");
            }
            return;
        }

        // Si no tiene permisos
        if (!hasPermission) {
            player.sendMessage("§c¡No tienes permisos para usar este tag!");
            player.sendMessage("§7Permiso requerido: §f" + tag.getPermission());
            return;
        }

        // Si el tag cuesta dinero (implementar compra en el futuro)
        if (tag.getCost() > 0) {
            player.sendMessage("§e¡Este tag cuesta §6" + tag.getCost() + " monedas§e!");
            player.sendMessage("§7Sistema de compra en desarrollo...");
            return;
        }

        // Si el tag es gratis pero no lo tiene (probablemente necesita un admin)
        player.sendMessage("§e¡Este tag debe ser otorgado por un administrador!");
        player.sendMessage("§7Usa §f/grvtags give " + player.getName() + " " + tag.getName());
    }

    /**
     * Maneja la remoción del tag actual del jugador
     */
    private void handleRemoveTag() {
        String currentTag = PlayerDataManager.getPlayerTag(player.getUniqueId());

        if (currentTag.equals(PlayerDataManager.getDefaultTag())) {
            player.sendMessage("§e¡Ya tienes el tag por defecto!");
            return;
        }

        if (PlayerDataManager.setPlayerTag(player.getUniqueId(), null)) {
            player.sendMessage("§a¡Tag removido! Ahora usas el tag por defecto.");
            refresh(); // Actualizar el GUI
        } else {
            player.sendMessage("§cError al remover el tag. Inténtalo de nuevo.");
        }
    }

    // Getters para acceso externo si es necesario
    public String getCategoryName() {
        return categoryName;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}