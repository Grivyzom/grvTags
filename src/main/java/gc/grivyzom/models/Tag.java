package gc.grivyzom.models;

/**
 * Modelo que representa un Tag en el sistema
 */
public class Tag {

    private int id;
    private String name;
    private String displayTag;
    private String permission;
    private String description;
    private String category;
    private int displayOrder;
    private String displayName;
    private String displayItem;
    private int cost;

    /**
     * Constructor completo
     */
    public Tag(int id, String name, String displayTag, String permission, String description,
               String category, int displayOrder, String displayName, String displayItem, int cost) {
        this.id = id;
        this.name = name;
        this.displayTag = displayTag;
        this.permission = permission;
        this.description = description;
        this.category = category;
        this.displayOrder = displayOrder;
        this.displayName = displayName;
        this.displayItem = displayItem;
        this.cost = cost;
    }

    /**
     * Constructor simplificado para crear nuevos tags
     */
    public Tag(String name, String displayTag, String category) {
        this.name = name;
        this.displayTag = displayTag;
        this.category = category;
        this.permission = "grvtags.tag." + name.toLowerCase();
        this.description = "Tag creado automáticamente";
        this.displayOrder = 1;
        this.displayName = "&7Tag: %tag%";
        this.displayItem = "NAME_TAG";
        this.cost = 0;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayTag() {
        return displayTag;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayItem() {
        return displayItem;
    }

    public int getCost() {
        return cost;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayTag(String displayTag) {
        this.displayTag = displayTag;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplayItem(String displayItem) {
        this.displayItem = displayItem;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    /**
     * Obtiene el tag formateado con colores
     */
    public String getFormattedTag() {
        if (displayTag == null) return "&8[&7" + name + "&8]";
        return displayTag.replaceAll("&", "§");
    }

    /**
     * Obtiene el nombre de display formateado
     */
    public String getFormattedDisplayName() {
        if (displayName == null) return "&7Tag: " + getFormattedTag();
        return displayName.replace("%tag%", getFormattedTag()).replaceAll("&", "§");
    }

    /**
     * Verifica si el tag es gratuito
     */
    public boolean isFree() {
        return cost <= 0;
    }

    /**
     * Clona el tag
     */
    public Tag clone() {
        return new Tag(id, name, displayTag, permission, description, category,
                displayOrder, displayName, displayItem, cost);
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayTag='" + displayTag + '\'' +
                ", category='" + category + '\'' +
                ", cost=" + cost +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name != null ? name.equalsIgnoreCase(tag.name) : tag.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0;
    }
}