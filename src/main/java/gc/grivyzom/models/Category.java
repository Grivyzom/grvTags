package gc.grivyzom.models;

import java.util.Arrays;
import java.util.List;

/**
 * Modelo que representa una Categoría en el sistema
 */
public class Category {

    private int id;
    private String name;
    private String title;
    private String material;
    private String displayName;
    private int slotPosition;
    private String lore;
    private String permission;
    private boolean permissionSeeCategory;

    /**
     * Constructor completo
     */
    public Category(int id, String name, String title, String material, String displayName,
                    int slotPosition, String lore, String permission, boolean permissionSeeCategory) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.material = material;
        this.displayName = displayName;
        this.slotPosition = slotPosition;
        this.lore = lore;
        this.permission = permission;
        this.permissionSeeCategory = permissionSeeCategory;
    }

    /**
     * Constructor simplificado para crear nuevas categorías
     */
    public Category(String name, String displayName) {
        this.name = name;
        this.displayName = "&7&l" + displayName + " Tags";
        this.title = "&8" + displayName + " Tags (Page %page%)";
        this.material = "NAME_TAG";
        this.slotPosition = 11;
        this.lore = "&8&m-----------------------------||&7" + displayName + " tags: &7%tags_amount%||&8&m-----------------------------";
        this.permission = "grvtags.category." + name.toLowerCase();
        this.permissionSeeCategory = false;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlotPosition() {
        return slotPosition;
    }

    public String getLore() {
        return lore;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPermissionSeeCategory() {
        return permissionSeeCategory;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setSlotPosition(int slotPosition) {
        this.slotPosition = slotPosition;
    }

    public void setLore(String lore) {
        this.lore = lore;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setPermissionSeeCategory(boolean permissionSeeCategory) {
        this.permissionSeeCategory = permissionSeeCategory;
    }

    /**
     * Obtiene el título formateado con colores
     */
    public String getFormattedTitle(int currentPage) {
        if (title == null) return "&8" + name + " (Page " + currentPage + ")";
        return title.replace("%page%", String.valueOf(currentPage)).replaceAll("&", "§");
    }

    /**
     * Obtiene el nombre de display formateado
     */
    public String getFormattedDisplayName() {
        if (displayName == null) return "&7&l" + name;
        return displayName.replaceAll("&", "§");
    }

    /**
     * Obtiene la descripción (lore) como lista
     */
    public List<String> getLoreAsList() {
        if (lore == null || lore.isEmpty()) {
            return Arrays.asList("&7Categoría de tags", "&8Sin descripción");
        }

        String[] lines = lore.split("\\|\\|");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replaceAll("&", "§");
        }

        return Arrays.asList(lines);
    }

    /**
     * Obtiene la descripción formateada con el número de tags
     */
    public List<String> getFormattedLore(int tagCount) {
        List<String> formattedLore = getLoreAsList();

        // Reemplazar placeholder %tags_amount%
        for (int i = 0; i < formattedLore.size(); i++) {
            String line = formattedLore.get(i);
            line = line.replace("%tags_amount%", String.valueOf(tagCount));
            formattedLore.set(i, line);
        }

        return formattedLore;
    }

    /**
     * Verifica si el material es válido
     */
    public boolean isValidMaterial() {
        if (material == null) return false;

        // Lista de materiales válidos comunes
        String[] validMaterials = {
                "NAME_TAG", "PAPER", "BOOK", "DIAMOND", "EMERALD", "GOLD_INGOT",
                "IRON_INGOT", "REDSTONE", "CHEST", "ENDER_CHEST", "BEACON",
                "NETHER_STAR", "BARRIER", "BEDROCK", "GRASS_BLOCK", "STONE"
        };

        for (String validMaterial : validMaterials) {
            if (validMaterial.equals(material.toUpperCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Clona la categoría
     */
    public Category clone() {
        return new Category(id, name, title, material, displayName, slotPosition,
                lore, permission, permissionSeeCategory);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", slotPosition=" + slotPosition +
                ", permission='" + permission + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return name != null ? name.equalsIgnoreCase(category.name) : category.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0;
    }
}