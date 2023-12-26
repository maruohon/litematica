package litematica.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileUtils;
import litematica.Litematica;
import litematica.util.LitematicaDirectories;

public class FileMigrationUtils
{
    private static final Pattern PATTERN_PER_DIM_DATA_FILE = Pattern.compile("litematica_(?<world>.*?)_dim(?<dim>-?[0-9]+)\\.json");

    private static final Predicate<Path> IS_OLD_WORLD_DATA_FILE = path -> {
        String name = path.getFileName().toString();
        return Files.isRegularFile(path) && name.startsWith("litematica_") && name.endsWith(".json");
    };

    public static void tryMigrateOldPerWorldData()
    {
        Path configDir = LitematicaDirectories.getModConfigDirectory();
        Path newDataDirBase = LitematicaDirectories.getPerWorldDataBaseDirectory();

        if (Files.isDirectory(configDir) == false || Files.isReadable(configDir) == false)
        {
            return;
        }

        for (Path file : FileUtils.getDirectoryContents(configDir, IS_OLD_WORLD_DATA_FILE, false))
        {
            String name = file.getFileName().toString();
            Matcher matcher = PATTERN_PER_DIM_DATA_FILE.matcher(name);
            String worldName;
            String newName;
            Path newDataDir;

            // Per-dimension data file
            if (matcher.matches())
            {
                worldName = matcher.group("world");

                String dim = matcher.group("dim");
                newName = "data_dim_" + dim + ".json";
            }
            // Per-world common data file
            else
            {
                worldName = name.substring(11, name.length() - 5);
                newName = "data_common.json";
            }

            newDataDir = newDataDirBase.resolve(worldName.trim());

            if (FileUtils.createDirectoriesIfMissing(newDataDir) == false)
            {
                MessageDispatcher.error().translate("Failed to create directory '" +
                                                    newDataDir.toAbsolutePath() + "'");
                continue;
            }

            Path newFile = newDataDir.resolve(newName.trim());

            try
            {
                FileUtils.move(file, newFile);
                Litematica.LOGGER.info("Moving '{}' => '{}'\n", file, newFile);
            }
            catch (Exception e)
            {
                MessageDispatcher.error().translate("Failed to move data file '" +
                                                    file.toAbsolutePath() + "' to '" +
                                                    newFile.toAbsolutePath() + "'");
            }
        }
    }

    public static void tryMigrateOldAreaSelections()
    {
        Path oldDirPerWorldBase = LitematicaDirectories.getModConfigDirectory().resolve("area_selections_per_world");
        Path newDirPerWorldBase = LitematicaDirectories.getDataDirectory("area_selections").resolve("per_world");

        if (Files.isDirectory(oldDirPerWorldBase) &&
            Files.isReadable(oldDirPerWorldBase))
        {
            if (FileUtils.createDirectoriesIfMissing(newDirPerWorldBase) == false)
            {
                MessageDispatcher.error().translate("Failed to create directory '" +
                                                    newDirPerWorldBase.toAbsolutePath() + "'");
            }
            else
            {
                for (Path file : FileUtils.getSubDirectories(oldDirPerWorldBase))
                {
                    Path oldDir = file.resolve("area_selections");

                    if (Files.isDirectory(oldDir) && Files.isReadable(oldDir))
                    {
                        Path newDir = newDirPerWorldBase.resolve(file.getFileName().toString().trim());

                        if (Files.exists(newDir) == false)
                        {
                            try
                            {
                                Litematica.LOGGER.info("Moving '{}' => '%s'\n", oldDir, newDir);
                                Files.move(oldDir, newDir);
                            }
                            catch (Exception e)
                            {
                                MessageDispatcher.error().translate("Failed to move directory '" +
                                                                    oldDir.toAbsolutePath() + "' to '" +
                                                                    newDir.toAbsolutePath() + "'");
                            }
                        }

                        if (FileUtils.isDirectoryEmpty(file))
                        {
                            Litematica.LOGGER.info("Deleting '{}'\n", file);
                            FileUtils.delete(file);
                        }
                    }
                }

                if (FileUtils.isDirectoryEmpty(oldDirPerWorldBase))
                {
                    Litematica.LOGGER.info("Deleting '{}'\n", oldDirPerWorldBase);
                    FileUtils.delete(oldDirPerWorldBase);
                }
            }
        }

        Path oldDirGlobal = LitematicaDirectories.getModConfigDirectory().resolve("area_selections");
        Path newDirGlobal = LitematicaDirectories.getDataDirectory("area_selections").resolve("global");

        if (Files.isDirectory(oldDirGlobal) &&
            Files.isReadable(oldDirGlobal) &&
            Files.exists(newDirGlobal) == false)
        {
            try
            {
                Litematica.LOGGER.info("Moving '{}' => '{}'\n", oldDirGlobal, newDirGlobal);
                Files.move(oldDirGlobal, newDirGlobal);
            }
            catch (Exception e)
            {
                MessageDispatcher.error().translate("Failed to move directory '" +
                                                    oldDirGlobal.toAbsolutePath() + "' to '" +
                                                    newDirGlobal.toAbsolutePath() + "'");
            }
        }
    }
}
