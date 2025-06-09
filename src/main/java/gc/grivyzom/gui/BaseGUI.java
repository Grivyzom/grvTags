package gc.grivyzom.gui;

import gc.grivyzom.grvTags;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Clase base para el manejo de GUIs
 * Proporciona métodos comunes y utilidades para todos los GUIs
 */
public abstract class BaseGUI {

    protected final grvTags plugin;
    protected final Player player;
    protected final String title;
    protected final int size;
    protected Inventory inventory;

    public BaseGUI(grvTags plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = colorize(title);
        this.size = size;
        this.inventory = Bukkit.createInventory(null, size, this.title);
    }

    /**
     * Método abstracto que cada GUI debe implementar para configurar sus elementos
     */
    public abstract void setupGUI();

    /**
     * Abre el GUI para el jugador
     */
    public void open() {
        setupGUI();
        player.openInventory(inventory);

        // Registrar el GUI en el manager
        GUIManager.registerActiveGUI(player, this);
    }

    /**
     * Actualiza el contenido del GUI
     */
    public void refresh() {
        inventory.clear();
        setupGUI();
    }

    /**
     * Cierra el GUI para el jugador
     */
    public void close() {
        player.closeInventory();
        GUIManager.unregisterActiveGUI(player);
    }

    /**
     * Maneja los clics en el GUI
     */
    public abstract void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick);

    /**
     * Crea un item de decoración/relleno
     */
    protected ItemStack createFillerItem() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    /**
     * Crea un item personalizado
     */
    protected ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(colorize(displayName));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Crea un item de navegación (anterior/siguiente página)
     */
    protected ItemStack createNavigationItem(Material material, String displayName, String direction, int currentPage, int totalPages) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Página actual: &f" + currentPage + "&7/&f" + totalPages);
        lore.add("");

        if (direction.equals("previous") && currentPage > 1) {
            lore.add("&a▶ Click para ir a la página anterior");
        } else if (direction.equals("next") && currentPage < totalPages) {
            lore.add("&a▶ Click para ir a la página siguiente");
        } else {
            lore.add("&c✗ No hay más páginas en esta dirección");
        }

        return createItem(material, displayName, lore);
    }

    /**
     * Rellena los bordes del GUI con items de decoración
     */
    protected void fillBorders() {
        ItemStack filler = createFillerItem();

        // Llenar primera y última fila
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(size - 9 + i, filler);
        }

        // Llenar bordes laterales
        for (int i = 9; i < size - 9; i += 9) {
            inventory.setItem(i, filler);
            inventory.setItem(i + 8, filler);
        }
    }

    /**
     * Rellena slots específicos con items de decoración
     */
    protected void fillSlots(int... slots) {
        ItemStack filler = createFillerItem();
        for (int slot : slots) {
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, filler);
            }
        }
    }

    /**
     * Convierte códigos de color
     */
    protected String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Obtiene un material desde una string de forma segura
     */
    protected Material getMaterialSafely(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.NAME_TAG;
        }

        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return material;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material inválido: " + materialName + ". Usando NAME_TAG por defecto.");
            return Material.NAME_TAG;
        }
    }

    /**
     * Verifica si un slot está en los bordes del GUI
     */
    protected boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        int maxRow = (size / 9) - 1;

        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    /**
     * Calcula el número total de páginas necesarias
     */
    protected int calculateTotalPages(int totalItems, int itemsPerPage) {
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }

    /**
     * Obtiene los slots disponibles para contenido (excluyendo navegación y decoración)
     */
    protected List<Integer> getContentSlots() {
        List<Integer> contentSlots = new ArrayList<>();

        // Para un GUI de 6 filas (54 slots), los slots de contenido son:
        // Fila 2-5, columnas 2-8 (excluyendo bordes)
        for (int row = 1; row < (size / 9) - 1; row++) {
            for (int col = 1; col < 8; col++) {
                contentSlots.add(row * 9 + col);
            }
        }

        return contentSlots;
    }

    /**
     * Crea un item de información/estado
     */
    protected ItemStack createInfoItem() {
        List<String> lore = Arrays.asList(
                "&7Plugin: &fgrvTags v" + plugin.getDescription().getVersion(),
                "&7Jugador: &f" + player.getName(),
                "",
                "&8Desarrollado por Brocolitx"
        );

        return createItem(Material.BOOK, "&6&lInformación", lore);
    }

    // Getters
    public Player getPlayer() {
        return player;
    }

    public String getTitle() {
        return title;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getSize() {
        return size;
    }
}