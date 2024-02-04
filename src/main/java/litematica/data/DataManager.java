package litematica.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

import malilib.gui.config.ConfigTab;
import malilib.gui.tab.ScreenTab;
import malilib.gui.widget.util.DirectoryCache;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.WorldWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.gui.ConfigScreen;
import litematica.materials.MaterialListBase;
import litematica.materials.MaterialListHudRenderer;
import litematica.render.infohud.InfoHud;
import litematica.scheduler.TaskScheduler;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.projects.SchematicProjectsManager;
import litematica.selection.AreaSelectionManager;
import litematica.selection.AreaSelectionSimple;
import litematica.tool.ToolMode;
import litematica.tool.ToolModeData;
import litematica.util.LitematicaDirectories;
import litematica.world.SchematicWorldHandler;
import litematica.world.SchematicWorldRenderingNotifier;

public class DataManager implements DirectoryCache
{
    public static final DataManager INSTANCE = new DataManager();

    private static final Pattern PATTERN_ITEM_META_NBT = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)@(?<meta>[0-9]+)(?<nbt>\\{.*\\})$");
    private static final Pattern PATTERN_ITEM_META = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)@(?<meta>[0-9]+)$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)$");
    private static final Map<String, Path> LAST_DIRECTORIES = new HashMap<>();

    private static ToolMode operationMode = ToolMode.SCHEMATIC_PLACEMENT;
    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static ScreenTab configGuiTab = ConfigScreen.VISUALS;
    private static boolean canSave;
    private static long clientTickStart;

    private final AreaSelectionManager areaSelectionManager = new AreaSelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager(SchematicWorldHandler::getSchematicWorld);
    private final SchematicProjectsManager schematicProjectsManager = new SchematicProjectsManager();
    private LayerRange renderRange = new LayerRange(SchematicWorldRenderingNotifier.INSTANCE);
    private AreaSelectionSimple areaSimple = new AreaSelectionSimple(true);
    @Nullable
    private MaterialListBase materialList;

    private DataManager()
    {
    }

    public static void onClientTickStart()
    {
        clientTickStart = System.nanoTime();
    }

    public static long getClientTickStartTime()
    {
        return clientTickStart;
    }

    public static ItemStack getToolItem()
    {
        return toolItem;
    }

    public static ScreenTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigTab tab)
    {
        configGuiTab = tab;
    }

    public static AreaSelectionManager getAreaSelectionManager()
    {
        return INSTANCE.areaSelectionManager;
    }

    public static SchematicPlacementManager getSchematicPlacementManager()
    {
        return INSTANCE.schematicPlacementManager;
    }

    public static SchematicProjectsManager getSchematicProjectsManager()
    {
        return INSTANCE.schematicProjectsManager;
    }

    @Nullable
    public static MaterialListBase getMaterialList()
    {
        return INSTANCE.materialList;
    }

    public static void setMaterialList(@Nullable MaterialListBase materialList)
    {
        MaterialListBase old = INSTANCE.materialList;

        if (old != null && materialList != old)
        {
            MaterialListHudRenderer renderer = old.getHudRenderer();

            if (renderer.getShouldRenderCustom())
            {
                renderer.toggleShouldRender();
                InfoHud.getInstance().removeInfoHudRenderer(renderer, false);
            }
        }

        INSTANCE.materialList = materialList;
    }

    public static ToolMode getToolMode()
    {
        return operationMode;
    }

    public static void setToolMode(ToolMode mode)
    {
        operationMode = mode;
    }

    public static void cycleToolMode(boolean forward)
    {
        operationMode = operationMode.cycle(forward);
    }

    public static LayerRange getRenderLayerRange()
    {
        return INSTANCE.renderRange;
    }

    public static AreaSelectionSimple getSimpleArea()
    {
        return INSTANCE.areaSimple;
    }

    @Override
    @Nullable
    public Path getCurrentDirectoryForContext(String context)
    {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, Path dir)
    {
        LAST_DIRECTORIES.put(context, dir);
    }

    public static void clear()
    {
        TaskScheduler.getInstanceClient().clearTasks();
        InfoHud.getInstance().reset(); // remove the line providers and clear the data

        INSTANCE.clearData(true);
    }

    private void clearData(boolean isLogout)
    {
        this.areaSelectionManager.clear();
        this.schematicPlacementManager.clear();
        this.schematicProjectsManager.clear();
        this.areaSimple = new AreaSelectionSimple(true);

        if (isLogout || (this.materialList != null && this.materialList.isForPlacement()))
        {
            setMaterialList(null);
        }
    }

    public static void load(boolean isDimensionChange)
    {
        if (isDimensionChange == false)
        {
            loadPerWorldData();
        }

        INSTANCE.loadPerDimensionData();

        canSave = true;
    }

    private static void loadPerWorldData()
    {
        LAST_DIRECTORIES.clear();

        Path file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasString(root, "config_gui_tab"))
            {
                configGuiTab = ScreenTab.getTabByNameOrDefault(root.get("config_gui_tab").getAsString(), ConfigScreen.ALL_TABS, ConfigScreen.VISUALS);
            }

            if (JsonUtils.hasString(root, "operation_mode"))
            {
                operationMode = ToolMode.fromString(root.get("operation_mode").getAsString());
            }

            if (JsonUtils.hasObject(root, "tool_mode_data"))
            {
                toolModeDataFromJson(root.get("tool_mode_data").getAsJsonObject());
            }

            if (JsonUtils.hasObject(root, "last_directories"))
            {
                JsonObject obj = root.get("last_directories").getAsJsonObject();

                for (Map.Entry<String, JsonElement> entry : obj.entrySet())
                {
                    String name = entry.getKey();
                    JsonElement el = entry.getValue();

                    if (el.isJsonPrimitive())
                    {
                        Path dir = Paths.get(el.getAsString());

                        if (Files.isDirectory(dir))
                        {
                            LAST_DIRECTORIES.put(name, dir);
                        }
                    }
                }
            }
        }
    }

    private void loadPerDimensionData()
    {
        this.clearData(false);

        Path file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();
            this.fromJson(root);
        }
    }

    private void fromJson(JsonObject obj)
    {
        JsonUtils.getObjectIfExists(obj, "selections", this.areaSelectionManager::loadFromJson);
        JsonUtils.getObjectIfExists(obj, "placements", this.schematicPlacementManager::loadFromJson);
        JsonUtils.getObjectIfExists(obj, "schematic_projects_manager", this.schematicProjectsManager::loadFromJson);

        if (JsonUtils.hasObject(obj, "render_range"))
        {
            this.renderRange = LayerRange.createFromJson(JsonUtils.getNestedObject(obj, "render_range", false), SchematicWorldRenderingNotifier.INSTANCE);
        }

        if (JsonUtils.hasObject(obj, "area_simple"))
        {
            this.areaSimple = AreaSelectionSimple.fromJson(obj.get("area_simple").getAsJsonObject());
        }
    }

    public static void save(boolean isDimensionChange)
    {
        if (canSave == false)
        {
            return;
        }

        INSTANCE.savePerDimensionData();

        if (isDimensionChange == false)
        {
            savePerWorldData();
        }

        canSave = false;
    }

    private static void savePerWorldData()
    {
        JsonObject root = new JsonObject();
        JsonObject objDirs = new JsonObject();

        for (Map.Entry<String, Path> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().toAbsolutePath().toString()));
        }

        root.add("config_gui_tab", new JsonPrimitive(configGuiTab.getName()));
        root.add("operation_mode", new JsonPrimitive(operationMode.name()));
        root.add("tool_mode_data", toolModeDataToJson());
        root.add("last_directories", objDirs);

        Path file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFile(root, file);
    }

    private void savePerDimensionData()
    {
        this.schematicProjectsManager.saveCurrentProject();

        Path file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFile(this.toJson(), file);
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.areaSelectionManager.toJson());
        obj.add("placements", this.schematicPlacementManager.toJson());
        obj.add("schematic_projects_manager", this.schematicProjectsManager.toJson());
        obj.add("render_range", this.renderRange.toJson());
        obj.add("area_simple", this.areaSimple.toJson());

        return obj;
    }

    private static JsonObject toolModeDataToJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("delete", ToolModeData.DELETE.toJson());
        obj.add("update_blocks", ToolModeData.UPDATE_BLOCKS.toJson());
        return obj;
    }

    private static void toolModeDataFromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "delete"))
        {
            ToolModeData.DELETE.fromJson(obj.get("delete").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "update_blocks"))
        {
            ToolModeData.UPDATE_BLOCKS.fromJson(obj.get("update_blocks").getAsJsonObject());
        }
    }

    private static Path getCurrentStorageFile(boolean globalData)
    {
        Path dir = LitematicaDirectories.getPerWorldDataBaseDirectory();
        String worldName = StringUtils.getWorldOrServerName();

        if (worldName != null)
        {
            dir = dir.resolve(worldName);
        }

        LitematicaDirectories.createDirectoriesIfMissingOrPrintError(dir);

        return dir.resolve(getStorageFileName(globalData));
    }

    private static String getStorageFileName(boolean globalData)
    {
        return globalData ? "data_common.json" : "data_dim_" +
                            WorldWrap.getDimensionIdAsString(GameWrap.getClientWorld()) + ".json";
    }

    public static void setToolItem(String itemNameIn)
    {
        if (itemNameIn.isEmpty() || itemNameIn.equals("empty"))
        {
            toolItem = ItemStack.EMPTY;
            return;
        }

        try
        {
            Matcher matcherNbt = PATTERN_ITEM_META_NBT.matcher(itemNameIn);
            Matcher matcherMeta = PATTERN_ITEM_META.matcher(itemNameIn);
            Matcher matcherBase = PATTERN_ITEM_BASE.matcher(itemNameIn);

            String itemName = null;
            int meta = 0;
            NBTTagCompound nbt = null;

            if (matcherNbt.matches())
            {
                itemName = matcherNbt.group("name");
                meta = Integer.parseInt(matcherNbt.group("meta"));
                nbt = JsonToNBT.getTagFromJson(matcherNbt.group("nbt"));
            }
            else if (matcherMeta.matches())
            {
                itemName = matcherMeta.group("name");
                meta = Integer.parseInt(matcherMeta.group("meta"));
            }
            else if (matcherBase.matches())
            {
                itemName = matcherBase.group("name");
            }

            if (itemName != null)
            {
                Item item = RegistryUtils.getItemByIdStr(itemName);

                if (item != null && item != Items.AIR)
                {
                    toolItem = new ItemStack(item, 1, meta);
                    ItemWrap.setTag(toolItem, nbt);
                    return;
                }
            }
        }
        catch (Exception ignore) {}

        // Fall back to a stick
        toolItem = new ItemStack(Items.STICK);

        Configs.Generic.TOOL_ITEM.setValue(RegistryUtils.getItemIdStr(Items.STICK));
    }

    public static void setHeldItemAsTool()
    {
        EntityPlayer player = GameWrap.getClientPlayer();

        if (player != null)
        {
            ItemStack stack = player.getHeldItemMainhand();
            toolItem = ItemWrap.isEmpty(stack) ? ItemStack.EMPTY : stack.copy();
            String cfgStr = "";

            if (ItemWrap.notEmpty(stack))
            {
                cfgStr = RegistryUtils.getItemIdStr(stack.getItem());
                NBTTagCompound nbt = ItemWrap.getTag(stack);

                if (stack.isItemStackDamageable() == false || nbt != null)
                {
                    cfgStr += "@" + stack.getMetadata();

                    if (nbt != null)
                    {
                        cfgStr += nbt.toString();
                    }
                }
            }

            Configs.Generic.TOOL_ITEM.setValue(cfgStr);
            MessageDispatcher.generic().customHotbar().translate("litematica.message.set_currently_held_item_as_tool");
        }
    }

    public static void checkDateFormat(String value)
    {
        try
        {
            SimpleDateFormat fmt = new SimpleDateFormat(value);
            assert fmt != null;
        }
        catch (Exception ignore)
        {
            MessageDispatcher.error("litematica.message.error.config.date_format_invalid");
        }
    }
}
