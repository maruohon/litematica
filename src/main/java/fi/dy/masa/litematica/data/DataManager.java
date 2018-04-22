package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.schematic.SchematicPlacement;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.JsonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class DataManager
{
    private static final Int2ObjectOpenHashMap<DataManager> INSTANCES = new Int2ObjectOpenHashMap<>();

    public static ItemStack toolItem = new ItemStack(Items.STICK);

    private final SelectionManager selectionManager = new SelectionManager();
    private final List<SchematicPlacement> loadedSchematics = new ArrayList<>();
    private final Minecraft mc;
    private File lastSchematicDirectory; // TODO use a custom class with split directories?
    private String currentWorld = "default";

    private DataManager()
    {
        this.mc = Minecraft.getMinecraft();
        this.lastSchematicDirectory = new File(this.mc.mcDataDir, "schematics");
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

    public File getSchematicDirectory()
    {
        return this.lastSchematicDirectory;
    }

    public void setSchematicDirectory(File dir)
    {
        this.lastSchematicDirectory = dir;
    }

    public List<SchematicPlacement> getLoadedSchematicsForDimension(World world)
    {
        List<SchematicPlacement> list = new ArrayList<>();

        if (world != null)
        {
            final int dimension = world.provider.getDimensionType().getId();

            for (SchematicPlacement placement : this.loadedSchematics)
            {
                if (placement.getDimension() == dimension)
                {
                    list.add(placement);
                }
            }
        }

        return list;
    }

    public void loadSchematic(World world, SchematicPlacement placement)
    {
        if (this.loadedSchematics.contains(placement) == false)
        {
            this.loadedSchematics.add(placement);
        }
        else
        {
            this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentTranslation("litematica.error.duplicate_schematic_load"));
        }
    }

    public static void load()
    {
        File file = getCurrentStorageFile();
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
        }
    }

    // TODO Call this from something like world exit/save
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

        File file = getCurrentStorageFile();
        JsonUtils.writeJsonToFile(root, file);
    }

    private void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "selections"))
        {
            this.selectionManager.loadFromJson(obj.get("selections").getAsJsonObject());
        }
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.selectionManager.toJson());

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
