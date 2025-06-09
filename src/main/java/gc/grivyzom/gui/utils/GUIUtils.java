package gc.grivyzom.gui.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Clase de utilidades para la creación y manejo de GUIs
 * Contiene métodos helper comunes para todos los GUIs
 */
public class GUIUtils {

    /**
     * Crea un separador visual para lores
     */
    public static String createSeparator(int length, char character) {
        StringBuilder separator = new StringBuilder("&8");
        for (int i = 0; i < length; i++) {
            separator.append(character);
        }
        return separator.toString();
    }

    /**
     * Crea una barra de progreso colorizada
     */
    public static String createProgressBar(double percentage, int length, char fillChar, char emptyChar) {
        int filled = (int) Math.round(percentage / 100.0 * length);
        StringBuilder bar = new StringBuilder("&8[");

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                if (percentage >= 75) {
                    bar.append("&a"); // Verde para alto progreso
                } else if (percentage >= 50) {
                    bar.append("&e"); // Amarillo para progreso medio
                } else if (percentage >= 25) {
                    bar.append("&6"); // Naranja para progreso bajo
                } else {
                    bar.append("&c"); // Rojo para muy bajo progreso
                }
                bar.append(fillChar);
            } else {
                bar.append("&7").append(emptyChar);
            }
        }

        bar.append("&8]");
        return bar.toString();
    }

    /**
     * Formatea números grandes con separadores
     */
    public static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }

    /**
     * Crea un lore con texto que se ajusta automáticamente
     */
    public static List<String> createWrappedLore(String text, int maxLineLength) {
        List<String> lore = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLineLength) {
                if (currentLine.length() > 0) {
                    lore.add("&7" + currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lore.add("&7" + currentLine.toString());
        }

        return lore;
    }

    /**
     * Crea un item de cabeza de jugador
     */
    public static ItemStack createPlayerHead(String playerName, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwner(playerName);

            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * Crea items de navegación estándar
     */
    public static class NavigationItems {

        public static ItemStack createBackButton() {
            return createItem(Material.ARROW, "&c&l← Volver",
                    Arrays.asList("&7Volver al menú anterior"));
        }

        public static ItemStack createHomeButton() {
            return createItem(Material.COMPASS, "&6&l⌂ Menú Principal",
                    Arrays.asList("&7Ir al menú principal"));
        }

        public static ItemStack createRefreshButton() {
            return createItem(Material.ECHO_SHARD, "&b&l↻ Actualizar",
                    Arrays.asList("&7Actualizar el contenido", "&7de este menú"));
        }

        public static ItemStack createCloseButton() {
            return createItem(Material.BARRIER, "&c&l✗ Cerrar",
                    Arrays.asList("&7Cerrar este menú"));
        }

        public static ItemStack createPreviousPageButton(int currentPage, int totalPages) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Página actual: &f" + currentPage + "&7/&f" + totalPages);
            lore.add("");

            if (currentPage > 1) {
                lore.add("&a▶ Click para ir a la página anterior");
            } else {
                lore.add("&c✗ Ya estás en la primera página");
            }

            Material material = currentPage > 1 ? Material.LIME_DYE : Material.GRAY_DYE;
            return createItem(material, "&a◀ Página Anterior", lore);
        }

        public static ItemStack createNextPageButton(int currentPage, int totalPages) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Página actual: &f" + currentPage + "&7/&f" + totalPages);
            lore.add("");

            if (currentPage < totalPages) {
                lore.add("&a▶ Click para ir a la página siguiente");
            } else {
                lore.add("&c✗ Ya estás en la última página");
            }

            Material material = currentPage < totalPages ? Material.LIME_DYE : Material.GRAY_DYE;
            return createItem(material, "&aRáfica Siguiente ▶", lore);
        }

        public static ItemStack createPageIndicator(int currentPage, int totalPages) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Página actual: &f" + currentPage + "&7/&f" + totalPages);
            lore.add("");
            lore.add("&7Usa las flechas para navegar");
            lore.add("&7entre las páginas disponibles");

            return createItem(Material.BOOK, "&6Página " + currentPage, lore);
        }
    }

    /**
     * Crea items decorativos comunes
     */
    public static class DecorativeItems {

        public static ItemStack createFillerGlass(Material glassType) {
            ItemStack glass = new ItemStack(glassType);
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                glass.setItemMeta(meta);
            }
            return glass;
        }

        public static ItemStack createSeparator() {
            return createItem(Material.IRON_BARS, "&8&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    Arrays.asList("&7Separador decorativo"));
        }

        public static ItemStack createTitle(String title) {
            return createItem(Material.NAME_TAG, "&6&l" + title,
                    Arrays.asList("&7Título de sección"));
        }

        public static ItemStack createInfoPanel(String title, List<String> information) {
            return createItem(Material.BOOK, "&b&l" + title, information);
        }
    }

    /**
     * Crea items de estado
     */
    public static class StatusItems {

        public static ItemStack createEnabledStatus(String feature) {
            return createItem(Material.LIME_DYE, "&a&l✓ " + feature,
                    Arrays.asList("&7Estado: &aHabilitado", "&7Esta función está activa"));
        }

        public static ItemStack createDisabledStatus(String feature) {
            return createItem(Material.RED_DYE, "&c&l✗ " + feature,
                    Arrays.asList("&7Estado: &cDeshabilitado", "&7Esta función está inactiva"));
        }

        public static ItemStack createLoadingStatus(String feature) {
            return createItem(Material.YELLOW_DYE, "&e&l⟳ " + feature,
                    Arrays.asList("&7Estado: &eCargando...", "&7Espera un momento"));
        }

        public static ItemStack createErrorStatus(String feature, String error) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Estado: &cError");
            lore.add("");
            lore.add("&cError: " + error);

            return createItem(Material.BARRIER, "&c&l⚠ " + feature, lore);
        }
    }

    /**
     * Método helper para crear items básicos
     */
    private static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Calcula la posición en una grilla
     */
    public static class GridUtils {

        /**
         * Convierte coordenadas de fila/columna a slot
         */
        public static int getSlot(int row, int col) {
            return row * 9 + col;
        }

        /**
         * Obtiene la fila de un slot
         */
        public static int getRow(int slot) {
            return slot / 9;
        }

        /**
         * Obtiene la columna de un slot
         */
        public static int getColumn(int slot) {
            return slot % 9;
        }

        /**
         * Verifica si un slot está en el borde
         */
        public static boolean isBorder(int slot, int inventorySize) {
            int row = getRow(slot);
            int col = getColumn(slot);
            int maxRow = (inventorySize / 9) - 1;

            return row == 0 || row == maxRow || col == 0 || col == 8;
        }

        /**
         * Obtiene los slots de contenido (excluyendo bordes)
         */
        public static List<Integer> getContentSlots(int inventorySize) {
            List<Integer> contentSlots = new ArrayList<>();
            int rows = inventorySize / 9;

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < 8; col++) {
                    contentSlots.add(getSlot(row, col));
                }
            }

            return contentSlots;
        }

        /**
         * Obtiene los slots de una fila específica
         */
        public static List<Integer> getRowSlots(int row) {
            List<Integer> rowSlots = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                rowSlots.add(getSlot(row, col));
            }
            return rowSlots;
        }

        /**
         * Obtiene los slots de una columna específica
         */
        public static List<Integer> getColumnSlots(int col, int inventorySize) {
            List<Integer> columnSlots = new ArrayList<>();
            int rows = inventorySize / 9;

            for (int row = 0; row < rows; row++) {
                columnSlots.add(getSlot(row, col));
            }
            return columnSlots;
        }
    }
}