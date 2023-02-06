package litematica.util;

import java.nio.file.Path;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileUtils;
import malilib.util.StringUtils;

public class LitematicaDirectories
{
    protected static void createDirectoriesIfMissingOrPrintError(Path dir)
    {
        if (FileUtils.createDirectoriesIfMissing(dir) == false)
        {
            String key = "litematica.message.error.schematic_placement.failed_to_create_directory";
            MessageDispatcher.error().translate(key, dir.toAbsolutePath().toString());
        }
    }

    protected static Path getDataDirectory(String dirName)
    {
        return FileUtils.getMinecraftDirectory().resolve("litematica").resolve(dirName);
    }

    protected static Path getPerWorldDataDirectory(String dirName)
    {
        String worldName = StringUtils.getWorldOrServerNameOrDefault("__fallback");
        Path dir = getDataDirectory(dirName).resolve(worldName);
        return dir;
    }

    public static Path getPlacementSaveFilesDirectory()
    {
        Path dir = getPerWorldDataDirectory("placements");
        createDirectoriesIfMissingOrPrintError(dir);
        return dir;
    }
}
