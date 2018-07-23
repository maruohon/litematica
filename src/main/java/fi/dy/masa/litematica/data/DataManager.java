package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.OperationMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DataManager
{
    public static final File ROOT_SCHEMATIC_DIRECTORY = FileUtils.getCanonicalFileIfPossible(new File(Minecraft.getMinecraft().mcDataDir, "schematics"));

    private static final Int2ObjectOpenHashMap<DataManager> INSTANCES = new Int2ObjectOpenHashMap<>();

    private static final Pattern PATTERN_ITEM_META = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)(@(?<meta>[0-9]+))$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();

    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static OperationMode operationMode = OperationMode.PLACEMENT;
    private static List<BlockPos> selectedMismatchPositions = new ArrayList<>();
    @Nullable
    private static MismatchType selectedMismatchType = null;

    @Nullable
    private static SchematicPlacement placementToVerify = null;
    private static long clientTickStart;

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();

    private DataManager()
    {
    }

    @Nullable
    public static DataManager getInstance()
    {
        World world = Minecraft.getMinecraft().world;
        return world != null ? getInstance(world) : null;
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

    public static void onClientTickStart()
    {
        clientTickStart = System.nanoTime();
    }

    public static long getClientTickStartTime()
    {
        return clientTickStart;
    }

    public static boolean addSchematicVerificationTask(SchematicPlacement placement)
    {
        if (placementToVerify == null)
        {
            placementToVerify = placement;
            return true;
        }

        return false;
    }

    public static void removeSchematicVerificationTask()
    {
        placementToVerify = null;
    }

    @Nullable
    public static SchematicVerifier getActiveSchematicVerifier()
    {
        return placementToVerify != null ? placementToVerify.getSchematicVerifier() : null;
    }

    public static void runTasks()
    {
        DataManager manager = getInstance();

        if (manager != null)
        {
            manager.getSchematicPlacementManager().processQueuedChunks();
        }

        if (placementToVerify != null)
        {
            SchematicVerifier verifier = placementToVerify.getSchematicVerifier();
            verifier.verifyChunks();
            verifier.checkChangedPositions();
        }
    }

    public static void setActiveMismatchPositionsForRender(MismatchType type, List<BlockPos> list)
    {
        selectedMismatchType = type;
        selectedMismatchPositions.clear();
        selectedMismatchPositions.addAll(list);
    }

    public static void clearActiveMismatchRenderPositions()
    {
        selectedMismatchPositions.clear();
    }

    @Nullable
    public static MismatchType getSelectedMismatchTypeForRender()
    {
        return selectedMismatchType;
    }

    public static List<BlockPos> getSelectedMismatchPositionsForRender()
    {
        return selectedMismatchPositions;
    }

    public static ItemStack getToolItem()
    {
        return toolItem;
    }

    public SelectionManager getSelectionManager()
    {
        return this.selectionManager;
    }

    public SchematicPlacementManager getSchematicPlacementManager()
    {
        return this.schematicPlacementManager;
    }

    public static OperationMode getOperationMode()
    {
        return operationMode;
    }

    public static void setOperationMode(OperationMode mode)
    {
        operationMode = mode;
    }

    @Nullable
    public static File getCurrentDirectoryForContext(String context, boolean useFallback)
    {
        File dir = LAST_DIRECTORIES.get(context);

        if (dir == null && useFallback)
        {
            dir = ROOT_SCHEMATIC_DIRECTORY;
            LAST_DIRECTORIES.put(context, dir);
        }

        return dir;
    }

    public static void setCurrentDirectoryForContext(String context, File dir)
    {
        LAST_DIRECTORIES.put(context, dir);
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

            LAST_DIRECTORIES.clear();

            if (JsonUtils.hasObject(root, "last_directories"))
            {
                JsonObject obj = root.get("last_directories").getAsJsonObject();

                for (Map.Entry<String, JsonElement> entry : obj.entrySet())
                {
                    String name = entry.getKey();
                    JsonElement el = entry.getValue();

                    if (el.isJsonPrimitive())
                    {
                        File dir = new File(el.getAsString());

                        if (dir.exists() && dir.isDirectory())
                        {
                            LAST_DIRECTORIES.put(name, dir);
                        }
                    }
                }
            }

            if (JsonUtils.hasString(root, "operation_mode"))
            {
                try
                {
                    operationMode = OperationMode.valueOf(root.get("operation_mode").getAsString());
                }
                catch (Exception e) {}

                if (operationMode == null)
                {
                    operationMode = OperationMode.PLACEMENT;
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

        JsonObject o = new JsonObject();

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            o.add(entry.getKey(), new JsonPrimitive(entry.getValue().getAbsolutePath()));
            root.add("last_directories", o);
        }

        root.add("operation_mode", new JsonPrimitive(operationMode.name()));

        File file = getCurrentStorageFile();

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

    public static void setToolItem(String itemName)
    {
        try
        {
            Matcher matcher = PATTERN_ITEM_META.matcher(itemName);

            if (matcher.matches())
            {
                Item item = Item.REGISTRY.getObject(new ResourceLocation(matcher.group("name")));

                if (item != null && item != Items.AIR)
                {
                    toolItem = new ItemStack(item, 1, Integer.parseInt(matcher.group("meta")));
                    return;
                }
            }

            matcher = PATTERN_ITEM_BASE.matcher(itemName);

            if (matcher.matches())
            {
                Item item = Item.REGISTRY.getObject(new ResourceLocation(matcher.group("name")));

                if (item != null && item != Items.AIR)
                {
                    toolItem = new ItemStack(item);
                    return;
                }
            }
        }
        catch (Exception e)
        {
        }

        toolItem = new ItemStack(Items.STICK);
    }
}
