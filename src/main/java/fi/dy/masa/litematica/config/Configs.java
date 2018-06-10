package fi.dy.masa.litematica.config;

import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputEventHandler;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.malilib.config.options.ConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigString;

public class Configs
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    public static class Generic
    {
        public static final ConfigDouble  GHOST_BLOCK_ALPHA     = new ConfigDouble("ghostBlockAlpha", 0.5, 0, 1, "The alpha value of the ghost blocks, when rendering them as translucent");
        public static final ConfigBoolean RENDER_AS_TRANSLUCENT = new ConfigBoolean("renderAsTranslucent", false, "If enabled, then the schematics are rendered using translucent \"ghost blocks\"");
        public static final ConfigString  TOOL_ITEM             = new ConfigString("toolItem", "minecraft:stick", "The item to use as the \"tool\" for selections etc.");
        public static final ConfigBoolean VERBOSE_LOGGING       = new ConfigBoolean("verboseLogging", false, "If enabled, a bunch of debug messages will be printed to the console");

        public static final ImmutableList<ConfigBase> OPTIONS = ImmutableList.of(
                GHOST_BLOCK_ALPHA,
                RENDER_AS_TRANSLUCENT,
                TOOL_ITEM,
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
        DataManager.setToolItem(Generic.TOOL_ITEM.getValue());
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
