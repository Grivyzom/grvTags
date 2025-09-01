package gc.grivyzom.utils;

import gc.grivyzom.grvTags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Utilidad para crear respaldos de archivos YAML antes de modificaciones importantes
 */
public class YamlBackupUtil {

    private static grvTags plugin;

    public static void initialize(grvTags pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Crea un respaldo de categories.yml antes de una eliminación
     */
    public static boolean backupCategoriesYaml() {
        return createBackup("categories.yml", "categories_backup");
    }

    /**
     * Crea un respaldo de tags.yml antes de una eliminación
     */
    public static boolean backupTagsYaml() {
        return createBackup("tags.yml", "tags_backup");
    }

    /**
     * Crea un respaldo de ambos archivos YAML
     */
    public static boolean backupAllYamlFiles() {
        boolean categoriesBackup = backupCategoriesYaml();
        boolean tagsBackup = backupTagsYaml();

        return categoriesBackup && tagsBackup;
    }

    /**
     * Crea un respaldo de un archivo específico
     */
    private static boolean createBackup(String fileName, String backupPrefix) {
        try {
            File originalFile = new File(plugin.getDataFolder(), fileName);
            if (!originalFile.exists()) {
                plugin.getLogger().warning("Archivo " + fileName + " no existe, no se puede crear respaldo");
                return false;
            }

            // Crear directorio de respaldos si no existe
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // Generar nombre del respaldo con timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String backupFileName = backupPrefix + "_" + timestamp + ".yml";
            File backupFile = new File(backupDir, backupFileName);

            // Copiar archivo
            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().info("Respaldo creado: " + backupFileName);
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear respaldo de " + fileName + ":", e);
            return false;
        }
    }

    /**
     * Limpia respaldos antiguos (mantiene solo los últimos 5)
     */
    public static void cleanupOldBackups() {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) return;

            File[] backupFiles = backupDir.listFiles((dir, name) ->
                    name.startsWith("categories_backup_") || name.startsWith("tags_backup_"));

            if (backupFiles == null || backupFiles.length <= 10) return; // Mantener 5 de cada tipo

            // Ordenar por fecha de modificación (más reciente primero)
            java.util.Arrays.sort(backupFiles, (f1, f2) ->
                    Long.compare(f2.lastModified(), f1.lastModified()));

            // Eliminar los más antiguos (mantener solo los primeros 10)
            for (int i = 10; i < backupFiles.length; i++) {
                if (backupFiles[i].delete()) {
                    plugin.getLogger().fine("Respaldo antiguo eliminado: " + backupFiles[i].getName());
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al limpiar respaldos antiguos:", e);
        }
    }
}