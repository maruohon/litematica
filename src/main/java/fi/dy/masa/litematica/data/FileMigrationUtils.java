package fi.dy.masa.litematica.data;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.io.Files;
import fi.dy.masa.malilib.message.MessageUtils;

public class FileMigrationUtils
{
    private static final Pattern PATTERN_PER_DIM_DATA_FILE = Pattern.compile("litematica_(?<world>.*?)_dim(?<dim>-?[0-9]+)\\.json");

    private static final FileFilter IS_OLD_WORLD_DATA_FILE = (file) -> {
        String name = file.getName();

        if (file.isFile() && name.startsWith("litematica_") && name.endsWith(".json"))
        {
            return true;
        }

        return false;
    };

    public static void tryMigrateOldPerWorldData()
    {
        File configDir = DataManager.getCurrentConfigDirectory();
        File newDataDirBase = DataManager.getPerWorldDataBaseDirectory();

        if (configDir.exists() == false || configDir.canRead() == false)
        {
            return;
        }

        for (File file : configDir.listFiles(IS_OLD_WORLD_DATA_FILE))
        {
            String name = file.getName();
            Matcher matcher = PATTERN_PER_DIM_DATA_FILE.matcher(name);
            String worldName;
            String newName;
            File newDataDir;

            // Per-dimension data file
            if (matcher.matches())
            {
                worldName = matcher.group("world");
                newDataDir = new File(newDataDirBase, worldName);

                String dim = matcher.group("dim");
                newName = "data_dim_" + dim + ".json";
            }
            // Per-world common data file
            else
            {
                worldName = name.substring(11, name.length() - 5);
                newDataDir = new File(newDataDirBase, worldName);
                newName = "data_common.json";
            }

            if (newDataDir.exists() == false && newDataDir.mkdirs() == false)
            {
                MessageUtils.printErrorMessage("Failed to create directory '" + newDataDir.getAbsolutePath() + "'");
                continue;
            }

            File newFile = new File(newDataDir, newName);

            try
            {
                Files.move(file, newFile);
                System.out.printf("moving '%s' => '%s'\n", file, newFile);
            }
            catch (Exception e)
            {
                MessageUtils.printErrorMessage("Failed to move data file '" + file.getAbsolutePath() + "' to '" + newFile.getAbsolutePath() + "'");
            }
        }
    }

    public static void tryMigrateOldAreaSelections()
    {
        File oldDirPerWorldBase = new File(DataManager.getCurrentConfigDirectory(), "area_selections_per_world");
        File newDirPerWorldBase = new File(DataManager.getDataBaseDirectory("area_selections"), "per_world");

        if (oldDirPerWorldBase.exists() && oldDirPerWorldBase.isDirectory() && oldDirPerWorldBase.canRead())
        {
            if (newDirPerWorldBase.exists() == false && newDirPerWorldBase.mkdirs() == false)
            {
                MessageUtils.printErrorMessage("Failed to create directory '" + newDirPerWorldBase.getAbsolutePath() + "'");
            }
            else
            {
                for (File file : oldDirPerWorldBase.listFiles((f) -> f.isDirectory()))
                {
                    File oldDir = new File(file, "area_selections");

                    if (oldDir.exists() && oldDir.isDirectory() && oldDir.canRead())
                    {
                        File newDir = new File(newDirPerWorldBase, file.getName());

                        if (newDir.exists() == false)
                        {
                            try
                            {
                                System.out.printf("Moving '%s' => '%s'\n", oldDir, newDir);
                                Files.move(oldDir, newDir);
                            }
                            catch (Exception e)
                            {
                                MessageUtils.printErrorMessage("Failed to move directory '" + oldDir.getAbsolutePath() + "' to '" + newDir.getAbsolutePath() + "'");
                            }
                        }

                        if (file.list().length == 0)
                        {
                            System.out.printf("Deleting '%s'\n", file);
                            file.delete();
                        }
                    }
                }

                if (oldDirPerWorldBase.list().length == 0)
                {
                    System.out.printf("Deleting '%s'\n", oldDirPerWorldBase);
                    oldDirPerWorldBase.delete();
                }
            }
        }

        File oldDirGlobal = new File(DataManager.getCurrentConfigDirectory(), "area_selections");
        File newDirGlobal = new File(DataManager.getDataBaseDirectory("area_selections"), "global");

        if (oldDirGlobal.exists() && oldDirGlobal.isDirectory() && oldDirGlobal.canRead() && newDirGlobal.exists() == false)
        {
            try
            {
                System.out.printf("Moving '%s' => '%s'\n", oldDirGlobal, newDirGlobal);
                Files.move(oldDirGlobal, newDirGlobal);
            }
            catch (Exception e)
            {
                MessageUtils.printErrorMessage("Failed to move directory '" + oldDirGlobal.getAbsolutePath() + "' to '" + newDirGlobal.getAbsolutePath() + "'");
            }
        }
    }
}
