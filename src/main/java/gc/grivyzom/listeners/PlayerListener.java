package gc.grivyzom.listeners;

import gc.grivyzom.grvTags;
import gc.grivyzom.managers.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos de jugadores
 */
public class PlayerListener implements Listener {

    private final grvTags plugin;

    public PlayerListener(grvTags plugin) {
        this.plugin = plugin;
    }

    /**
     * Maneja cuando un jugador se conecta al servidor
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Ejecutar de forma asíncrona para no bloquear el hilo principal
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Crear datos del jugador si no existen
                PlayerDataManager.createPlayerDataIfNotExists(event.getPlayer());

                // Actualizar nombre del jugador por si cambió
                PlayerDataManager.updatePlayerName(event.getPlayer());

                plugin.getLogger().info("Datos de jugador " + event.getPlayer().getName() + " verificados/actualizados");

            } catch (Exception e) {
                plugin.getLogger().severe("Error al procesar conexión de jugador " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Maneja cuando un jugador se desconecta del servidor
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Aquí podrías agregar lógica adicional si es necesaria
        // Por ejemplo, limpiar caches en memoria del jugador
        plugin.getLogger().fine("Jugador " + event.getPlayer().getName() + " se desconectó");
    }
}