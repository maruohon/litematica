package fi.dy.masa.litematica.schematic.conversion;

import java.util.Arrays;
import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers.IStateFixer;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBannerWall;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChorusPlant;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockGlassPane;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockShearableDoublePlant;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSkullWall;
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

public class SchematicConverter
{
    private final IdentityHashMap<Class<? extends Block>, IStateFixer> fixersPerBlock = new IdentityHashMap<>();
    private IdentityHashMap<IBlockState, IStateFixer> postProcessingStateFixers = new IdentityHashMap<>();

    private SchematicConverter()
    {
        this.addPostUpdateBlocks();
    }

    public static SchematicConverter create()
    {
        return new SchematicConverter();
    }

    public boolean getConvertedStatesForBlock(int schematicBlockId, String blockName, IBlockState[] paletteOut)
    {
        int shiftedOldVanillaId = SchematicConversionMaps.getOldNameToShiftedBlockId(blockName);
        int successCount = 0;

        //System.out.printf("blockName: %s, shiftedOldVanillaId: %d\n", blockName, shiftedOldVanillaId);
        if (shiftedOldVanillaId >= 0)
        {
            for (int meta = 0; meta < 16; ++meta)
            {
                // Make sure to clear the meta bits, in case the entry in the map for the name wasn't for meta 0
                IBlockState state = SchematicConversionMaps.get_1_13_2_StateForIdMeta((shiftedOldVanillaId & 0xFFF0) | meta);

                if (state != null)
                {
                    paletteOut[(schematicBlockId << 4) | meta] = state;
                    ++successCount;
                    //System.out.printf("idMeta: %5d: state: %s\n", (shiftedOldVanillaId & 0xFFF0) | meta, state);
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

    public NBTTagCompound fixTileEntityNBT(NBTTagCompound tag, IBlockState state)
    {
        /*
        try
        {
            tag = (NBTTagCompound) this.mc.getDataFixer().update(TypeReferences.BLOCK_ENTITY, new Dynamic<>(NBTDynamicOps.INSTANCE, tag),
                    1139, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Throwable e)
        {
            Litematica.logger.warn("Failed to update BlockEntity data for block '{}'", state, e);
        }
        */

        return tag;
    }

    private void addPostUpdateBlocks()
    {
        // Fixers to fix the state according to the adjacent blocks
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
        this.fixersPerBlock.put(BlockRedstoneRepeater.class,        SchematicConversionFixers.FIXER_REDSTONE_REPEATER);
        this.fixersPerBlock.put(BlockRedstoneWire.class,            SchematicConversionFixers.FIXER_REDSTONE_WIRE);
        this.fixersPerBlock.put(BlockShearableDoublePlant.class,    SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(BlockStem.class,                    SchematicConversionFixers.FIXER_STEM);
        this.fixersPerBlock.put(BlockStainedGlassPane.class,        SchematicConversionFixers.FIXER_PANE);
        this.fixersPerBlock.put(BlockStairs.class,                  SchematicConversionFixers.FIXER_STAIRS);
        this.fixersPerBlock.put(BlockTallFlower.class,              SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(BlockTripWire.class,                SchematicConversionFixers.FIXER_TRIPWIRE);
        this.fixersPerBlock.put(BlockVine.class,                    SchematicConversionFixers.FIXER_VINE);
        this.fixersPerBlock.put(BlockWall.class,                    SchematicConversionFixers.FIXER_WALL);

        // Fixers to get values from old TileEntity data
        this.fixersPerBlock.put(BlockBanner.class,                  SchematicConversionFixers.FIXER_BANNER);
        this.fixersPerBlock.put(BlockBannerWall.class,              SchematicConversionFixers.FIXER_BANNER_WALL);
        this.fixersPerBlock.put(BlockBed.class,                     SchematicConversionFixers.FIXER_BED);
        this.fixersPerBlock.put(BlockFlowerPot.class,               SchematicConversionFixers.FIXER_FLOWER_POT);
        this.fixersPerBlock.put(BlockNote.class,                    SchematicConversionFixers.FIXER_NOTE_BLOCK);
        this.fixersPerBlock.put(BlockSkull.class,                   SchematicConversionFixers.FIXER_SKULL);
        this.fixersPerBlock.put(BlockSkullWall.class,               SchematicConversionFixers.FIXER_SKULL_WALL);
    }
}
