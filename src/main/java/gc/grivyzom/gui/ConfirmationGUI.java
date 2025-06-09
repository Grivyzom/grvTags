package gc.grivyzom.gui;

import gc.grivyzom.grvTags;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI de confirmación para acciones importantes
 * Permite confirmar o cancelar una acción específica
 */
public class ConfirmationGUI extends BaseGUI {

    private final String actionDescription;
    private final Consumer<Player> confirmAction;
    private final Consumer<Player> cancelAction;
    private final Material iconMaterial;

    public ConfirmationGUI(grvTags plugin, Player player, String actionDescription,
                           Consumer<Player> confirmAction, Consumer<Player> cancelAction) {
        this(plugin, player, actionDescription, confirmAction, cancelAction, Material.EMERALD);
    }

    public ConfirmationGUI(grvTags plugin, Player player, String actionDescription,
                           Consumer<Player> confirmAction, Consumer<Player> cancelAction,
                           Material iconMaterial) {
        super(plugin, player, "&c&lConfirmar Acción", 27);
        this.actionDescription = actionDescription;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
        this.iconMaterial = iconMaterial;
    }

    @Override
    public void setupGUI() {
        // Llenar bordes
        fillBorders();

        // Item de descripción de la acción (centro)
        List<String> actionLore = new ArrayList<>();
        actionLore.add("&7¿Estás seguro de que quieres");
        actionLore.add("&7realizar esta acción?");
        actionLore.add("");
        actionLore.add("&e" + actionDescription);
        actionLore.add("");
        actionLore.add("&c⚠ Esta acción no se puede deshacer");

        ItemStack actionItem = createItem(iconMaterial, "&6&lAcción a Realizar", actionLore);
        inventory.setItem(13, actionItem);

        // Botón de confirmación (izquierda)
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("&7Confirmar la acción");
        confirmLore.add("");
        confirmLore.add("&a▶ Click para confirmar");

        ItemStack confirmItem = createItem(Material.GREEN_WOOL, "&a&l✓ CONFIRMAR", confirmLore);
        inventory.setItem(11, confirmItem);

        // Botón de cancelación (derecha)
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("&7Cancelar la acción");
        cancelLore.add("");
        cancelLore.add("&c▶ Click para cancelar");

        ItemStack cancelItem = createItem(Material.RED_WOOL, "&c&l✗ CANCELAR", cancelLore);
        inventory.setItem(15, cancelItem);

        // Items decorativos adicionales
        ItemStack warningItem = createItem(Material.YELLOW_WOOL, "&e&l⚠ ADVERTENCIA",
                List.of("&7Esta es una acción importante", "&7Asegúrate antes de confirmar"));
        inventory.setItem(4, warningItem);
    }

    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        switch (slot) {
            case 11: // Botón confirmar
                if (confirmAction != null) {
                    try {
                        confirmAction.accept(player);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error al ejecutar acción de confirmación: " + e.getMessage());
                        player.sendMessage("§cError al ejecutar la acción. Contacta con un administrador.");
                    }
                }
                close();
                break;

            case 15: // Botón cancelar
                if (cancelAction != null) {
                    try {
                        cancelAction.accept(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error al ejecutar acción de cancelación: " + e.getMessage());
                    }
                }
                close();
                break;

            default:
                // Otros slots no hacen nada
                break;
        }
    }

    /**
     * Método estático para crear rápidamente un GUI de confirmación simple
     */
    public static void showConfirmation(grvTags plugin, Player player, String description,
                                        Runnable confirmAction, Runnable cancelAction) {
        Consumer<Player> confirmConsumer = confirmAction != null ? p -> confirmAction.run() : null;
        Consumer<Player> cancelConsumer = cancelAction != null ? p -> cancelAction.run() : null;

        ConfirmationGUI gui = new ConfirmationGUI(plugin, player, description, confirmConsumer, cancelConsumer);
        gui.open();
    }
}