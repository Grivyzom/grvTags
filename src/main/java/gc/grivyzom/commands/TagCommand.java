package gc.grivyzom.commands;

import gc.grivyzom.grvTags;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // Por ahora mostrar mensaje placeholder ya que el GUI no está implementado
        player.sendMessage(colorize(PREFIX + "&7Abriendo GUI de Tags..."));
        player.sendMessage(colorize(PREFIX + "&e⚠ &7GUI en desarrollo - próximamente disponible"));

        // TODO: Implementar apertura del GUI de tags
        // TagGUI.openTagsGUI(player);

        plugin.getLogger().info("Jugador " + player.getName() + " ejecutó el comando /tags");

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