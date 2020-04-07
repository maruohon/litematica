package fi.dy.masa.litematica.data;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.io.Files;
import fi.dy.masa.malilib.util.InfoUtils;

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
                InfoUtils.printErrorMessage("Failed to create directory '" + newDataDir.getAbsolutePath() + "'");
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
                InfoUtils.printErrorMessage("Failed to move data file '" + file.getAbsolutePath() + "' to '" + newFile.getAbsolutePath() + "'");
            }
        }
    }
}
