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
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.projects.SchematicProjectsManager;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiTab;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.message.MessageUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;

public class DataManager implements IDirectoryCache
{
    private static final DataManager INSTANCE = new DataManager();

    private static final Pattern PATTERN_ITEM_META_NBT = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)@(?<meta>[0-9]+)(?<nbt>\\{.*\\})$");
    private static final Pattern PATTERN_ITEM_META = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)@(?<meta>[0-9]+)$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)$");
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();

    private static ToolMode operationMode = ToolMode.SCHEMATIC_PLACEMENT;
    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static IConfigGuiTab configGuiTab = GuiConfigs.VISUALS;
    private static boolean canSave;
    private static long clientTickStart;

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();
    private final SchematicProjectsManager schematicProjectsManager = new SchematicProjectsManager();
    private LayerRange renderRange = new LayerRange(SchematicWorldRenderingNotifier.INSTANCE);
    private AreaSelectionSimple areaSimple = new AreaSelectionSimple(true);
    @Nullable
    private MaterialListBase materialList;

    private DataManager()
    {
    }

    private static DataManager getInstance()
    {
        return INSTANCE;
    }

    public static IDirectoryCache getDirectoryCache()
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

    public static ItemStack getToolItem()
    {
        return toolItem;
    }

    public static IConfigGuiTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(IConfigGuiTab tab)
    {
        configGuiTab = tab;
    }

    public static SelectionManager getSelectionManager()
    {
        return getInstance().selectionManager;
    }

    public static SchematicPlacementManager getSchematicPlacementManager()
    {
        return getInstance().schematicPlacementManager;
    }

    public static SchematicProjectsManager getSchematicProjectsManager()
    {
        return getInstance().schematicProjectsManager;
    }

    @Nullable
    public static MaterialListBase getMaterialList()
    {
        return getInstance().materialList;
    }

    public static void setMaterialList(@Nullable MaterialListBase materialList)
    {
        MaterialListBase old = getInstance().materialList;

        if (old != null && materialList != old)
        {
            MaterialListHudRenderer renderer = old.getHudRenderer();

            if (renderer.getShouldRenderCustom())
            {
                renderer.toggleShouldRender();
                InfoHud.getInstance().removeInfoHudRenderer(renderer, false);
            }
        }

        getInstance().materialList = materialList;
    }

    public static ToolMode getToolMode()
    {
        return operationMode;
    }

    public static void setToolMode(ToolMode mode)
    {
        operationMode = mode;
    }

    public static LayerRange getRenderLayerRange()
    {
        return getInstance().renderRange;
    }

    public static AreaSelectionSimple getSimpleArea()
    {
        return getInstance().areaSimple;
    }

    @Override
    @Nullable
    public File getCurrentDirectoryForContext(String context)
    {
        return LAST_DIRECTORIES.get(context);
    }

    @Override
    public void setCurrentDirectoryForContext(String context, File dir)
    {
        LAST_DIRECTORIES.put(context, dir);
    }

    public static void clear()
    {
        TaskScheduler.getInstanceClient().clearTasks();
        SchematicVerifier.clearActiveVerifiers();
        InfoHud.getInstance().reset(); // remove the line providers and clear the data

        getInstance().clearData(true);
    }

    private void clearData(boolean isLogout)
    {
        this.selectionManager.clear();
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

        getInstance().loadPerDimensionData();

        canSave = true;
    }

    private static void loadPerWorldData()
    {
        LAST_DIRECTORIES.clear();

        File file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasString(root, "config_gui_tab"))
            {
                configGuiTab = IConfigGuiTab.getTabByNameOrDefault(root.get("config_gui_tab").getAsString(), GuiConfigs.TABS, GuiConfigs.VISUALS);
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
                        File dir = new File(el.getAsString());

                        if (dir.exists() && dir.isDirectory())
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
        if (JsonUtils.hasObject(obj, "selections"))
        {
            this.selectionManager.loadFromJson(obj.get("selections").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "placements"))
        {
            this.schematicPlacementManager.loadFromJson(obj.get("placements").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "schematic_projects_manager"))
        {
            this.schematicProjectsManager.loadFromJson(obj.get("schematic_projects_manager").getAsJsonObject());
        }

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

        getInstance().savePerDimensionData();

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

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().getAbsolutePath()));
        }

        root.add("config_gui_tab", new JsonPrimitive(configGuiTab.getName()));
        root.add("operation_mode", new JsonPrimitive(operationMode.name()));
        root.add("tool_mode_data", toolModeDataToJson());
        root.add("last_directories", objDirs);

        File file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFile(root, file);
    }

    private void savePerDimensionData()
    {
        this.schematicProjectsManager.saveCurrentProject();

        File file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFile(this.toJson(), file);
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.selectionManager.toJson());
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

    public static File getCurrentConfigDirectory()
    {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    public static File getSchematicsBaseDirectory()
    {
        boolean custom = Configs.Generic.CUSTOM_SCHEMATIC_DIR_ENABLED.getBooleanValue();
        File dir = null;

        if (custom)
        {
            dir = Configs.Generic.CUSTOM_SCHEMATIC_DIRECTORY.getFile();
        }

        if (custom == false || dir == null)
        {
            dir = FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics"));
        }

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the schematic directory '{}'", dir.getAbsolutePath());
        }

        return dir;
    }

    public static File getAreaSelectionsBaseDirectory()
    {
        File dir;
        String name = StringUtils.getWorldOrServerName();
        File baseDir = getDataBaseDirectory("area_selections");

        if (Configs.Generic.AREAS_PER_WORLD.getBooleanValue() && name != null)
        {
            // The 'area_selections' sub-directory is to prevent showing the world name or server IP in the browser,
            // as the root directory name is shown in the navigation widget
            dir = FileUtils.getCanonicalFileIfPossible(new File(new File(baseDir, "per_world"), name));
        }
        else
        {
            dir = FileUtils.getCanonicalFileIfPossible(new File(baseDir, "global"));
        }

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the area selections base directory '{}'", dir.getAbsolutePath());
        }

        return dir;
    }

    public static File getPerWorldDataBaseDirectory()
    {
        return getDataBaseDirectory("world_specific_data");
    }

    public static File getDataBaseDirectory(String dirName)
    {
        File dir = new File(new File(FileUtils.getMinecraftDirectory(), "litematica"), dirName);

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            MessageUtils.printErrorMessage("litematica.message.error.schematic_placement.failed_to_create_directory", dir.getAbsolutePath());
        }

        return dir;
    }

    private static File getCurrentStorageFile(boolean globalData)
    {
        File dir;
        String worldName = StringUtils.getWorldOrServerName();

        if (worldName != null)
        {
            dir = new File(getPerWorldDataBaseDirectory(), worldName);
        }
        // Fall back to a common set of files at the base directory
        else
        {
            dir = getPerWorldDataBaseDirectory();
        }

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, getStorageFileName(globalData));
    }

    private static String getStorageFileName(boolean globalData)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return globalData ? "data_common.json" : "data_dim_" + WorldUtils.getDimensionId(mc.world) + ".json";
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
                Item item = Item.REGISTRY.getObject(new ResourceLocation(itemName));

                if (item != null && item != Items.AIR)
                {
                    toolItem = new ItemStack(item, 1, meta);
                    toolItem.setTagCompound(nbt);
                    return;
                }
            }
        }
        catch (Exception e)
        {
        }

        // Fall back to a stick
        toolItem = new ItemStack(Items.STICK);

        Configs.Generic.TOOL_ITEM.setValueFromString(Item.REGISTRY.getNameForObject(Items.STICK).toString());
    }

    public static void setHeldItemAsTool()
    {
        EntityPlayer player = Minecraft.getMinecraft().player;

        if (player != null)
        {
            ItemStack stack = player.getHeldItemMainhand();
            toolItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            String cfgStr = "";

            if (stack.isEmpty() == false)
            {
                cfgStr = Item.REGISTRY.getNameForObject(stack.getItem()).toString();
                NBTTagCompound nbt = stack.getTagCompound();

                if (stack.isItemStackDamageable() == false || nbt != null)
                {
                    cfgStr += "@" + stack.getMetadata();

                    if (nbt != null)
                    {
                        cfgStr += nbt.toString();
                    }
                }
            }

            Configs.Generic.TOOL_ITEM.setValueFromString(cfgStr);
            MessageUtils.printActionbarMessage("litematica.message.set_currently_held_item_as_tool");
        }
    }
}
