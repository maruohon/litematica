package litematica.schematic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import malilib.config.option.OptionConfig;
import malilib.config.value.BaseOptionConfigValue;
import malilib.util.data.SimpleBooleanStorageWithDefault;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.world.BlockState;
import litematica.Litematica;
import litematica.util.value.SchematicSaveWorldSelection;

public class SchematicSaveSettings
{
    public final SimpleBooleanStorageWithDefault saveBlocks              = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveBlockEntities       = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveScheduledBlockTicks = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault saveEntities            = new SimpleBooleanStorageWithDefault(true);
    public final SimpleBooleanStorageWithDefault exposedBlocksOnly       = new SimpleBooleanStorageWithDefault(false);
    public final SimpleBooleanStorageWithDefault obeyIgnoredBlocks       = new SimpleBooleanStorageWithDefault(false);
    public final SimpleBooleanStorageWithDefault obeyIgnoredBlockStates  = new SimpleBooleanStorageWithDefault(false);
    public final SimpleBooleanStorageWithDefault obeyIgnoredEntities     = new SimpleBooleanStorageWithDefault(false);
    public final OptionConfig<SchematicSaveWorldSelection> worldSelection = new OptionConfig<>("-", SchematicSaveWorldSelection.VANILLA_ONLY, SchematicSaveWorldSelection.VALUES);
    public final OptionConfig<SaveSide> saveSide = new OptionConfig<>("-", SaveSide.AUTO, SaveSide.VALUES);

    public final Set<Block> ignoredBlocks = new HashSet<>();
    public final Set<BlockState> ignoredBlockStates = new HashSet<>();
    public final Set<Class<? extends Entity>> ignoredEntities = new HashSet<>();

    public SchematicType schematicType = SchematicType.LITEMATICA;

    public SchematicSaveSettings copy()
    {
        SchematicSaveSettings newSettings = new SchematicSaveSettings();

        newSettings.saveBlocks.setBooleanValue(this.saveBlocks.getBooleanValue());
        newSettings.saveBlockEntities.setBooleanValue(this.saveBlockEntities.getBooleanValue());
        newSettings.saveScheduledBlockTicks.setBooleanValue(this.saveScheduledBlockTicks.getBooleanValue());
        newSettings.saveEntities.setBooleanValue(this.saveEntities.getBooleanValue());
        newSettings.exposedBlocksOnly.setBooleanValue(this.exposedBlocksOnly.getBooleanValue());

        newSettings.worldSelection.setValue(this.worldSelection.getValue());
        newSettings.saveSide.setValue(this.saveSide.getValue());

        newSettings.obeyIgnoredBlocks.setBooleanValue(this.obeyIgnoredBlocks.getBooleanValue());
        newSettings.obeyIgnoredBlockStates.setBooleanValue(this.obeyIgnoredBlockStates.getBooleanValue());
        newSettings.obeyIgnoredEntities.setBooleanValue(this.obeyIgnoredEntities.getBooleanValue());

        newSettings.ignoredBlocks.addAll(this.ignoredBlocks);
        newSettings.ignoredBlockStates.addAll(this.ignoredBlockStates);
        newSettings.ignoredEntities.addAll(this.ignoredEntities);

        newSettings.schematicType = this.schematicType;

        return newSettings;
    }

    public void setIgnoredBlocks(Collection<Block> ignoredBlocks)
    {
        this.ignoredBlocks.clear();
        this.ignoredBlocks.addAll(ignoredBlocks);
    }

    public void setIgnoredBlockStates(Collection<BlockState> ignoredBlockStates)
    {
        this.ignoredBlockStates.clear();
        this.ignoredBlockStates.addAll(ignoredBlockStates);
    }

    public void setIgnoredEntities(Collection<Class<? extends Entity>> ignoredEntities)
    {
        this.ignoredEntities.clear();
        this.ignoredEntities.addAll(ignoredEntities);
    }

    public JsonElement toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("save_blocks", this.saveBlocks.getBooleanValue());
        obj.addProperty("save_block_entities", this.saveBlockEntities.getBooleanValue());
        obj.addProperty("save_scheduled_block_ticks", this.saveScheduledBlockTicks.getBooleanValue());
        obj.addProperty("save_entities", this.saveEntities.getBooleanValue());
        obj.addProperty("exposed_blocks_only", this.exposedBlocksOnly.getBooleanValue());
        obj.addProperty("world_selection", this.worldSelection.getValue().getName());
        obj.addProperty("save_side", this.saveSide.getValue().getName());

        obj.addProperty("obey_ignored_blocks", this.obeyIgnoredBlocks.getBooleanValue());
        obj.addProperty("obey_ignored_block_states", this.obeyIgnoredBlockStates.getBooleanValue());
        obj.addProperty("obey_ignored_entities", this.obeyIgnoredEntities.getBooleanValue());

        obj.add("ignored_blocks", this.getIgnoredBlocksAsJson());
        obj.add("ignored_block_states", this.getIgnoredBlockStatesAsJson());
        obj.add("ignored_entities", this.getIgnoredEntitiesAsJson());

        return obj;
    }

    protected JsonElement getIgnoredBlocksAsJson()
    {
        JsonArray arr = new JsonArray();

        for (Block block : this.ignoredBlocks)
        {
            arr.add(RegistryUtils.getBlockIdStr(block));
        }

        return arr;
    }

    protected JsonElement getIgnoredBlockStatesAsJson()
    {
        JsonArray arr = new JsonArray();

        for (BlockState state : this.ignoredBlockStates)
        {
            arr.add(state.getFullStateString());
        }

        return arr;
    }

    protected JsonElement getIgnoredEntitiesAsJson()
    {
        JsonArray arr = new JsonArray();

        for (Class<? extends Entity> clazz : this.ignoredEntities)
        {
            ResourceLocation rl = EntityList.getKey(clazz);

            if (rl == null)
            {
                continue;
            }

            arr.add(rl.toString());
        }

        return arr;
    }

    public void tryLoad(String serializedValue)
    {
        JsonElement el = JsonUtils.parseJsonFromString(serializedValue);

        if ((el instanceof JsonObject) == false)
        {
            return;
        }

        JsonObject obj = el.getAsJsonObject();

        this.saveBlocks.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_blocks", true));
        this.saveBlockEntities.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_block_entities", true));
        this.saveScheduledBlockTicks.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_scheduled_block_ticks", true));
        this.saveEntities.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "save_entities", true));
        this.exposedBlocksOnly.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "exposed_blocks_only", false));

        this.worldSelection.setValue(SchematicSaveWorldSelection.findValueByName(JsonUtils.getStringOrDefault(obj, "world_selection", ""), SchematicSaveWorldSelection.VALUES));
        this.saveSide.setValue(SaveSide.findValueByName(JsonUtils.getStringOrDefault(obj, "save_side", ""), SaveSide.VALUES));

        this.obeyIgnoredBlocks.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "obey_ignored_blocks", false));
        this.obeyIgnoredBlockStates.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "obey_ignored_block_states", false));
        this.obeyIgnoredEntities.setBooleanValue(JsonUtils.getBooleanOrDefault(obj, "obey_ignored_entities", false));

        this.ignoredBlocks.clear();
        JsonUtils.getArrayElementsIfExists(obj, "ignored_blocks", this::parseBlock);

        this.ignoredBlockStates.clear();
        JsonUtils.getArrayElementsIfExists(obj, "ignored_block_states", this::parseBlockState);

        this.ignoredEntities.clear();
        JsonUtils.getArrayElementsIfExists(obj, "ignored_entities", this::parseEntity);
    }

    protected void parseBlock(JsonElement el)
    {
        Block block = RegistryUtils.getBlockByIdStr(el.getAsString());
        this.ignoredBlocks.add(block);
    }

    protected void parseBlockState(JsonElement el)
    {
        Optional<BlockState> stateOpt = BlockUtils.getBlockStateFromString(el.getAsString());

        if (stateOpt.isPresent())
        {
            this.ignoredBlockStates.add(stateOpt.get());
        }
    }

    protected void parseEntity(JsonElement el)
    {
        try
        {
            String str = el.getAsString();
            Class<? extends Entity> clazz = EntityList.getClassFromName(str);

            if (clazz != null)
            {
                this.ignoredEntities.add(clazz);
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Failed to parse ignored entity type: {}", el);
        }
    }

    public static class SaveSide extends BaseOptionConfigValue
    {
        public static final SaveSide AUTO   = new SaveSide("auto",   "litematica.name.save_side.auto");
        public static final SaveSide CLIENT = new SaveSide("client", "litematica.name.save_side.client");
        public static final SaveSide SERVER = new SaveSide("server", "litematica.name.save_side.server");

        public static final ImmutableList<SaveSide> VALUES = ImmutableList.of(AUTO, CLIENT, SERVER);

        public SaveSide(String name, String translationKey)
        {
            super(name, translationKey);
        }
    }
}
