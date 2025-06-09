package gc.grivyzom.commands;

import gc.grivyzom.gui.GUIManager;
import gc.grivyzom.grvTags;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando principal para que los jugadores accedan al sistema de tags
 * Actualizado para usar el nuevo sistema de GUIs
 */
public class TagCommand implements CommandExecutor {

    private final grvTags plugin;
    private final String PREFIX = "&8[&6grvTags&8] ";

    public TagCommand(grvTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar que el comando lo ejecute un jugador
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize(PREFIX + "&cEste comando solo puede ser ejecutado por un jugador."));
            return true;
        }

        Player player = (Player) sender;

        // Verificar permisos básicos (opcional)
        if (!player.hasPermission("grvtags.use") && !player.hasPermission("grvtags.*")) {
            player.sendMessage(colorize(PREFIX + "&cNo tienes permisos para usar este comando."));
            plugin.getLogger().info("Jugador " + player.getName() + " intentó usar /tags sin permisos");
            return true;
        }

        try {
            // Abrir el GUI principal de tags usando el manager
            GUIManager.openTagsGUI(player);

            plugin.getLogger().fine("Jugador " + player.getName() + " abrió el GUI de tags");

        } catch (Exception e) {
            // Manejar errores de forma segura
            player.sendMessage(colorize(PREFIX + "&cError al abrir el menú de tags."));
            player.sendMessage(colorize(PREFIX + "&7Si el problema persiste, contacta con un administrador."));

            plugin.getLogger().severe("Error al abrir GUI de tags para " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Convierte códigos de color al formato de Minecraft
     * @param message Mensaje con códigos de color
     * @return Mensaje formateado
     */
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}