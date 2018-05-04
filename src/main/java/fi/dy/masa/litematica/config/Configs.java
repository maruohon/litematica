package fi.dy.masa.litematica.config;

import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.options.ConfigBase;
import fi.dy.masa.litematica.config.options.ConfigBoolean;
import fi.dy.masa.litematica.event.InputEventHandler;
import fi.dy.masa.litematica.util.JsonUtils;

public class Configs
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    public static class Generic
    {
        public static final ConfigBoolean VERBOSE_LOGGING       = new ConfigBoolean("verboseLogging", false, "If enabled, a bunch of debug messages will be printed to the console");

        public static final ImmutableList<ConfigBase> OPTIONS = ImmutableList.of(
                VERBOSE_LOGGING
        );
    }

    public static void load()
    {
        File configFile = new File(LiteLoader.getCommonConfigFolder(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                readOptions(root, "Generic", Generic.OPTIONS);
            }
        }

        InputEventHandler.getInstance().updateUsedKeys();
    }

    public static void save()
    {
        File dir = LiteLoader.getCommonConfigFolder();

        if (dir.exists() && dir.isDirectory())
        {
            File configFile = new File(dir, CONFIG_FILE_NAME);
            JsonObject root = new JsonObject();

            writeOptions(root, "Generic", Generic.OPTIONS);

            JsonUtils.writeJsonToFile(root, configFile);
        }
    }

    public static void readOptions(JsonObject root, String category, ImmutableList<ConfigBase> options)
    {
        JsonObject obj = JsonUtils.getNestedObject(root, category, false);

        if (obj != null)
        {
            for (ConfigBase option : options)
            {
                if (obj.has(option.getName()))
                {
                    option.setValueFromJsonElement(obj.get(option.getName()));
                }
            }
        }
    }

    public static void writeOptions(JsonObject root, String category, ImmutableList<ConfigBase> options)
    {
        JsonObject obj = JsonUtils.getNestedObject(root, category, true);

        for (ConfigBase option : options)
        {
            obj.add(option.getName(), option.getAsJsonElement());
        }
    }
}
