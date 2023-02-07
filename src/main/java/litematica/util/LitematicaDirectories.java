package litematica.util;

import java.nio.file.Path;

import malilib.config.option.BooleanAndFileConfig.BooleanAndFile;
import malilib.config.util.ConfigUtils;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileUtils;
import malilib.util.StringUtils;
import litematica.Reference;
import litematica.config.Configs;

public class LitematicaDirectories
{
    public static void createDirectoriesIfMissingOrPrintError(Path dir)
    {
        if (FileUtils.createDirectoriesIfMissing(dir) == false)
        {
            String key = "litematica.message.error.failed_to_create_directory";
            MessageDispatcher.error().console().translate(key, dir.toAbsolutePath());
        }
    }

    public static Path getDataDirectory(String dirName)
    {
        return FileUtils.getMinecraftDirectory().resolve("litematica").resolve(dirName);
    }

    protected static Path getPerWorldDataDirectory(String dirName)
    {
        String worldName = StringUtils.getWorldOrServerNameOrDefault("__fallback");
        Path dir = getDataDirectory(dirName).resolve(worldName);
        return dir;
    }

    public static Path getPerWorldDataBaseDirectory()
    {
        return LitematicaDirectories.getDataDirectory("world_specific_data");
    }

    public static Path getAreaSelectionsBaseDirectory()
    {
        String name = StringUtils.getWorldOrServerName();
        Path baseDir = getDataDirectory("area_selections");
        Path dir;

        if (Configs.Generic.AREAS_PER_WORLD.getBooleanValue() && name != null)
        {
            // The 'area_selections' sub-directory is to prevent showing the world name or server IP in the browser,
            // as the root directory name is shown in the navigation widget
            dir = baseDir.resolve("per_world").resolve(name);
        }
        else
        {
            dir = baseDir.resolve("global");
        }

        createDirectoriesIfMissingOrPrintError(dir);

        return dir;
    }

    public static Path getModConfigDirectory()
    {
        return ConfigUtils.getConfigDirectory().resolve(Reference.MOD_ID);
    }

    public static Path getMaterialListDirectory()
    {
        return getDataDirectory("material_list");
    }

    public static Path getPlacementSaveFilesDirectory()
    {
        Path dir = getPerWorldDataDirectory("placements");
        createDirectoriesIfMissingOrPrintError(dir);
        return dir;
    }

    public static Path getDefaultSchematicDirectory()
    {
        return FileUtils.getMinecraftDirectory().resolve("schematics");
    }

    public static Path getSchematicsBaseDirectory()
    {
        BooleanAndFile value = Configs.Generic.CUSTOM_SCHEMATIC_DIRECTORY.getValue();
        boolean useCustom = value.booleanValue;
        Path dir = null;

        if (useCustom)
        {
            dir = value.fileValue;
        }

        if (useCustom == false || dir == null)
        {
            dir = getDefaultSchematicDirectory();
        }

        createDirectoriesIfMissingOrPrintError(dir);

        return dir;
    }

    public static Path getVCSProjectsBaseDirectory()
    {
        Path dir = getSchematicsBaseDirectory().resolve("VCS");
        createDirectoriesIfMissingOrPrintError(dir);
        return dir;
    }
}
