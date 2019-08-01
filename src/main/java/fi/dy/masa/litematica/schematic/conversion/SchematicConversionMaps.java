package fi.dy.masa.litematica.schematic.conversion;

import java.util.ArrayList;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Dynamic;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.datafix.TypeReferences;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;

public class SchematicConversionMaps
{
    private static final Object2IntOpenHashMap<String> OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID = DataFixUtils.make(new Object2IntOpenHashMap<>(), (map) -> { map.defaultReturnValue(-1); });
    private static final Object2IntOpenHashMap<IBlockState> BLOCKSTATE_TO_ID_META = DataFixUtils.make(new Object2IntOpenHashMap<>(), (map) -> { map.defaultReturnValue(-1); });
    private static final Int2ObjectOpenHashMap<IBlockState> ID_META_TO_BLOCKSTATE = new Int2ObjectOpenHashMap<>();
    private static final HashBiMap<String, String> OLD_NAME_TO_NEW_NAME = HashBiMap.create();
    private static final HashBiMap<NBTTagCompound, NBTTagCompound> OLD_STATE_TO_NEW_STATE = HashBiMap.create();
    private static final ArrayList<ConversionData> CACHED_DATA = new ArrayList<>();

    public static void addEntry(int idMeta, String newStateString, String... oldStateStrings)
    {
        CACHED_DATA.add(new ConversionData(idMeta, newStateString, oldStateStrings));
    }

    public static void computeMaps()
    {
        clearMaps();

        for (ConversionData data : CACHED_DATA)
        {
            try
            {
                if (data.oldStateStrings.length > 0)
                {
                    Dynamic<?> dynamic = BlockStateFlatteningMap.makeDynamic(data.oldStateStrings[0]);
                    String name = dynamic.getString("Name");
                    OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.putIfAbsent(name, data.idMeta & 0xFFFFFFF0);
                }

                addIdMetaToBlockState(data.idMeta, BlockStateFlatteningMap.makeDynamic(data.newStateString), data.oldStateStrings);
            }
            catch (Exception e)
            {
                Litematica.logger.warn("addEntry(): Exception while adding blockstate conversion map entry for ID '{}' (fixed state: '{}')", data.idMeta, data.newStateString, e);
            }
        }
    }

    public static NBTTagCompound get_1_13_2_StateTagFor_1_12_tag(NBTTagCompound oldStateTag)
    {
        NBTTagCompound tag = OLD_STATE_TO_NEW_STATE.get(oldStateTag);
        return tag != null ? tag : NBTUtil.writeBlockState(Blocks.AIR.getDefaultState());
    }

    public static NBTTagCompound get_1_12_StateTagFor_1_13_2_tag(NBTTagCompound newStateTag)
    {
        NBTTagCompound tag = OLD_STATE_TO_NEW_STATE.inverse().get(newStateTag);
        return tag != null ? tag : NBTUtil.writeBlockState(Blocks.AIR.getDefaultState());
    }

    public static int getOldNameToShiftedBlockId(String oldBlockname)
    {
        return OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.getInt(oldBlockname);
    }

    private static void addOverrides()
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        BLOCKSTATE_TO_ID_META.put(air, 0);
        ID_META_TO_BLOCKSTATE.put(0, air);
    }

    private static void clearMaps()
    {
        OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.clear();
        BLOCKSTATE_TO_ID_META.clear();
        ID_META_TO_BLOCKSTATE.clear();

        addOverrides();
    }

    private static void addIdMetaToBlockState(int idMeta, Dynamic<?> newStateString, String... oldStateStrings)
    {
        try
        {
            NBTTagCompound tag = (NBTTagCompound) newStateString.getValue();

            if (tag != null)
            {
                tag = tag.copy();

                // Run the DataFixer for the block name, for blocks that were renamed after the flattening.
                // ie. the flattening map actually has outdated names for some blocks... >_>
                String namePre = tag.getString("Name");

                if (namePre.equals("%%FILTER_ME%%"))
                {
                    // FIXME
                    tag.putString("Name", "minecraft:skull");
                }
                else
                {
                    tag.putString("Name", updateBlockName(namePre));
                }

                String newName = tag.getString("Name");

                if (OLD_NAME_TO_NEW_NAME.containsKey(namePre) == false &&
                    OLD_NAME_TO_NEW_NAME.inverse().containsKey(newName) == false)
                {
                    OLD_NAME_TO_NEW_NAME.put(namePre, newName);
                }

                IBlockState state = NBTUtil.readBlockState(tag);
                ID_META_TO_BLOCKSTATE.put(idMeta, state);

                // Don't override the id and meta for air, which is what unrecognized blocks will turn into
                BLOCKSTATE_TO_ID_META.putIfAbsent(state, idMeta);

                addOldStateToNewState(tag, oldStateStrings);
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("addIdMetaToBlockState(): Exception while adding blockstate conversion map entry for ID '{}'", idMeta, e);
        }
    }

    private static void addOldStateToNewState(NBTTagCompound newStateTagIn, String... oldStateStrings)
    {
        try
        {
            // A 1:1 mapping from the old state to the new state
            if (oldStateStrings.length == 1)
            {
                NBTTagCompound oldStateTag = JsonToNBT.getTagFromJson(oldStateStrings[0].replace('\'', '"'));

                if (OLD_STATE_TO_NEW_STATE.containsKey(oldStateTag) == false &&
                    OLD_STATE_TO_NEW_STATE.inverse().containsKey(newStateTagIn) == false)
                {
                    OLD_STATE_TO_NEW_STATE.put(oldStateTag, newStateTagIn);
                }
            }
            // Multiple old states collapsed into one new state.
            // These are basically states where all the properties were not stored in metadata, but
            // some of the property values were calculated in the getActualState() method.
            else if (oldStateStrings.length > 1)
            {
                NBTTagCompound oldStateTag = JsonToNBT.getTagFromJson(oldStateStrings[0].replace('\'', '"'));

                // Same property names and same number of properties - just remap the block name.
                // FIXME Is this going to be correct for everything?
                if (newStateTagIn.keySet().equals(oldStateTag.keySet()))
                {
                    String oldBlockName = oldStateTag.getString("Name");
                    String newBlockName = OLD_NAME_TO_NEW_NAME.get(oldBlockName);

                    if (newBlockName != null && newBlockName.equals(oldBlockName) == false)
                    {
                        for (String oldStateString : oldStateStrings)
                        {
                            oldStateTag = JsonToNBT.getTagFromJson(oldStateString.replace('\'', '"'));
                            NBTTagCompound newTag = oldStateTag.copy();
                            newTag.putString("Name", newBlockName);

                            if (OLD_STATE_TO_NEW_STATE.containsKey(oldStateTag) == false &&
                                OLD_STATE_TO_NEW_STATE.inverse().containsKey(newTag) == false)
                            {
                                OLD_STATE_TO_NEW_STATE.put(oldStateTag, newTag);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("addOldStateToNewState(): Exception while adding new blockstate to old blockstate conversion map entry for '{}'", newStateTagIn, e);
        }
    }

    public static String updateBlockName(String oldName)
    {
        NBTTagString tagStr = new NBTTagString(oldName);

        return Minecraft.getInstance().getDataFixer().update(TypeReferences.BLOCK_NAME, new Dynamic<>(NBTDynamicOps.INSTANCE, tagStr),
                        1139, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue().getString();
    }

    private static class ConversionData
    {
        private final int idMeta;
        private final String newStateString;
        private final String[] oldStateStrings;

        private ConversionData(int idMeta, String newStateString, String[] oldStateStrings)
        {
            this.idMeta = idMeta;
            this.newStateString = newStateString;
            this.oldStateStrings = oldStateStrings;
        }
    }
}
