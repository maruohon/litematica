package fi.dy.masa.litematica.schematic.conversion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps.ConversionData;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.IProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;

public class BlockStateMapBuilder_v2
{
    private final HashMap<NBTTagCompound, JsonObject> blockStates_1_12_stateTagToBlockStateEntryJsonObject = new HashMap<>();
    private final Int2ObjectOpenHashMap<ArrayList<BlockStateEntryOld>> _1_12_idMeta_to_1_12_States = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<BlockStateEntry> _1_12_idMeta_to_1_13_State = new Int2ObjectOpenHashMap<>();

    private int changedPropertiesStates;
    private int copiedProperties;
    private int emptyStateLists;
    private int fixedStateTags;
    private int flatteningMapStates;
    private int idMetaStates;
    private int invalidFlatteningMapStates;
    private int matchedMetaStates;
    private int matchedNonMetaStates;
    private int nonMatchedStates;
    private int nonMetaStateLists;
    private int notFoundIdMeta;
    private int nullBlocks;
    private int renamedBlocksStates;
    private int reSerializationModifiedTags;
    private int totalStates112;

    /*
    private int totalStates113;
    private int totalBlocks113;
    private int fullyMatched113Blocks;
    private int partiallyMatched113Blocks;
    private int indirectlyMatched113Blocks;
    private int specialMatched113Blocks;
    private int nonMatched113Blocks;
    private int nonFlatteningMap113Blocks;
    private int flatteningMapMatched113States;
    private int fullyMatched113States;
    private int indirectlyMatched113States;
    private int specialMatched113States;
    private int nonMatched113States;
    private int nonMatched113StatesBecauseNo112TagFound;
    */

    public static void run()
    {
        BlockStateMapBuilder_v2 builder = new BlockStateMapBuilder_v2();
        builder.addStatesToMap();
    }

    private void addStatesToMap()
    {
        File fileIn = new File(FileUtils.getMinecraftDirectory(), "block_state_map_in.json");
        JsonObject root = this.read_1_12_states(fileIn);

        if (root == null)
        {
            System.out.printf("Failed to read the input block state map from '%s'\n", fileIn.getAbsolutePath());
            return;
        }

        JsonArray array_1_12_states = root.get("block_states").getAsJsonArray();

        this.resetCounters();

        this.buildIdMetaTo_1_13_TagMap(SchematicConversionMaps.CACHED_DATA);
        this.build_1_12_maps(array_1_12_states);
        this.mapStates(root);
        //this.buildNewNameToStatesMap(SchematicConversionMaps.CACHED_DATA);
    }

    @Nullable
    private JsonObject read_1_12_states(File fileIn)
    {
        if (fileIn.exists() == false || fileIn.canRead() == false)
        {
            System.out.printf("Can't read file '%s'\n", fileIn.getName());
            return null;
        }

        JsonElement el = JsonUtils.parseJsonFile(fileIn);

        if (el == null || el.isJsonObject() == false)
        {
            System.out.printf("Failed to parse JSON from file '%s'\n", fileIn.getName());
            return null;
        }

        JsonObject obj = el.getAsJsonObject();

        if (JsonUtils.hasArray(obj, "block_states") == false)
        {
            System.out.printf("No 'block_states' array in JSON file '%s'\n", fileIn.getName());
            return null;
        }

        return obj;
    }

    private void buildIdMetaTo_1_13_TagMap(ArrayList<ConversionData> conversionData)
    {
        for (ConversionData data : conversionData)
        {
            try
            {
                if (data.oldStateStrings.length > 0)
                {
                    NBTTagCompound oldStateTag = SchematicConversionMaps.getStateTagFromString(data.oldStateStrings[0]);
                    NBTTagCompound newStateTag = SchematicConversionMaps.getStateTagFromString(data.newStateString);

                    if (oldStateTag != null && newStateTag != null)
                    {
                        this.fixStateFromFlatteningMap(oldStateTag, newStateTag);
                        this._1_12_idMeta_to_1_13_State.put(data.idMeta, new BlockStateEntry(oldStateTag, newStateTag));
                        ++this.flatteningMapStates;
                    }
                    else
                    {
                        System.out.printf("Invalid entry in flattening map - old: '%s', new: '%s'\n", oldStateTag, newStateTag);
                        ++this.invalidFlatteningMapStates;
                    }
                }
            }
            catch (Exception e)
            {
                Litematica.logger.warn("Exception while building new name to state tags map", e);
            }
        }
    }

    private void build_1_12_maps(JsonArray array_1_12_states)
    {
        this._1_12_idMeta_to_1_12_States.clear();
        this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.clear();

        final int count = array_1_12_states.size();

        for (int i = 0; i < count; ++i)
        {
            JsonElement arrEl = array_1_12_states.get(i);

            if (arrEl.isJsonObject())
            {
                JsonObject objEntry = arrEl.getAsJsonObject();
                JsonObject obj112 = JsonUtils.getNestedObject(objEntry, "1.12", false);

                if (obj112 != null)
                {
                    NBTTagCompound stateTag112 = blockStateJsonObjectToTag(obj112);

                    if (stateTag112 != null)
                    {
                        int idMeta = (JsonUtils.getInteger(obj112, "id") << 4) | (JsonUtils.getInteger(obj112, "meta") & 0xF);
                        ArrayList<BlockStateEntryOld> states = this._1_12_idMeta_to_1_12_States.computeIfAbsent(idMeta, (im) -> new ArrayList<>());
                        BlockStateEntryOld entryOld = new BlockStateEntryOld(stateTag112, obj112);

                        // Always add the meta state as the first entry
                        if (JsonUtils.getBooleanOrDefault(obj112, "meta_state", false))
                        {
                            states.add(0, entryOld);
                        }
                        else
                        {
                            states.add(entryOld);
                        }

                        this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.put(stateTag112, objEntry);
                        ++this.totalStates112;
                    }
                    else
                    {
                        System.out.printf("Invalid \"1.12\" object for array element %d: '%s'\n", i, obj112.toString());
                    }
                }
                else
                {
                    System.out.printf("Missing \"1.12\" object for array element %d\n", i);
                }
            }
        }
    }

    private void mapStates(JsonObject root)
    {
        HashSet<String> removedPropNames = new HashSet<>();
        HashSet<String> addedPropNames = new HashSet<>();
        HashSet<String> identicalPropNames = new HashSet<>();
        //HashSet<String> nonMatchedBlocks = new HashSet<>();
        Object2IntOpenHashMap<IBlockState> mappedStates113Counts = new Object2IntOpenHashMap<>();
        ArrayListMultimap<IBlockState, JsonObject> newStateToOldStates = ArrayListMultimap.create();

        ArrayList<NBTTagCompound> identicalStates = new ArrayList<>();
        ArrayList<Pair<NBTTagCompound, NBTTagCompound>> statesWithIdenticalProperties = new ArrayList<>();
        ArrayList<Pair<JsonObject, ArrayList<String>>> addedProperties = new ArrayList<>();
        ArrayList<Pair<JsonObject, ArrayList<Pair<String, String>>>> removedProperties = new ArrayList<>();

        ArrayList<Pair<JsonObject, ArrayList<Pair<String, String>>>> copiedProperties = new ArrayList<>();
        ArrayList<Pair<JsonObject, ArrayList<Pair<String, String>>>> invalidProperties = new ArrayList<>();

        IntArrayList ids = new IntArrayList(this._1_12_idMeta_to_1_12_States.keySet());
        Collections.sort(ids);

        for (int idMeta : ids)
        {
            ++this.idMetaStates;

            ArrayList<BlockStateEntryOld> oldStates = this._1_12_idMeta_to_1_12_States.get(idMeta);

            if (oldStates == null || oldStates.isEmpty())
            {
                System.out.printf("No states for idMeta: %d\n", idMeta);
                ++this.emptyStateLists;
                continue;
            }

            // Get the new state for the meta state
            BlockStateEntry entryNew = this._1_12_idMeta_to_1_13_State.get(idMeta);

            if (entryNew == null)
            {
                System.out.printf("New state not found for idMeta %d\n", idMeta);
                ++this.notFoundIdMeta;
                continue;
            }

            // Get the first entry, which should be the meta state
            BlockStateEntryOld firstOldEntry = oldStates.get(0);
            NBTTagCompound stateTag112 = firstOldEntry.oldStateTag;
            NBTTagCompound stateTag113Orig = entryNew.newStateTag;

            // Account for post-flattening-map changes
            NBTTagCompound stateTag113 = StateTagFixers_1_12_to_1_13_2.fixStateTag(stateTag113Orig.copy());

            if (stateTag113.equals(stateTag113Orig) == false)
            {
                ++this.fixedStateTags;
            }

            String oldName = stateTag112.getString("Name");
            String newName = stateTag113.getString("Name");

            newName = SchematicConversionMaps.get_1_13_2_NameForIdMeta(idMeta, newName);
            stateTag113.putString("Name", newName);

            boolean renamed = oldName.equals(newName) == false;
            boolean isMetaState = JsonUtils.getBooleanOrDefault(firstOldEntry.blockStateEntry, "meta_state", false);
            Block block = null;

            try
            {
                ResourceLocation rl = new ResourceLocation(newName);
                block = IRegistry.BLOCK.get(rl);
            }
            catch (Exception e)
            {
                System.out.printf("Invalid new block name: '%s' for old block '%s'\n", newName, oldName);
                ++this.nullBlocks;
                continue;
            }

            if (block == null)
            {
                System.out.printf("Got null block for new name: '%s'\n", newName);
                ++this.nullBlocks;
                continue;
            }

            if (isMetaState == false)
            {
                //System.out.printf("Not meta state: idMeta: %d => '%s'\n", idMeta, entryOld.blockStateEntry);
                ++this.nonMetaStateLists;
            }

            if (renamed)
            {
                ++this.renamedBlocksStates;
            }

            if (stateTag112.getCompound("Properties").equals(stateTag113.getCompound("Properties")) == false)
            {
                ++this.changedPropertiesStates;
            }

            for (BlockStateEntryOld oldStateEntry : oldStates)
            {
                NBTTagCompound stateTagOld = oldStateEntry.oldStateTag;
                NBTTagCompound stateTagNew = stateTag113.copy();
                NBTTagCompound propsNew = stateTagNew.getCompound("Properties");
                NBTTagCompound propsOld = stateTagOld.getCompound("Properties");
                boolean flagCopiedProperties = false;

                fixState(stateTagOld, stateTagNew);

                identicalPropNames.clear();
                getIdenticalPropertyKeys(stateTagNew, stateTagOld, identicalPropNames);

                if (identicalPropNames.isEmpty() == false)
                {
                    for (String propName : identicalPropNames)
                    {
                        String propValue = propsOld.getString(propName);

                        if (isValidPropertyValue(block, propName, propValue))
                        {
                            ArrayList<Pair<String, String>> list = new ArrayList<>();
                            propsNew.putString(propName, propValue);
                            flagCopiedProperties = true;
                            list.add(Pair.of(propName, propValue));
                            copiedProperties.add(Pair.of(oldStateEntry.blockStateEntry, list));
                        }
                        else
                        {
                            ArrayList<Pair<String, String>> list = new ArrayList<>();
                            list.add(Pair.of(propName, propValue));
                            invalidProperties.add(Pair.of(oldStateEntry.blockStateEntry, list));
                        }
                    }
                }

                // Convert the tag to a block state and back to include all the current properties in the tag
                IBlockState state = NBTUtil.readBlockState(stateTagNew);

                /*
                if (oldEntry.oldStateTag.getString("Name").equals("minecraft:acacia_door"))
                {
                    System.out.printf("idMeta: %d, meta_state: %s, copied: %s\n  - old_state: %s\n  - new state: %s\n", idMeta, isMetaState, flagCopiedProperties, oldEntry.blockStateEntry, state);
                }
                */

                // Valid block state entry
                if (state.getBlock() != Blocks.AIR || stateTagNew.getString("Name").equals("minecraft:air"))
                {
                    isMetaState = JsonUtils.getBooleanOrDefault(oldStateEntry.blockStateEntry, "meta_state", false);

                    if (isMetaState)
                    {
                        ++this.matchedMetaStates;
                    }
                    else
                    {
                        ++this.matchedNonMetaStates;
                    }

                    if (flagCopiedProperties)
                    {
                        ++this.copiedProperties;
                    }

                    newStateToOldStates.put(state, oldStateEntry.blockStateEntry);
                    mappedStates113Counts.addTo(state, 1);
                    NBTTagCompound stateTagNewReSerialized = NBTUtil.writeBlockState(state);

                    if (stateTagNewReSerialized.equals(stateTagOld))
                    {
                        identicalStates.add(stateTagNewReSerialized);
                    }
                    else if (stateTagNewReSerialized.getCompound("Properties").equals(propsOld))
                    {
                        statesWithIdenticalProperties.add(Pair.of(stateTagOld, stateTagNewReSerialized));
                    }

                    removedPropNames.clear();
                    addedPropNames.clear();
                    getExtraProperties(stateTagOld, stateTagNewReSerialized, removedPropNames);
                    getExtraProperties(stateTagNewReSerialized, stateTagOld, addedPropNames);

                    if (addedPropNames.isEmpty() == false)
                    {
                        ArrayList<String> list = new ArrayList<>();
                        list.addAll(addedPropNames);
                        Collections.sort(list);
                        addedProperties.add(Pair.of(oldStateEntry.blockStateEntry, list));
                    }

                    if (removedPropNames.isEmpty() == false)
                    {
                        ArrayList<Pair<String, String>> list = new ArrayList<>();

                        for (String propName : removedPropNames)
                        {
                            list.add(Pair.of(propName, propsOld.getString(propName)));
                        }

                        Collections.sort(list);
                        removedProperties.add(Pair.of(oldStateEntry.blockStateEntry, list));
                    }

                    JsonObject objEntry = this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.get(stateTagOld);
                    objEntry.add("1.13", BlockStateMapBuilder.blockStateTagToJsonObject(stateTagNewReSerialized));

                    if (stateTagNewReSerialized.equals(stateTagNew) == false)
                    {
                        ++this.reSerializationModifiedTags;
                    }
                }
                else
                {
                    System.out.printf("tag failed to convert to a block state and back: %s\n", stateTagNew);
                    ++this.nonMatchedStates;
                }
            }
        }

        File fileOut = new File(FileUtils.getMinecraftDirectory(), "block_state_map_out_v2.json");
        BlockStateMapBuilder.dumpDataToFile(fileOut, BlockStateMapBuilder.getCustomFormattedJson(root));

        int countMultiMapped112 = 0;
        int countMultiMapped113 = 0;
        int countSingleMapped = 0;
        ArrayList<IBlockState> multiMappedStates = new ArrayList<>();

        for (Map.Entry<IBlockState, Integer> entry : mappedStates113Counts.object2IntEntrySet())
        {
            int count = entry.getValue();

            if (count > 1)
            {
                multiMappedStates.add(entry.getKey());
                countMultiMapped112 += 1;
                countMultiMapped113 += count;
            }
            else if (count == 1)
            {
                ++countSingleMapped;
            }
        }

        /*
        if (countMultiMapped > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("1.13 multi-mapped states:\n");
            System.out.printf("===============================\n");

            multiMappedStates.sort((s1, s2) -> s1.toString().compareTo(s2.toString()));

            for (IBlockState st : multiMappedStates)
            {
                System.out.printf(" - uses: %d, state '%s'\n", mappedStates113Counts.getInt(st), st.toString());

                for (JsonObject obj : newStateToOldStates.get(st))
                {
                    System.out.printf("   > 1.12 state: '%s'\n", obj);
                }
            }

            System.out.printf("\n");
        }
        */

        if (identicalStates.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf(" Identical states (%d):\n", identicalStates.size());
            System.out.printf("===============================\n");

            for (NBTTagCompound stateTag : identicalStates)
            {
                if (stateTag.contains("Properties") == false)
                    System.out.printf("%s\n", stateTag.getString("Name"));
            }

            for (NBTTagCompound stateTag : identicalStates)
            {
                if (stateTag.contains("Properties"))
                {
                    NBTTagCompound props = stateTag.getCompound("Properties");
                    ArrayList<String> list = new ArrayList<>();
                    for (String key : props.keySet())
                    {
                        list.add(key + "=" + props.getString(key));
                    }
                    Collections.sort(list);
                    System.out.printf("%-40s %s\n", stateTag.getString("Name"), String.join(", ", list));
                }
            }

            System.out.printf("\n");
        }

        if (statesWithIdenticalProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf(" Identical properties (renamed block) (%d):\n", statesWithIdenticalProperties.size());
            System.out.printf("===============================\n");

            for (Pair<NBTTagCompound, NBTTagCompound> pair : statesWithIdenticalProperties)
            {
                NBTTagCompound old = pair.getLeft();
                NBTTagCompound nnw = pair.getRight();
                NBTTagCompound props = old.getCompound("Properties");

                if (props.keySet().isEmpty() == false)
                {
                    continue;
                }

                System.out.printf("%-40s %-40s\n", old.getString("Name"), nnw.getString("Name"));
            }

            for (Pair<NBTTagCompound, NBTTagCompound> pair : statesWithIdenticalProperties)
            {
                NBTTagCompound old = pair.getLeft();
                NBTTagCompound nnw = pair.getRight();
                NBTTagCompound props = old.getCompound("Properties");

                if (props.keySet().isEmpty())
                {
                    continue;
                }

                ArrayList<String> list = new ArrayList<>();

                for (String key : props.keySet())
                {
                    list.add(key + "=" + props.getString(key));
                }

                Collections.sort(list);
                System.out.printf("%-40s %-40s %s\n", old.getString("Name"), nnw.getString("Name"), String.join(", ", list));
            }

            System.out.printf("\n");
        }

        if (addedProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf(" States with new properties (%d):\n", addedProperties.size());
            System.out.printf("===============================\n");

            for (Pair<JsonObject, ArrayList<String>> pair : addedProperties)
            {
                JsonObject oldState = pair.getLeft();
                ArrayList<String> newProps = pair.getRight();
                System.out.printf("%-160s added: %s\n", oldState, newProps);
            }

            System.out.printf("\n");
        }

        if (removedProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf(" States with removed properties (%d):\n", removedProperties.size());
            System.out.printf("===============================\n");

            for (Pair<JsonObject, ArrayList<Pair<String, String>>> pair : removedProperties)
            {
                JsonObject oldState = pair.getLeft();
                ArrayList<Pair<String, String>> remProps = pair.getRight();
                Collections.sort(remProps, (p1, p2) -> p1.getLeft().compareTo(p2.getLeft()));
                System.out.printf("%-140s removed: %s\n", oldState, remProps);
            }

            System.out.printf("\n");
        }

        /*
        if (statesWithIdenticalProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("States with identical properties:\n");
            System.out.printf("===============================\n");

            for (Pair<JsonObject, String> val : statesWithIdenticalProperties)
            {
                System.out.printf("old '%s' => new: '%s'\n", val.getLeft(), val.getRight());
            }

            System.out.printf("\n");
        }

        if (addedProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("States with new properties (%d):\n", addedProperties.size());
            System.out.printf("===============================\n");

            for (JsonObject oldState : addedProperties.keySet())
            {
                System.out.printf("old state '%s' - added properties: %s\n", oldState, addedProperties.get(oldState));
            }

            System.out.printf("\n");
        }

        if (removedProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("States with removed properties (%d):\n", removedProperties.size());
            System.out.printf("===============================\n");

            for (JsonObject oldState : removedProperties.keySet())
            {
                System.out.printf("old state '%s' - removed properties: %s\n", oldState, removedProperties.get(oldState));
            }

            System.out.printf("\n");
        }

        if (copiedProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("States with copied properties (%d):\n", copiedProperties.size());
            System.out.printf("===============================\n");

            for (JsonObject oldState : copiedProperties.keySet())
            {
                System.out.printf("old state '%s' - copied properties: %s\n", oldState, copiedProperties.get(oldState));
            }

            System.out.printf("\n");
        }

        if (invalidProperties.size() > 0)
        {
            System.out.printf("\n");
            System.out.printf("===============================\n");
            System.out.printf("States with invalid properties (%d):\n", invalidProperties.size());
            System.out.printf("===============================\n");

            for (JsonObject oldState : invalidProperties.keySet())
            {
                System.out.printf("old state '%s' - invalid properties: %s\n", oldState, invalidProperties.get(oldState));
            }

            System.out.printf("\n");
        }
        */

        System.out.printf(" > changedPropertiesStates: %d\n", this.changedPropertiesStates);
        System.out.printf(" > copiedProperties: %d\n", this.copiedProperties);
        System.out.printf(" > countMultiMapped112: %d\n", countMultiMapped112);
        System.out.printf(" > countMultiMapped113: %d\n", countMultiMapped113);
        System.out.printf(" > countSingleMapped: %d\n", countSingleMapped);
        System.out.printf(" > emptyStateLists: %d\n", this.emptyStateLists);
        System.out.printf(" > fixedStateTags: %d\n", this.fixedStateTags);
        System.out.printf(" > flatteningMapStates: %d\n", this.flatteningMapStates);
        System.out.printf(" > idMetaStates: %d\n", this.idMetaStates);
        System.out.printf(" > invalidFlatteningMapStates: %d\n", this.invalidFlatteningMapStates);
        System.out.printf(" > matchedMetaStates: %d\n", this.matchedMetaStates);
        System.out.printf(" > matchedNonMetaStates: %d\n", this.matchedNonMetaStates);
        System.out.printf(" > nonMatchedStates: %d\n", this.nonMatchedStates);
        System.out.printf(" > nonMetaStateLists: %d\n", this.nonMetaStateLists);
        System.out.printf(" > notFoundIdMeta: %d\n", this.notFoundIdMeta);
        System.out.printf(" > nullBlocks: %d\n", this.nullBlocks);
        System.out.printf(" > renamedBlocksStates: %d\n", this.renamedBlocksStates);
        System.out.printf(" > reSerializationModifiedTags: %d\n", this.reSerializationModifiedTags);
        System.out.printf(" > totalStates112: %d\n", this.totalStates112);
        System.out.printf(" > total matched states: %d\n", this.matchedMetaStates + this.matchedNonMetaStates);
        System.out.printf(" > mapped 1.13 states: %d\n", mappedStates113Counts.size());

        /*
        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.12 Non-matched blocks:\n");
        System.out.printf("===============================\n");
        ArrayList<String> list = new ArrayList<>(nonMatchedBlocks);
        Collections.sort(list);
        list.forEach((str) -> System.out.printf("%s\n", str));
        */
    }

    /**
     * Gets the property names in <b>stateTag</b> that are not in <b>stateTagRef</b> and adds them to the set <b>propNamesOut</b>
     * @param stateTag
     * @param stateTagRef
     */
    public static void getExtraProperties(NBTTagCompound stateTag, NBTTagCompound stateTagRef, HashSet<String> propNamesOut)
    {
        NBTTagCompound props = stateTag.getCompound("Properties");
        NBTTagCompound propsRef = stateTagRef.getCompound("Properties");
        Set<String> keysRef = propsRef.keySet();

        for (String key : props.keySet())
        {
            if (keysRef.contains(key) == false)
            {
                propNamesOut.add(key);
            }
        }
    }

    public static void getIdenticalPropertyKeys(NBTTagCompound stateTag, NBTTagCompound stateTagRef, HashSet<String> propNamesOut)
    {
        NBTTagCompound props = stateTag.getCompound("Properties");
        NBTTagCompound propsRef = stateTagRef.getCompound("Properties");

        for (String key : propsRef.keySet())
        {
            if (props.contains(key) &&
                propsRef.contains(key)
                // && props.get(key).equals(propsRef.get(key))
                )
            {
                propNamesOut.add(key);
            }
        }
    }

    public static <T extends Comparable<T>> boolean isValidPropertyValue(Block block, String propName, String propValue)
    {
        IProperty<?> prop = block.getStateContainer().getProperty(propName);
        return prop != null && prop.parseValue(propValue).isPresent();
    }

    /**
     * Fix post-flattening renames (ie. the "new" names in the flattening map are actually not what 1.13+ uses)
     * @param oldStateTag
     * @param newStateTag
     */
    private void fixStateFromFlatteningMap(NBTTagCompound oldStateTag, NBTTagCompound newStateTag)
    {
        if (matchesName(newStateTag, "minecraft:portal"))
        {
            newStateTag.putString("Name", "minecraft:nether_portal");
        }
        else if (matchesName(newStateTag, "minecraft:mob_spawner"))
        {
            newStateTag.putString("Name", "minecraft:spawner");
        }
        else if (matchesName(newStateTag, "minecraft:melon_block"))
        {
            newStateTag.putString("Name", "minecraft:melon");
        }
        else if (newStateTag.getString("Name").endsWith("_bark"))
        {
            newStateTag.putString("Name", newStateTag.getString("Name").replace("_bark", "_wood"));
        }
        else if (oldStateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propsOld = oldStateTag.getCompound("Properties");

            // Fix property value case mismatch
            /*if (this.matchesNameAndHasProperty(oldStateTag, "minecraft:structure_block", "mode"))
            {
                NBTTagCompound propsNew = newStateTag.getCompound("Properties");
                propsOld.putString("mode", propsOld.getString("mode").toUpperCase());
                propsNew.putString("mode", propsNew.getString("mode").toUpperCase());
            }
            // Fix pre-flattening property value changes for lightBlue => light_blue blocks
            else */
            if (propsOld.contains("color", Constants.NBT.TAG_STRING) &&
                     propsOld.getString("color").equals("light_blue"))
            {
                propsOld.putString("color", "lightBlue");
            }
        }
    }

    private static void fixState(NBTTagCompound oldStateTag, NBTTagCompound newStateTag)
    {
        if (matchesName(oldStateTag, "minecraft:leaves") || matchesName(oldStateTag, "minecraft:leaves2"))
        {
            NBTTagCompound propTagOld = oldStateTag.getCompound("Properties");
            NBTTagCompound propTagNew = newStateTag.getCompound("Properties");
            String oldValue = propTagOld.getString("decayable");
            propTagNew.putString("persistent", oldValue.equals("true") ? "false" : "true");
            propTagNew.remove("check_decay");
            propTagNew.remove("decayable");
        }
        else if (matchesName(oldStateTag, "minecraft:pumpkin_stem") || matchesName(oldStateTag, "minecraft:melon_stem"))
        {
            NBTTagCompound propTagNew = newStateTag.getCompound("Properties");

            if (hasPropertyWithValue(oldStateTag, "facing", "up"))
            {
                propTagNew.remove("facing");
            }
            else
            {
                String name = oldStateTag.getString("Name");

                if (name.equals("minecraft:pumpkin_stem"))
                {
                    name = "minecraft:attached_pumpkin_stem";
                }
                else
                {
                    name = "minecraft:attached_melon_stem";
                }

                propTagNew.putString("Name", name);
                propTagNew.remove("age");
            }
        }
        else if (matchesName(oldStateTag, "minecraft:double_plant") && hasPropertyWithValue(oldStateTag, "half", "upper"))
        {
            NBTTagCompound propTagOld = oldStateTag.getCompound("Properties");
            String variant = propTagOld.getString("variant");
            String name = "minecraft:";

            switch (variant)
            {
                case "double_grass":    name += "tall_grass"; break;
                case "double_fern":     name += "large_fern"; break;
                case "double_rose":     name += "rose_bush"; break;
                case "paeonia":         name += "peony"; break;
                case "syringa":         name += "lilac"; break;
                default:                name += variant;
            }

            newStateTag.putString("Name", name);
        }
        else if (matchesName(oldStateTag, "minecraft:flower_pot"))
        {
            String contents = oldStateTag.getCompound("Properties").getString("contents");
            String name = "";

            switch (contents)
            {
                case "acacia_sapling":      name = "minecraft:potted_acacia_sapling"; break;
                case "allium":              name = "minecraft:potted_allium"; break;
                case "birch_sapling":       name = "minecraft:potted_birch_sapling"; break;
                case "blue_orchid":         name = "minecraft:potted_blue_orchid"; break;
                case "cactus":              name = "minecraft:potted_cactus"; break;
                case "dandelion":           name = "minecraft:potted_dandelion"; break;
                case "dark_oak_sapling":    name = "minecraft:potted_dark_oak_sapling"; break;
                case "dead_bush":           name = "minecraft:potted_dead_bush"; break;
                case "empty":               name = "minecraft:flower_pot"; break;
                case "fern":                name = "minecraft:potted_fern"; break;
                case "houstonia":           name = "minecraft:potted_azure_bluet"; break;
                case "jungle_sapling":      name = "minecraft:potted_jungle_sapling"; break;
                case "mushroom_brown":      name = "minecraft:potted_brown_mushroom"; break;
                case "mushroom_red":        name = "minecraft:potted_red_mushroom"; break;
                case "oak_sapling":         name = "minecraft:potted_oak_sapling"; break;
                case "orange_tulip":        name = "minecraft:potted_orange_tulip"; break;
                case "oxeye_daisy":         name = "minecraft:potted_oxeye_daisy"; break;
                case "pink_tulip":          name = "minecraft:potted_pink_tulip"; break;
                case "red_tulip":           name = "minecraft:potted_red_tulip"; break;
                case "rose":                name = "minecraft:potted_poppy"; break;
                case "spruce_sapling":      name = "minecraft:potted_spruce_sapling"; break;
                case "white_tulip":         name = "minecraft:potted_white_tulip"; break;
            }

            newStateTag.putString("Name", name);
            newStateTag.remove("Properties");
        }
    }

    private static boolean matchesName(NBTTagCompound stateTag, String blockName)
    {
        return stateTag.getString("Name").equals(blockName);
    }

    private static boolean hasPropertyWithValue(NBTTagCompound stateTag, String propName, String propValue)
    {
        if (stateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propTag = stateTag.getCompound("Properties");

            return propTag.contains(propName, Constants.NBT.TAG_STRING) &&
                   propTag.getString(propName).equals(propValue);
        }

        return false;
    }

    private boolean matchesNameAndHasProperty(NBTTagCompound stateTag, String blockName, String propName)
    {
        if (stateTag.getString("Name").equals(blockName) &&
            stateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propTag = stateTag.getCompound("Properties");
            return propTag.contains(propName, Constants.NBT.TAG_STRING);
        }

        return false;
    }

    private void resetCounters()
    {
        this.changedPropertiesStates = 0;
        this.copiedProperties = 0;
        this.emptyStateLists = 0;
        this.fixedStateTags = 0;
        this.flatteningMapStates = 0;
        this.idMetaStates = 0;
        this.invalidFlatteningMapStates = 0;
        this.matchedMetaStates = 0;
        this.matchedNonMetaStates = 0;
        this.nonMatchedStates = 0;
        this.nonMetaStateLists = 0;
        this.notFoundIdMeta = 0;
        this.nullBlocks = 0;
        this.renamedBlocksStates = 0;
        this.reSerializationModifiedTags = 0;
        this.totalStates112 = 0;

        /*
        this.fullyMatchedBlocks.clear();
        this.partiallyMatchedBlocks.clear();
        this.indirectlyMatchedBlocks.clear();
        this.specialMatchedBlocks.clear();
        this.nonMatchedBlocks.clear();
        */
    }

    @Nullable
    public static NBTTagCompound blockStateJsonObjectToTag(JsonObject obj)
    {
        if (obj.has("block"))
        {
            NBTTagCompound stateTag = new NBTTagCompound();
            stateTag.putString("Name", obj.get("block").getAsString());

            JsonObject props = JsonUtils.getNestedObject(obj, "properties", false);

            if (props != null)
            {
                NBTTagCompound propsTag = new NBTTagCompound();

                for (Map.Entry<String, JsonElement> entry : props.entrySet())
                {
                    propsTag.putString(entry.getKey(), entry.getValue().getAsString());
                }

                stateTag.put("Properties", propsTag);
            }

            return stateTag;
        }

        return null;
    }

    private static class BlockStateEntry
    {
        public final NBTTagCompound oldStateTag;
        public final NBTTagCompound newStateTag;

        public BlockStateEntry(NBTTagCompound oldStateTag, NBTTagCompound newStateTag)
        {
            this.oldStateTag = oldStateTag;
            this.newStateTag = newStateTag;
        }
    }

    private static class BlockStateEntryOld
    {
        public final NBTTagCompound oldStateTag;
        public final JsonObject blockStateEntry;

        public BlockStateEntryOld(NBTTagCompound oldStateTag, JsonObject blockStateEntry)
        {
            this.oldStateTag = oldStateTag;
            this.blockStateEntry = blockStateEntry;
        }
    }
}
