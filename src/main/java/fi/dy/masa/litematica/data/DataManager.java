package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
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
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.util.ToBooleanFunction;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryCache;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class DataManager implements IDirectoryCache
{
    private static final DataManager INSTANCE = new DataManager();

    private static final Pattern PATTERN_ITEM_NBT = Pattern.compile("^(?<name>[a-z0-9\\._-]+:[a-z0-9\\._-]+)(?<nbt>\\{.*\\})$");
    private static final Pattern PATTERN_ITEM_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");
    private static final Map<String, File> LAST_DIRECTORIES = new HashMap<>();
    private static final ArrayList<ToBooleanFunction<Text>> CHAT_LISTENERS = new ArrayList<>();

    private static ItemStack toolItem = new ItemStack(Items.STICK);
    private static ConfigGuiTab configGuiTab = ConfigGuiTab.GENERIC;
    private static boolean createPlacementOnLoad = true;
    private static boolean canSave;
    private static boolean isCarpetServer;
    private static long clientTickStart;

    private final SelectionManager selectionManager = new SelectionManager();
    private final SchematicPlacementManager schematicPlacementManager = new SchematicPlacementManager();
    private final SchematicProjectsManager schematicProjectsManager = new SchematicProjectsManager();
    private LayerRange renderRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
    private ToolMode operationMode = ToolMode.SCHEMATIC_PLACEMENT;
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

    public static void setIsCarpetServer(boolean isCarpetServer)
    {
        DataManager.isCarpetServer = isCarpetServer;
    }

    public static boolean isCarpetServer()
    {
        return isCarpetServer;
    }

    public static void addChatListener(ToBooleanFunction<Text> listener)
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.add(listener);
        }
    }

    public static void removeChatListener(ToBooleanFunction<Text> listener)
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.remove(listener);
        }
    }

    public static void clearChatListeners()
    {
        synchronized (CHAT_LISTENERS)
        {
            CHAT_LISTENERS.clear();
        }
    }

    public static boolean onChatMessage(Text text)
    {
        synchronized (CHAT_LISTENERS)
        {
            boolean cancel = false;

            for (ToBooleanFunction<Text> listener : CHAT_LISTENERS)
            {
                cancel |= listener.applyAsBoolean(text);
            }

            return cancel;
        }
    }

    public static boolean getCreatePlacementOnLoad()
    {
        return createPlacementOnLoad;
    }

    public static void setCreatePlacementOnLoad(boolean create)
    {
        createPlacementOnLoad = create;
    }

    public static ConfigGuiTab getConfigGuiTab()
    {
        return configGuiTab;
    }

    public static void setConfigGuiTab(ConfigGuiTab tab)
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

        if (old != null)
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
        return getInstance().operationMode;
    }

    public static void setToolMode(ToolMode mode)
    {
        getInstance().operationMode = mode;
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

    public static void load()
    {
        getInstance().loadPerDimensionData();

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

            if (JsonUtils.hasString(root, "config_gui_tab"))
            {
                try
                {
                    configGuiTab = ConfigGuiTab.valueOf(root.get("config_gui_tab").getAsString());
                }
                catch (Exception e) {}

                if (configGuiTab == null)
                {
                    configGuiTab = ConfigGuiTab.GENERIC;
                }
            }

            createPlacementOnLoad = JsonUtils.getBooleanOrDefault(root, "create_placement_on_load", true);
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

        getInstance().savePerDimensionData();

        JsonObject root = new JsonObject();
        JsonObject objDirs = new JsonObject();

        for (Map.Entry<String, File> entry : LAST_DIRECTORIES.entrySet())
        {
            objDirs.add(entry.getKey(), new JsonPrimitive(entry.getValue().getAbsolutePath()));
        }

        root.add("last_directories", objDirs);

        root.add("create_placement_on_load", new JsonPrimitive(createPlacementOnLoad));
        root.add("config_gui_tab", new JsonPrimitive(configGuiTab.name()));

        File file = getCurrentStorageFile(true);
        JsonUtils.writeJsonToFile(root, file);

        canSave = false;
    }

    public static void clear()
    {
        TaskScheduler.getInstanceClient().clearTasks();
        SchematicVerifier.clearActiveVerifiers();

        getSchematicPlacementManager().clear();
        getSchematicProjectsManager().clear();
        getSelectionManager().clear();
        setMaterialList(null);
        clearChatListeners();

        InfoHud.getInstance().reset(); // remove the line providers and clear the data
        setIsCarpetServer(false);
    }

    private void savePerDimensionData()
    {
        this.schematicProjectsManager.saveCurrentProject();
        JsonObject root = this.toJson();

        File file = getCurrentStorageFile(false);
        JsonUtils.writeJsonToFile(root, file);
    }

    private void loadPerDimensionData()
    {
        this.selectionManager.clear();
        this.schematicPlacementManager.clear();
        this.schematicProjectsManager.clear();
        this.materialList = null;

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
            this.renderRange = LayerRange.createFromJson(JsonUtils.getNestedObject(obj, "render_range", false), SchematicWorldRefresher.INSTANCE);
        }

        if (JsonUtils.hasString(obj, "operation_mode"))
        {
            try
            {
                this.operationMode = ToolMode.valueOf(obj.get("operation_mode").getAsString());
            }
            catch (Exception e) {}

            if (this.operationMode == null)
            {
                this.operationMode = ToolMode.AREA_SELECTION;
            }
        }

        if (JsonUtils.hasObject(obj, "area_simple"))
        {
            this.areaSimple = AreaSelectionSimple.fromJson(obj.get("area_simple").getAsJsonObject());
        }

        if (JsonUtils.hasObject(obj, "tool_mode_data"))
        {
            this.toolModeDataFromJson(obj.get("tool_mode_data").getAsJsonObject());
        }
    }

    private JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("selections", this.selectionManager.toJson());
        obj.add("placements", this.schematicPlacementManager.toJson());
        obj.add("schematic_projects_manager", this.schematicProjectsManager.toJson());
        obj.add("operation_mode", new JsonPrimitive(this.operationMode.name()));
        obj.add("render_range", this.renderRange.toJson());
        obj.add("area_simple", this.areaSimple.toJson());
        obj.add("tool_mode_data", this.toolModeDataToJson());

        return obj;
    }

    private JsonObject toolModeDataToJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("delete", ToolModeData.DELETE.toJson());
        return obj;
    }

    private void toolModeDataFromJson(JsonObject obj)
    {
        if (JsonUtils.hasObject(obj, "delete"))
        {
            ToolModeData.DELETE.fromJson(obj.get("delete").getAsJsonObject());
        }
    }

    public static File getDefaultBaseSchematicDirectory()
    {
        return FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics"));
    }

    public static File getCurrentConfigDirectory()
    {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    public static File getSchematicsBaseDirectory()
    {
        File dir;

        if (Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED.getBooleanValue())
        {
            dir = new File(Configs.Generic.CUSTOM_SCHEMATIC_BASE_DIRECTORY.getStringValue());
        }
        else
        {
            dir = getDefaultBaseSchematicDirectory();
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

        if (Configs.Generic.AREAS_PER_WORLD.getBooleanValue() && name != null)
        {
            // The 'area_selections' sub-directory is to prevent showing the world name or server IP in the browser,
            // as the root directory name is shown in the navigation widget
            dir = FileUtils.getCanonicalFileIfPossible(new File(new File(new File(getCurrentConfigDirectory(), "area_selections_per_world"), name), "area_selections"));
        }
        else
        {
            dir = FileUtils.getCanonicalFileIfPossible(new File(getCurrentConfigDirectory(), "area_selections"));
        }

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the area selections base directory '{}'", dir.getAbsolutePath());
        }

        return dir;
    }

    private static File getCurrentStorageFile(boolean globalData)
    {
        File dir = getCurrentConfigDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            Litematica.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, StringUtils.getStorageFileName(globalData, Reference.MOD_ID + "_", ".json", "default"));
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
            Matcher matcherNbt = PATTERN_ITEM_NBT.matcher(itemNameIn);
            Matcher matcherBase = PATTERN_ITEM_BASE.matcher(itemNameIn);

            String itemName = null;
            NbtCompound nbt = null;

            if (matcherNbt.matches())
            {
                itemName = matcherNbt.group("name");
                nbt = (new StringNbtReader(new StringReader(matcherNbt.group("nbt")))).parseCompound();
            }
            else if (matcherBase.matches())
            {
                itemName = matcherBase.group("name");
            }

            if (itemName != null)
            {
                Item item = Registry.ITEM.get(new Identifier(itemName));

                if (item != null && item != Items.AIR)
                {
                    toolItem = new ItemStack(item);
                    toolItem.setNbt(nbt);
                    return;
                }
            }
        }
        catch (Exception ignore)
        {
        }

        // Fall back to a stick
        toolItem = new ItemStack(Items.STICK);
        Configs.Generic.TOOL_ITEM.setValueFromString(Registry.ITEM.getId(Items.STICK).toString());
    }
}
