package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;

public class DataManager
{
    public static final File ROOT_AREA_SELECTIONS_DIRECTORY = FileUtils.getCanonicalFileIfPossible(new File(getCurrentConfigDirectory(), "area_selections"));
    public static final File ROOT_SCHEMATIC_DIRECTORY = FileUtils.getCanonicalFileIfPossible(new File(Minecraft.getMinecraft().mcDataDir, "schematics"));

    private static final DataManager INSTANCE = new DataManager();

    private static final Pattern PATTERN_ITEM_META = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)(@(?<meta>[0-9]+))$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();

    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static OperationMode operationMode = OperationMode.SCHEMATIC_PLACEMENT;
    private static LayerMode layerMode = LayerMode.ALL;
    private static boolean enableRendering = true;
    private static boolean renderMismatches = true;
    private static boolean renderSchematics = true;
    private static boolean renderSelections = true;
    private static int layerSingle = 0;
    private static int layerMin = 0;
    private static int layerMax = 0;
    private static boolean canSave;

    @Nullable
    private static SchematicPlacement placementToVerify = null;
    private static long clientTickStart;

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();

    private DataManager()
    {
    }

    public static DataManager getInstance()
    {
        return INSTANCE;
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
        INSTANCE.schematicPlacementManager.processQueuedChunks();

        if (placementToVerify != null)
        {
            SchematicVerifier verifier = placementToVerify.getSchematicVerifier();
            verifier.verifyChunks();
            verifier.checkChangedPositions();
        }
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

    public static void setAllRenderingEnabled(boolean enable)
    {
        enableRendering = enable;
    }

    public static boolean toggleAllRenderingEnabled()
    {
        enableRendering = ! enableRendering;
        return enableRendering;
    }

    public static boolean toggleRenderMismatches()
    {
        renderMismatches = ! renderMismatches;
        return renderMismatches;
    }

    public static boolean toggleRenderSelectionBoxes()
    {
        renderSelections = ! renderSelections;
        return renderSelections;
    }

    public static boolean toggleRenderSchematics()
    {
        renderSchematics = ! renderSchematics;
        return renderSchematics;
    }

    public static boolean isRenderingEnabled()
    {
        return enableRendering;
    }

    public static boolean renderSchematics()
    {
        return renderSchematics;
    }

    public static boolean renderMismatches()
    {
        return renderMismatches;
    }

    public static boolean renderSelections()
    {
        return renderSelections;
    }

    public static OperationMode getOperationMode()
    {
        return operationMode;
    }

    public static void setOperationMode(OperationMode mode)
    {
        operationMode = mode;
    }

    public static LayerMode getLayerMode()
    {
        return layerMode;
    }

    public static void setLayerMode(LayerMode mode)
    {
        layerMode = mode;

        WorldUtils.markSchematicChunksForRenderUpdateBetween(0, 255);
        String val = TextFormatting.GREEN.toString() + mode.getDisplayName();
        StringUtils.printActionbarMessage("litematica.message.set_layer_mode_to", val);
    }

    public static int getLayerMin()
    {
        if (layerMode == LayerMode.SINGLE_LAYER)
        {
            return layerSingle;
        }
        else if (layerMode == LayerMode.LAYER_RANGE)
        {
            return layerMin;
        }
        else
        {
            return Integer.MIN_VALUE;
        }
    }

    public static int getLayerMax()
    {
        if (layerMode == LayerMode.SINGLE_LAYER)
        {
            return layerSingle;
        }
        else if (layerMode == LayerMode.LAYER_RANGE)
        {
            return layerMax;
        }
        else
        {
            return Integer.MAX_VALUE;
        }
    }

    public static void setSingleLayer(int layer)
    {
        // TODO different axes
        layerSingle = MathHelper.clamp(layer, 0, 255);
        String val = TextFormatting.GREEN.toString() + String.valueOf(layerSingle);
        StringUtils.printActionbarMessage("litematica.message.set_layer_to", val);
    }

    public static void setMinLayer(int layer)
    {
        layerMin = layer;
    }

    public static void setMaxLayer(int layer)
    {
        layerMax = layer;
    }

    public static boolean moveLayer(int amount)
    {
        if (layerMode == LayerMode.SINGLE_LAYER)
        {
            // TODO proper bounds checks
            int y1 = layerSingle - 1;
            setSingleLayer(layerSingle + amount);
            int y2 = layerSingle + 1;

            WorldUtils.markSchematicChunksForRenderUpdateBetween(y1, y2);

            return true;
        }
        else if (layerMode == LayerMode.LAYER_RANGE)
        {
            layerMin += amount;
            layerMax += amount;
            return true;
        }
        else
        {
            return false;
        }
    }

    @Nullable
    public static File getCurrentDirectoryForContext(String context)
    {
        return LAST_DIRECTORIES.get(context);
    }

    public static void setCurrentDirectoryForContext(String context, File dir)
    {
        LAST_DIRECTORIES.put(context, dir);
    }

    public static void load()
    {
        INSTANCE.loadPerDimensionData();

        File file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            LAST_DIRECTORIES.clear();

            JsonObject root = element.getAsJsonObject();

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
                    operationMode = OperationMode.SCHEMATIC_PLACEMENT;
                }
            }

            if (JsonUtils.hasString(root, "layer_mode"))
            {
                try
                {
                    layerMode = LayerMode.valueOf(root.get("layer_mode").getAsString());
                }
                catch (Exception e) {}

                if (layerMode == null)
                {
                    layerMode = LayerMode.ALL;
                }
            }

            enableRendering = JsonUtils.getBoolean(root, "rendering_enabled");
            renderMismatches = JsonUtils.getBoolean(root, "render_mismatched");
            renderSchematics = JsonUtils.getBoolean(root, "render_schematic");
            renderSelections = JsonUtils.getBoolean(root, "render_selections");

            layerSingle = JsonUtils.getIntegerOrDefault(root, "layer_single", 0);
            layerMin = JsonUtils.getIntegerOrDefault(root, "layer_min", 0);
            layerMax = JsonUtils.getIntegerOrDefault(root, "layer_max", 0);
        }

        canSave = true;
    }

    public static void save()
    {
        save(false);
    }

    public static void save(boolean forceSave)
    {
        if (canSave == false && forceSave == false)
        {
            return;
        }

        INSTANCE.savePerDimensionData();

        JsonObject root = new JsonObject();
        JsonObject objDirs = new JsonObject();

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().getAbsolutePath()));
        }

        root.add("last_directories", objDirs);

        root.add("rendering_enabled", new JsonPrimitive(enableRendering));
        root.add("render_mismatched", new JsonPrimitive(renderMismatches));
        root.add("render_schematic", new JsonPrimitive(renderSchematics));
        root.add("render_selections", new JsonPrimitive(renderSelections));

        root.add("operation_mode", new JsonPrimitive(operationMode.name()));
        root.add("layer_mode", new JsonPrimitive(layerMode.name()));
        root.add("layer_single", new JsonPrimitive(layerSingle));
        root.add("layer_min", new JsonPrimitive(layerMin));
        root.add("layer_max", new JsonPrimitive(layerMax));

        File file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFile(root, file);

        canSave = false;
    }

    private void savePerDimensionData()
    {
        JsonObject root = this.toJson();

        File file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFile(root, file);
    }

    private void loadPerDimensionData()
    {
        File file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
            this.fromJson(root);
        }
    }

    private void fromJson(JsonObject obj)
    {
        this.selectionManager.clear();
        this.schematicPlacementManager.clear();

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

    public static File getCurrentConfigDirectory()
    {
        return new File(LiteLoader.getCommonConfigFolder(), Reference.MOD_ID);
    }

    private static File getCurrentStorageFile(boolean globalData)
    {
        File dir = getCurrentConfigDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            LiteModLitematica.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, getStorageFileName(globalData));
    }

    private static String getStorageFileName(boolean globalData)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            // TODO How to fix this for Forge custom dimensions compatibility (if the type ID is not unique)?
            final int dimension = mc.world.provider.getDimensionType().getId();

            if (mc.isSingleplayer())
            {
                IntegratedServer server = mc.getIntegratedServer();

                if (server != null)
                {
                    String nameEnd = globalData ? ".json" : "_dim" + dimension + ".json";
                    return Reference.MOD_ID + "_" + server.getFolderName() + nameEnd;
                }
            }
            else
            {
                ServerData server = mc.getCurrentServerData();

                if (server != null)
                {
                    String nameEnd = globalData ? ".json" : "_dim" + dimension + ".json";
                    return Reference.MOD_ID + "_" + server.serverIP.replace(':', '_') + nameEnd;
                }
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
