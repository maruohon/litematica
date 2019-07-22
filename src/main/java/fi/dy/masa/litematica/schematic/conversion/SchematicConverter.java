package fi.dy.masa.litematica.schematic.conversion;

import java.util.Arrays;
import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import com.mojang.datafixers.Dynamic;
import fi.dy.masa.litematica.mixin.IMixinBlockStateFlatteningMap;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers.IStateFixer;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockGlassPane;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockShearableDoublePlant;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStem;
import net.minecraft.block.BlockTallFlower;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.datafix.fixes.BlockStateFlatteningMap;

public class SchematicConverter
{
    private final Object2IntMap<String> nameToShiftedBlockId;
    private final IdentityHashMap<Class<? extends Block>, IStateFixer> fixersPerBlock = new IdentityHashMap<>();
    private IdentityHashMap<IBlockState, IStateFixer> postProcessingStateFixers = new IdentityHashMap<>();

    private SchematicConverter()
    {
        this.nameToShiftedBlockId = IMixinBlockStateFlatteningMap.getOldNameToShiftedOldBlockIdMap();

        this.addPostUpdateBlocks();
    }

    public static SchematicConverter create()
    {
        return new SchematicConverter();
    }

    public boolean getConvertedStatesForBlock(int schematicBlockId, String blockName, IBlockState[] paletteOut)
    {
        int shiftedOldVanillaId = this.nameToShiftedBlockId.getInt(blockName);
        int successCount = 0;

        if (shiftedOldVanillaId >= 0)
        {
            //System.out.printf("name: %s, vanilla id: %d\n", blockName, shiftedOldVanillaId);
            for (int meta = 0; meta < 16; ++meta)
            {
                // Make sure to clear the meta bits, in case the entry in the map for the name wasn't for meta 0
                Dynamic<?> newStateString = BlockStateFlatteningMap.getFixedNBTForID((shiftedOldVanillaId & 0xFFF0) | meta);
                //System.out.printf("name: %s, newStateString: %s\n", blockName, newStateString);
                try
                {
                    NBTTagCompound tag = (NBTTagCompound) newStateString.getValue();//.JsonToNBT.getTagFromJson(optional.get().replace('\'', '"'));
                    IBlockState state = NBTUtil.readBlockState(tag);
                    //System.out.printf("name: %s, tag: %s, state: %s\n", blockName, tag, state);
                    paletteOut[(schematicBlockId << 4) | meta] = state;
                    ++successCount;
                }
                catch (Exception e)
                {
                }
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to convert block with old name '" + blockName + "'");
        }

        return successCount > 0;
    }

    public IBlockState[] getBlockStatePaletteForBlockPalette(String[] blockPalette)
    {
        IBlockState[] palette = new IBlockState[blockPalette.length * 16];
        Arrays.fill(palette, Blocks.AIR.getDefaultState());

        for (int schematicBlockId = 0; schematicBlockId < blockPalette.length; ++schematicBlockId)
        {
            String blockName = blockPalette[schematicBlockId];
            this.getConvertedStatesForBlock(schematicBlockId, blockName, palette);
        }

        return palette;
    }

    /**
     * Creates the post process state filter array.
     * @param palette
     * @return true if there are at least some states that need post processing
     */
    public boolean createPostProcessStateFilter(IBlockState[] palette)
    {
        boolean needsPostProcess = false;
        this.postProcessingStateFixers.clear();

        // The main reason to lazy-construct the state-to-fixer map is to check if a given
        // schematic even has any states that need fixing.

        for (int i = 0; i < palette.length; ++i)
        {
            IBlockState state = palette[i];

            if (this.needsPostProcess(state))
            {
                this.postProcessingStateFixers.put(state, this.getFixerFor(state));
                needsPostProcess = true;
            }
        }

        return needsPostProcess;
    }

    public IdentityHashMap<IBlockState, IStateFixer> getPostProcessStateFilter()
    {
        return this.postProcessingStateFixers;
    }

    private boolean needsPostProcess(IBlockState state)
    {
        return state.isAir() == false && this.fixersPerBlock.containsKey(state.getBlock().getClass());
    }

    @Nullable
    private IStateFixer getFixerFor(IBlockState state)
    {
        return this.fixersPerBlock.get(state.getBlock().getClass());
    }

    private void addPostUpdateBlocks()
    {
        // TODO: Note block
        // TODO: Flower pot
        // TODO: Skulls
        // TODO: Banner

        // TODO: Farmland moisture
        // TODO: Melons and pumpkins are broken?
        this.fixersPerBlock.put(BlockChorusPlant.class,             SchematicConversionFixers.FIXER_CHRORUS_PLANT);
        this.fixersPerBlock.put(BlockDirtSnowy.class,               SchematicConversionFixers.FIXER_DIRT_SNOWY); // Podzol
        this.fixersPerBlock.put(BlockDoor.class,                    SchematicConversionFixers.FIXER_DOOR);
        this.fixersPerBlock.put(BlockFence.class,                   SchematicConversionFixers.FIXER_FENCE);
        this.fixersPerBlock.put(BlockFenceGate.class,               SchematicConversionFixers.FIXER_FENCE_GATE);
        this.fixersPerBlock.put(BlockFire.class,                    SchematicConversionFixers.FIXER_FIRE);
        this.fixersPerBlock.put(BlockGlassPane.class,               SchematicConversionFixers.FIXER_PANE);
        this.fixersPerBlock.put(BlockGrass.class,                   SchematicConversionFixers.FIXER_DIRT_SNOWY);
        this.fixersPerBlock.put(BlockMycelium.class,                SchematicConversionFixers.FIXER_DIRT_SNOWY);
        this.fixersPerBlock.put(BlockPane.class,                    SchematicConversionFixers.FIXER_PANE); // Iron Bars
        //this.fixersPerBlock.put(BlockRedstoneRepeater.class,        SchematicConversionFixers.FIXER_REDSTONE_WIRE);
        this.fixersPerBlock.put(BlockRedstoneWire.class,            SchematicConversionFixers.FIXER_REDSTONE_WIRE);
        this.fixersPerBlock.put(BlockShearableDoublePlant.class,    SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(BlockStem.class,                    SchematicConversionFixers.FIXER_STEM);
        this.fixersPerBlock.put(BlockStainedGlassPane.class,        SchematicConversionFixers.FIXER_PANE);
        this.fixersPerBlock.put(BlockStairs.class,                  SchematicConversionFixers.FIXER_STAIRS);
        this.fixersPerBlock.put(BlockTallFlower.class,              SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(BlockTripWire.class,                SchematicConversionFixers.FIXER_TRIPWIRE);
        this.fixersPerBlock.put(BlockVine.class,                    SchematicConversionFixers.FIXER_VINE);
        this.fixersPerBlock.put(BlockWall.class,                    SchematicConversionFixers.FIXER_WALL);
    }
}
