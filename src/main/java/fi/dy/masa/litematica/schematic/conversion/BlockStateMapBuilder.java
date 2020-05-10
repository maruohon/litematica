package fi.dy.masa.litematica.schematic.conversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps.ConversionData;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.state.IProperty;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;

public class BlockStateMapBuilder
{
    private final ListMultimap<String, BlockStateEntry> flatteningMap_1_13_NameToStateTags = MultimapBuilder.hashKeys().arrayListValues().build();
    private final HashMap<NBTTagCompound, JsonObject> blockStates_1_12_stateTagToBlockStateEntryJsonObject = new HashMap<>();
    private final ListMultimap<String, BlockStateEntryOld> blockStates_1_12_oldNameToOldBlockStateEntries = MultimapBuilder.hashKeys().arrayListValues().build();
    private JsonArray newStatesArray;
    private int totalStates112;
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
    private final List<String> fullyMatchedBlocks = new ArrayList<>();
    private final List<String> partiallyMatchedBlocks = new ArrayList<>();
    private final List<String> indirectlyMatchedBlocks = new ArrayList<>();
    private final List<String> specialMatchedBlocks = new ArrayList<>();
    private final List<String> nonMatchedBlocks = new ArrayList<>();

    public static void run()
    {
        BlockStateMapBuilder builder = new BlockStateMapBuilder();
        builder.addStatesToMap();
    }

    private void resetCounters()
    {
        this.totalStates112 = 0;
        this.totalStates113 = 0;
        this.totalBlocks113 = 0;
        this.fullyMatched113Blocks = 0;
        this.partiallyMatched113Blocks = 0;
        this.indirectlyMatched113Blocks = 0;
        this.specialMatched113Blocks = 0;
        this.nonMatched113Blocks = 0;
        this.nonFlatteningMap113Blocks = 0;
        this.flatteningMapMatched113States = 0;
        this.fullyMatched113States = 0;
        this.indirectlyMatched113States = 0;
        this.specialMatched113States = 0;
        this.nonMatched113States = 0;
        this.nonMatched113StatesBecauseNo112TagFound = 0;

        this.fullyMatchedBlocks.clear();
        this.partiallyMatchedBlocks.clear();
        this.indirectlyMatchedBlocks.clear();
        this.specialMatchedBlocks.clear();
        this.nonMatchedBlocks.clear();
    }

    @Nullable
    private JsonObject read_1_12_mappings(File fileIn)
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

    private void buildNewNameToStatesMap(ArrayList<ConversionData> conversionData)
    {
        this.flatteningMap_1_13_NameToStateTags.clear();

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

                        this.flatteningMap_1_13_NameToStateTags.put(newStateTag.getString("Name"), new BlockStateEntry(oldStateTag, newStateTag));
                    }
                }
            }
            catch (Exception e)
            {
                Litematica.logger.warn("Exception while building new name to state tags map", e);
            }
        }
    }

    /**
     * Fix post-flattening renames (ie. the "new" names in the flattening map are actually not what 1.13+ uses)
     * @param oldStateTag
     * @param newStateTag
     */
    private void fixStateFromFlatteningMap(NBTTagCompound oldStateTag, NBTTagCompound newStateTag)
    {
        if (this.matchesName(newStateTag, "minecraft:portal"))
        {
            newStateTag.putString("Name", "minecraft:nether_portal");
        }
        else if (this.matchesName(newStateTag, "minecraft:mob_spawner"))
        {
            newStateTag.putString("Name", "minecraft:spawner");
        }
        else if (this.matchesName(newStateTag, "minecraft:melon_block"))
        {
            newStateTag.putString("Name", "minecraft:melon");
        }
        else if (oldStateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propsOld = oldStateTag.getCompound("Properties");

            // Fix property value case mismatch
            if (this.matchesNameAndHasProperty(oldStateTag, "minecraft:structure_block", "mode"))
            {
                NBTTagCompound propsNew = newStateTag.getCompound("Properties");
                propsOld.putString("mode", propsOld.getString("mode").toUpperCase());
                propsNew.putString("mode", propsNew.getString("mode").toUpperCase());
            }
            // Fix pre-flattening property value changes for lightBlue => light_blue blocks
            else if (propsOld.contains("color", Constants.NBT.TAG_STRING) &&
                     propsOld.getString("color").equals("light_blue"))
            {
                propsOld.putString("color", "lightBlue");
            }
        }
    }

    private boolean matchesName(NBTTagCompound stateTag, String blockName)
    {
        return stateTag.getString("Name").equals(blockName);
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

    private boolean matchesNameAndProperty(NBTTagCompound stateTag, String blockName, String propName, String propValue)
    {
        if (stateTag.getString("Name").equals(blockName) &&
            stateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propTag = stateTag.getCompound("Properties");
            return propTag.contains(propName, Constants.NBT.TAG_STRING) && propTag.getString(propValue).equals(propValue);
        }

        return false;
    }

    private void build_1_12_BlockStateTagToBlockStateEntryJsonObjectMap(JsonArray array_1_12_states)
    {
        this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.clear();
        this.blockStates_1_12_oldNameToOldBlockStateEntries.clear();

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
                    NBTTagCompound stateTag = blockStateJsonObjectToTag(obj112);

                    if (stateTag != null)
                    {
                        this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.put(stateTag, objEntry);
                        this.blockStates_1_12_oldNameToOldBlockStateEntries.put(stateTag.getString("Name"), new BlockStateEntryOld(stateTag, objEntry));
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

    private void addBlockToMap(ResourceLocation id, Block block)
    {
        String blockName = id.toString();
        List<BlockStateEntry> stateTagsFor113Name = this.flatteningMap_1_13_NameToStateTags.get(blockName);
        boolean foundInFlatteningMap = stateTagsFor113Name.isEmpty() == false;

        ++this.totalBlocks113;

        if (foundInFlatteningMap == false)
        {
            //System.out.printf("1.13 block name not found in flattening map: '%s'\n", blockName);
            ++this.nonFlatteningMap113Blocks;
        }

        boolean fullyMatchedBlock = true;
        boolean partiallyMatchedBlock = false;
        int matchedStates = 0;
        int indirectlyMatchedStates = 0;
        int specialMatchedStates = 0;
        int nonMatchedStatesCount = 0;
        int totalStates = block.getStateContainer().getValidStates().size();
        List<IBlockState> nonMatchedStates = new ArrayList<>();

        for (IBlockState state : block.getStateContainer().getValidStates())
        {
            ++this.totalStates113;

            JsonObject objStateEntry = null;
            NBTTagCompound stateTag = blockStateToTag(state, blockName);

            if (foundInFlatteningMap)
            {
                for (BlockStateEntry entry : stateTagsFor113Name)
                {
                    if (Objects.equals(stateTag, entry.newStateTag))
                    {
                        objStateEntry = this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.get(entry.oldStateTag);

                        if (objStateEntry == null)
                        {
                            System.out.printf("Existing entry not found for old state tag: old: '%s', new: '%s'\n", entry.oldStateTag.toString(), entry.newStateTag.toString());
                            ++this.nonMatched113StatesBecauseNo112TagFound;
                        }

                        ++this.flatteningMapMatched113States;

                        break;
                    }
                }
            }

            JsonObject obj113 = blockStateTagToJsonObject(stateTag);

            // fully matched the 1.13 state to a 1.12 state
            if (objStateEntry != null)
            {
                objStateEntry.add("1.13", obj113);
                ++this.fullyMatched113States;
                partiallyMatchedBlock = true;
                ++matchedStates;
            }
            // non-matched state, add it as a new entry
            else
            {
                JsonObject newEntry = new JsonObject();
                newEntry.add("1.13", obj113);
                this.newStatesArray.add(newEntry);
                ++nonMatchedStatesCount;
                fullyMatchedBlock = false;
                nonMatchedStates.add(state);
            }
        }

        // Matched some states, but not all of them.
        // This likely means that only one state per metadata value was matched against the flattening map data.
        // So let's just try to match the properties directly against the states of this block in 1.12.
        if (matchedStates > 0 && fullyMatchedBlock == false)
        {
            // This list can't be empty if there were matched states against the flattening map in the first place
            String name112 = stateTagsFor113Name.get(0).oldStateTag.getString("Name");

            for (IBlockState state : nonMatchedStates)
            {
                NBTTagCompound stateTag113 = blockStateToTag(state, blockName);
                NBTTagCompound stateTag112 = blockStateToTag(state, name112);
                JsonObject objStateEntry = this.blockStates_1_12_stateTagToBlockStateEntryJsonObject.get(stateTag112);

                // Found an entry using the old 1.12 block name, and the current set of properties/values directly
                if (objStateEntry != null)
                {
                    JsonObject obj113 = blockStateTagToJsonObject(stateTag113);
                    objStateEntry.add("1.13", obj113);
                    ++indirectlyMatchedStates;
                    --nonMatchedStatesCount;
                }
            }
        }

        if (foundInFlatteningMap && fullyMatchedBlock == false && nonMatchedStatesCount > 0)
        {
            // This list can't be empty if there were matched states against the flattening map in the first place
            String name112 = stateTagsFor113Name.get(0).oldStateTag.getString("Name");
            List<BlockStateEntryOld> oldEntries = this.blockStates_1_12_oldNameToOldBlockStateEntries.get(name112);

            if (oldEntries.isEmpty() == false)
            {
                for (IBlockState state : nonMatchedStates)
                {
                    NBTTagCompound stateTag113 = blockStateToTag(state, blockName);

                    for (BlockStateEntryOld entry : oldEntries)
                    {
                        if (specialMatchesEntry(stateTag113, entry.oldStateTag))
                        {
                            JsonObject obj113 = blockStateTagToJsonObject(stateTag113);
                            entry.blockStateEntry.add("1.13", obj113);
                            ++specialMatchedStates;
                            --nonMatchedStatesCount;
                            break;
                        }
                    }
                }
            }
        }

        this.indirectlyMatched113States += indirectlyMatchedStates;
        this.specialMatched113States += specialMatchedStates;
        this.nonMatched113States += nonMatchedStatesCount;

        int indirectTotal = matchedStates + indirectlyMatchedStates;

        if (fullyMatchedBlock)
        {
            //System.out.printf("Fully matched block: %s\n", blockName);
            ++this.fullyMatched113Blocks;
            this.fullyMatchedBlocks.add(blockName + String.format(" [%d / %d]", matchedStates, totalStates));
        }
        else if (indirectlyMatchedStates > 0 && nonMatchedStatesCount == 0)
        {
            //System.out.printf("Indirectly matched block: %s\n", blockName);
            ++this.indirectlyMatched113Blocks;
            this.indirectlyMatchedBlocks.add(blockName + String.format(" [%d / %d] + %d => [%d / %d]", matchedStates, totalStates, indirectlyMatchedStates, indirectTotal, totalStates));
        }
        else if (specialMatchedStates > 0 && nonMatchedStatesCount == 0)
        {
            //System.out.printf("Special matched block: %s\n", blockName);
            ++this.specialMatched113Blocks;
            int specialTotal = indirectTotal + specialMatchedStates;
            this.specialMatchedBlocks.add(blockName + String.format(" [%d / %d] + %d ind + %d spec => [%d / %d]", matchedStates, totalStates, indirectlyMatchedStates, specialMatchedStates, specialTotal, totalStates));
        }
        else if (partiallyMatchedBlock || indirectlyMatchedStates > 0 || specialMatchedStates > 0)
        {
            //System.out.printf("Partially matched block: %s\n", blockName);
            ++this.partiallyMatched113Blocks;
            int partialTotal = matchedStates + indirectlyMatchedStates + specialMatchedStates;
            this.partiallyMatchedBlocks.add(blockName + String.format(" [M %d + I %d + S %d = %d / %d]", matchedStates, indirectlyMatchedStates, specialMatchedStates, partialTotal, totalStates));
        }
        else
        {
            //System.out.printf("Totally unmatched block: %s\n", blockName);
            ++this.nonMatched113Blocks;
            this.nonMatchedBlocks.add(blockName + String.format(" [%d / %d]", matchedStates, totalStates));
        }
    }

    public static boolean specialMatchesEntry(NBTTagCompound stateTagNew, NBTTagCompound stateTagOld)
    {
        if (stateTagNew.contains("Properties", Constants.NBT.TAG_COMPOUND) &&
            stateTagOld.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propsNew = stateTagNew.getCompound("Properties");
            NBTTagCompound propsOld = stateTagOld.getCompound("Properties");

            String blockNameNew = stateTagNew.getString("Name");

            if (blockNameNew.equals("minecraft:chest") || blockNameNew.equals("minecraft:trapped_chest"))
            {
                return propsNew.getString("waterlogged").equals("false") &&
                       propsNew.getString("type").equals("SINGLE") &&
                       propsNew.getString("facing").equals(propsOld.getString("facing"));
            }

            String blockNameOld = stateTagOld.getString("Name");

            if (blockNameOld.equals("minecraft:leaves") || blockNameOld.equals("minecraft:leaves2"))
            {
                //if (blockNameNew.equals("minecraft:acacia_leaves") && propsNew.getString("distance").equals("7")) System.out.printf("new: %s, old: %s\n\n", stateTagNew, stateTagOld);
                return blockNameNew.equals("minecraft:" + propsOld.getString("variant") + "_leaves") &&
                       propsNew.getString("distance").equals("1") &&
                       ! propsNew.getString("persistent").equals(propsOld.getString("check_decay"));
            }

            for (String propNameNew : propsNew.keySet())
            {
                if (propNameNew.equals("waterlogged") == false &&
                        (propsOld.contains(propNameNew, Constants.NBT.TAG_STRING) == false ||
                         propsOld.getString(propNameNew).equals(propsNew.getString(propNameNew)) == false))
                {
                    return false;
                }
            }

            /*
            if (stateTagNew.getString("Name").equals("minecraft:structure_block"))
            {
                System.out.printf("old: '%s' - new: '%s'\n", stateTagOld.toString(), stateTagNew.toString());
            }
            */

            return true;
        }

        return false;
    }

    public void addStatesToMap()
    {
        File fileIn = new File(FileUtils.getMinecraftDirectory(), "block_state_map_in.json");
        JsonObject root = this.read_1_12_mappings(fileIn);

        if (root == null)
        {
            System.out.printf("Failed to read the input block state map from '%s'\n", fileIn.getAbsolutePath());
            return;
        }

        JsonArray arr = root.get("block_states").getAsJsonArray();
        this.newStatesArray = new JsonArray();

        this.resetCounters();
        this.buildNewNameToStatesMap(SchematicConversionMaps.CACHED_DATA);
        this.build_1_12_BlockStateTagToBlockStateEntryJsonObjectMap(arr);

        for (ResourceLocation id : IRegistry.BLOCK.keySet())
        {
            Block block = IRegistry.BLOCK.get(id);
            this.addBlockToMap(id, block);
        }

        File fileOut = new File(FileUtils.getMinecraftDirectory(), "block_state_map_out.json");
        //JsonUtils.writeJsonToFile(root, fileOut);
        dumpDataToFile(fileOut, getCustomFormattedJson(root));

        if (this.newStatesArray.size() > 0)
        {
            root = new JsonObject();
            root.add("block_states", this.newStatesArray);
            fileOut = new File(FileUtils.getMinecraftDirectory(), "block_state_map_out_new_states.json");
            //JsonUtils.writeJsonToFile(root, fileOut);
            dumpDataToFile(fileOut, getCustomFormattedJson(root));
        }

        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.13 Fully matched blocks:\n");
        System.out.printf("===============================\n");
        Collections.sort(this.fullyMatchedBlocks);
        this.fullyMatchedBlocks.forEach((str) -> System.out.printf("%s\n", str));

        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.13 Indirectly matched blocks:\n");
        System.out.printf("===============================\n");
        Collections.sort(this.indirectlyMatchedBlocks);
        this.indirectlyMatchedBlocks.forEach((str) -> System.out.printf("%s\n", str));

        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.13 Special matched blocks:\n");
        System.out.printf("===============================\n");
        Collections.sort(this.specialMatchedBlocks);
        this.specialMatchedBlocks.forEach((str) -> System.out.printf("%s\n", str));

        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.13 Partially matched blocks:\n");
        System.out.printf("===============================\n");
        Collections.sort(this.partiallyMatchedBlocks);
        this.partiallyMatchedBlocks.forEach((str) -> System.out.printf("%s\n", str));

        System.out.printf("\n");
        System.out.printf("===============================\n");
        System.out.printf("1.13 Totally unmatched blocks:\n");
        System.out.printf("===============================\n");
        Collections.sort(this.nonMatchedBlocks);
        this.nonMatchedBlocks.forEach((str) -> System.out.printf("%s\n", str));
        System.out.printf("\n");

        System.out.printf("1.12 Total States: %d\n", this.totalStates112);
        System.out.printf("1.13 Blocks:\n");
        System.out.printf(" > Total: %d\n", this.totalBlocks113);
        System.out.printf(" > Fully matched: %d\n", this.fullyMatched113Blocks);
        System.out.printf(" > Partially matched: %d\n", this.partiallyMatched113Blocks);
        System.out.printf(" > Indirectly matched: %d\n", this.indirectlyMatched113Blocks);
        System.out.printf(" > Special matched: %d\n", this.specialMatched113Blocks);
        System.out.printf(" > Fully unmatched: %d\n", this.nonMatched113Blocks);
        System.out.printf(" > Non-flattening map blocks: %d\n", this.nonFlatteningMap113Blocks);
        System.out.printf("1.13 States:\n");
        System.out.printf(" > Total: %d\n", this.totalStates113);
        System.out.printf("   > Found in flattening map: %d\n", this.flatteningMapMatched113States);
        System.out.printf(" > Fully matched: %d / %d\n", this.fullyMatched113States, this.totalStates113);
        System.out.printf(" > Indirectly matched: %d / %d\n", this.indirectlyMatched113States, this.totalStates113);
        System.out.printf(" > Special matched: %d / %d\n", this.specialMatched113States, this.totalStates113);
        System.out.printf(" > Unmatched: %d / %d\n", this.nonMatched113States, this.totalStates113);
        System.out.printf("   > Unmatched because 1.12 state tag not found: %d / %d\n", this.nonMatched113StatesBecauseNo112TagFound, this.nonMatched113States);
    }

    public static NBTTagCompound blockStateToTag(IBlockState state, String blockName)
    {
        NBTTagCompound stateTag = new NBTTagCompound();
        stateTag.putString("Name", blockName);

        if (state.getProperties().size() > 0)
        {
            NBTTagCompound propsTag = new NBTTagCompound();

            for (IProperty<?> prop : state.getProperties())
            {
                propsTag.putString(prop.getName(), state.get(prop).toString());
            }

            stateTag.put("Properties", propsTag);
        }

        return stateTag;
    }

    public static JsonObject blockStateTagToJsonObject(NBTTagCompound stateTag)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("block", stateTag.getString("Name"));

        if (stateTag.contains("Properties", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound propsTag = stateTag.getCompound("Properties");
            List<Pair<String, String>> propsAndValues = new ArrayList<>();

            for (String propName : propsTag.keySet())
            {
                propsAndValues.add(Pair.of(propName, propsTag.getString(propName)));
            }

            Collections.sort(propsAndValues, (v1, v2) -> { return v1.getLeft().compareTo(v2.getLeft()); } );
            JsonObject objProps = new JsonObject();

            for (Pair<String, String> pair : propsAndValues)
            {
                objProps.addProperty(pair.getLeft(), pair.getRight());
            }

            obj.add("properties", objProps);
        }

        return obj;
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

    public static List<String> getCustomFormattedJson(JsonObject obj)
    {
        /*
        StringBuilder sb = new StringBuilder(indentationLevel);
        for (int i = 0; i < indentationLevel; ++i) { sb.append("\t"); }
        String indentation1 = sb.toString();
        String indentation2 = sb.append("\t").toString();
        */

        List<String> lines = new ArrayList<>();

        lines.add("{");

        try
        {
            if (JsonUtils.hasArray(obj, "block_states"))
            {
                lines.add("\t\"block_states\": [");

                JsonArray arr = obj.get("block_states").getAsJsonArray();
                final int size = arr.size();

                for (int i = 0; i < size; ++i)
                {
                    lines.add("\t\t{");

                    JsonObject entry = arr.get(i).getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entrySet = entry.entrySet();
                    final int count = entrySet.size();
                    int entryIndex = 0;

                    for (Map.Entry<String, JsonElement> versionEntry : entrySet)
                    {
                        String name = versionEntry.getKey();
                        String str = String.format("\t\t\t\"%s\": ", name) + jsonObjectToString(name, versionEntry.getValue().getAsJsonObject());

                        if (++entryIndex < count)
                        {
                            str += ",";
                        }

                        lines.add(str);
                    }

                    if (i < (size - 1))
                    {
                        lines.add("\t\t},");
                    }
                    else
                    {
                        lines.add("\t\t}");
                    }
                }

                lines.add("\t]");
            }
        }
        catch (Exception e)
        {
            System.out.printf("Exception while outputting custom JSON\n");
        }

        lines.add("}");
        //lines.add(String.format("%s}", indentation1));

        return lines;
    }

    private static String jsonObjectToString(String name, JsonObject obj)
    {
        Set<Map.Entry<String, JsonElement>> entrySet = obj.entrySet();
        final int valueCount = entrySet.size();

        StringBuilder sb = new StringBuilder();
        //sb.append("\"").append(name).append("\": { ");
        sb.append("{ ");
        int i = 0;

        for (Map.Entry<String, JsonElement> entry : entrySet)
        {
            String propName = entry.getKey();
            JsonElement el = entry.getValue();

            sb.append("\"").append(propName).append("\": ");

            if (el.isJsonPrimitive())
            {
                sb.append(el.toString());
            }
            else if (el.isJsonObject())
            {
                sb.append(jsonObjectToString(propName, el.getAsJsonObject()));
            }

            if (++i < valueCount)
            {
                sb.append(", ");
            }
        }

        sb.append(" }");

        return sb.toString();
    }

    @Nullable
    public static File dumpDataToFile(File outFile, List<String> lines)
    {
        try
        {
            outFile.createNewFile();
        }
        catch (IOException e)
        {
            Litematica.logger.error("dumpDataToFile(): Failed to create data dump file '{}'", outFile, e);
            return null;
        }

        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            final int size = lines.size();

            for (int i = 0; i < size; ++i)
            {
                writer.write(lines.get(i));
                writer.newLine();
            }

            writer.close();
        }
        catch (IOException e)
        {
            Litematica.logger.error("dumpDataToFile(): Exception while writing data dump to file '{}'", outFile, e);
        }

        return outFile;
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
