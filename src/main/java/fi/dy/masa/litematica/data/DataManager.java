package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;

public class DataManager
{
    public static final File ROOT_SCHEMATIC_DIRECTORY = FileUtils.getCanonicalFileIfPossible(new File(Minecraft.getMinecraft().mcDataDir, "schematics"));

    private static final Int2ObjectOpenHashMap<DataManager> INSTANCES = new Int2ObjectOpenHashMap<>();

    private static File lastSchematicDirectory = ROOT_SCHEMATIC_DIRECTORY;
    public static ItemStack toolItem = new ItemStack(Items.STICK);

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();

    private DataManager()
    {
    }

    public static DataManager getInstance(World world)
    {
        final int dimension = world.provider.getDimensionType().getId();
        return getInstance(dimension);
    }

    public static DataManager getInstance(int dimension)
    {
        DataManager instance = INSTANCES.get(dimension);

        if (instance == null)
        {
            instance = new DataManager();
            INSTANCES.put(dimension, instance);
        }

        return instance;
    }

    public SelectionManager getSelectionManager()
    {
        return this.selectionManager;
    }

    public SchematicPlacementManager getSchematicPlacementManager()
    {
        return this.schematicPlacementManager;
    }

    public static File getCurrentSchematicDirectory()
    {
        return lastSchematicDirectory;
    }

    public static void setCurrentSchematicDirectory(File dir)
    {
        lastSchematicDirectory = FileUtils.getCanonicalFileIfPossible(dir);
    }

    public static void load()
    {
        File file = getCurrentStorageFile();
        LiteModLitematica.logInfo("Loading settings from file '{}'", file.getAbsolutePath());
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasArray(root, "data"))
            {
                JsonArray arr = root.get("data").getAsJsonArray();
                final int size = arr.size();

                for (int i = 0; i < size; i++)
                {
                    JsonElement el = arr.get(i);

                    if (el.isJsonObject())
                    {
                        JsonObject obj = el.getAsJsonObject();

                        if (JsonUtils.hasInteger(obj, "dim") && JsonUtils.hasObject(obj, "data"))
                        {
                            DataManager manager = getInstance(obj.get("dim").getAsInt());
                            manager.fromJson(obj.get("data").getAsJsonObject());
                        }
                    }
                }
            }

            if (JsonUtils.hasString(root, "last_directory"))
            {
                File dir = new File(root.get("last_directory").getAsString());

                if (dir.exists() && dir.isDirectory())
                {
                    lastSchematicDirectory = dir;
                }
            }
        }
    }

    public static void save()
    {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();

        for (Map.Entry<Integer, DataManager> entry : INSTANCES.entrySet())
        {
            JsonObject o = new JsonObject();
            o.add("dim", new JsonPrimitive(entry.getKey()));
            o.add("data", entry.getValue().toJson());
            arr.add(o);
        }

        if (arr.size() > 0)
        {
            root.add("data", arr);
        }

        root.add("last_directory", new JsonPrimitive(lastSchematicDirectory.getAbsolutePath()));

        File file = getCurrentStorageFile();
        LiteModLitematica.logInfo("Writing settings to file '{}'", file.getAbsolutePath());

        JsonUtils.writeJsonToFile(root, file);
    }

    private void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "selections"))
        {
            this.selectionManager.loadFromJson(obj.get("selections").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "placements"))
        {
            this.schematicPlacementManager.loadFromJson(obj.get("placements").getAsJsonObject());
        }
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.selectionManager.toJson());
        obj.add("placements", this.schematicPlacementManager.toJson());

        return obj;
    }

    private static File getCurrentStorageFile()
    {
        File dir = new File(LiteLoader.getCommonConfigFolder(), Reference.MOD_ID);

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            LiteModLitematica.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, getStorageFileName());
    }

    private static String getStorageFileName()
    {
        if (Minecraft.getMinecraft().isSingleplayer())
        {
            IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();

            if (server != null)
            {
                return Reference.MOD_ID + "_" + server.getFolderName() + ".json";
            }
        }
        else
        {
            ServerData server = Minecraft.getMinecraft().getCurrentServerData();

            if (server != null)
            {
                return Reference.MOD_ID + "_" + server.serverIP.replace(':', '_') + ".json";
            }
        }

        return Reference.MOD_ID + "_default.json";
    }
}
