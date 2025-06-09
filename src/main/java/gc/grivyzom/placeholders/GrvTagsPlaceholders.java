package gc.grivyzom.placeholders;

import gc.grivyzom.grvTags;
import gc.grivyzom.managers.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI Hook para grvTags
 */
public class GrvTagsPlaceholders extends PlaceholderExpansion {

    private final grvTags plugin;

    public GrvTagsPlaceholders(grvTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "grvtags";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Esto significa que el expansion no se desregistrará cuando PlaceholderAPI se recargue
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %grvtags_tag% - Obtiene el tag actual del jugador
        if (params.equals("tag")) {
            String tag = PlayerDataManager.getPlayerTag(player.getName());
            return ChatColor.translateAlternateColorCodes('&', tag);
        }

        // %grvtags_tags% - Obtiene el número total de tags desbloqueados
        if (params.equals("tags")) {
            int tagCount = PlayerDataManager.getPlayerTagCount(player.getName());
            return String.valueOf(tagCount);
        }

        // %grvtags_tags_<category>% - Obtiene el número de tags desbloqueados por categoría
        if (params.startsWith("tags_")) {
            String categoryName = params.substring(5); // Remover "tags_" del inicio
            int tagCount = PlayerDataManager.getPlayerTagCountByCategory(player.getName(), categoryName);
            return String.valueOf(tagCount);
        }

        // Si no coincide con ningún placeholder, devolver null
        return null;
    }

    /**
     * Método para manejar el registro del expansion con acciones adicionales.
     */
    public void registerExpansion() {
        if (canRegister()) {
            if (register()) {
                plugin.getLogger().info("PlaceholderAPI hook registrado exitosamente");
                plugin.getLogger().info("Placeholders disponibles:");
                plugin.getLogger().info("- %grvtags_tag% - Tag actual del jugador");
                plugin.getLogger().info("- %grvtags_tags% - Número total de tags desbloqueados");
                plugin.getLogger().info("- %grvtags_tags_<category>% - Tags desbloqueados por categoría");
            } else {
                plugin.getLogger().warning("Error al registrar PlaceholderAPI hook");
            }
        } else {
            plugin.getLogger().warning("No se puede registrar PlaceholderAPI hook - PlaceholderAPI no encontrado");
        }
    }
}