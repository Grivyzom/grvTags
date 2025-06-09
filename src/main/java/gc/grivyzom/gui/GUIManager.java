package gc.grivyzom.gui;

import gc.grivyzom.grvTags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manager principal para el sistema de GUIs
 * Maneja el registro, eventos y ciclo de vida de todos los GUIs
 */
public class GUIManager implements Listener {

    private static final Map<Player, BaseGUI> activeGUIs = new ConcurrentHashMap<>();
    private static grvTags plugin;

    /**
     * Inicializa el sistema de GUIs
     */
    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
        plugin.getServer().getPluginManager().registerEvents(new GUIManager(), plugin);
        plugin.getLogger().info("Sistema de GUIs inicializado correctamente");
    }

    /**
     * Registra un GUI activo para un jugador
     */
    public static void registerActiveGUI(Player player, BaseGUI gui) {
        activeGUIs.put(player, gui);
    }

    /**
     * Desregistra el GUI activo de un jugador
     */
    public static void unregisterActiveGUI(Player player) {
        activeGUIs.remove(player);
    }

    /**
     * Obtiene el GUI activo de un jugador
     */
    public static BaseGUI getActiveGUI(Player player) {
        return activeGUIs.get(player);
    }

    /**
     * Verifica si un jugador tiene un GUI activo
     */
    public static boolean hasActiveGUI(Player player) {
        return activeGUIs.containsKey(player);
    }

    /**
     * Abre el GUI principal de tags para un jugador
     */
    public static void openTagsGUI(Player player) {
        try {
            MainTagsGUI gui = new MainTagsGUI(plugin, player);
            gui.open();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al abrir GUI de tags para " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cError al abrir el menú de tags. Contacta con un administrador.");
        }
    }

    /**
     * Abre el GUI de una categoría específica
     */
    public static void openCategoryGUI(Player player, String categoryName) {
        openCategoryGUI(player, categoryName, 1);
    }

    /**
     * Abre el GUI de una categoría específica con página
     */
    public static void openCategoryGUI(Player player, String categoryName, int page) {
        try {
            CategoryTagsGUI gui = new CategoryTagsGUI(plugin, player, categoryName, page);
            gui.open();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al abrir GUI de categoría " + categoryName + " para " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cError al abrir el menú de la categoría. Contacta con un administrador.");
        }
    }

    /**
     * Cierra todos los GUIs activos (útil para recargas)
     */
    public static void closeAllGUIs() {
        for (Map.Entry<Player, BaseGUI> entry : activeGUIs.entrySet()) {
            Player player = entry.getKey();
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        activeGUIs.clear();
        plugin.getLogger().info("Todos los GUIs cerrados (" + activeGUIs.size() + " activos)");
    }

    /**
     * Obtiene estadísticas del sistema de GUIs
     */
    public static String getGUIStats() {
        return "GUIs activos: " + activeGUIs.size();
    }

    // =================== EVENT HANDLERS ===================

    /**
     * Maneja los clics en inventarios de GUIs
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        BaseGUI activeGUI = activeGUIs.get(player);

        if (activeGUI == null) return;

        // Verificar que el inventario clickeado es el del GUI activo
        if (!event.getInventory().equals(activeGUI.getInventory())) return;

        // Cancelar el evento para evitar que el jugador tome items
        event.setCancelled(true);

        try {
            // Manejar el clic en el GUI
            activeGUI.handleClick(
                    event.getSlot(),
                    event.getCurrentItem(),
                    event.isShiftClick()
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Error al manejar clic en GUI para " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cError al procesar la acción. Inténtalo de nuevo.");
        }
    }

    /**
     * Maneja cuando un jugador cierra un inventario de GUI
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        BaseGUI activeGUI = activeGUIs.get(player);

        if (activeGUI == null) return;

        // Verificar que el inventario cerrado es el del GUI activo
        if (!event.getInventory().equals(activeGUI.getInventory())) return;

        // Desregistrar el GUI con un pequeño delay para permitir navegación entre GUIs
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Solo desregistrar si el jugador no abrió otro GUI inmediatamente
            if (activeGUIs.get(player) == activeGUI) {
                unregisterActiveGUI(player);
            }
        }, 1L);
    }

    /**
     * Limpia los GUIs cuando un jugador se desconecta
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        unregisterActiveGUI(player);
    }
}