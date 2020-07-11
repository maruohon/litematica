package fi.dy.masa.litematica.schematic.conversion;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.MyceliumBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers.IStateFixer;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

public class SchematicConverter
{
    private final IdentityHashMap<Class<? extends Block>, IStateFixer> fixersPerBlock = new IdentityHashMap<>();
    private IdentityHashMap<BlockState, IStateFixer> postProcessingStateFixers = new IdentityHashMap<>();

    private SchematicConverter()
    {
    }

    public static SchematicConverter createForSchematica()
    {
        SchematicConverter converter = new SchematicConverter();
        converter.addPostUpdateBlocksSchematica();
        return converter;
    }

    public static SchematicConverter createForLitematica()
    {
        SchematicConverter converter = new SchematicConverter();
        converter.addPostUpdateBlocksLitematica();
        return converter;
    }

    public boolean getConvertedStatesForBlock(int schematicBlockId, String blockName, BlockState[] paletteOut)
    {
        int shiftedOldVanillaId = SchematicConversionMaps.getOldNameToShiftedBlockId(blockName);
        int successCount = 0;

        //System.out.printf("blockName: %s, shiftedOldVanillaId: %d\n", blockName, shiftedOldVanillaId);
        if (shiftedOldVanillaId >= 0)
        {
            for (int meta = 0; meta < 16; ++meta)
            {
                // Make sure to clear the meta bits, in case the entry in the map for the name wasn't for meta 0
                BlockState state = SchematicConversionMaps.get_1_13_2_StateForIdMeta((shiftedOldVanillaId & 0xFFF0) | meta);

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

    public boolean getVanillaBlockPalette(BlockState[] paletteOut)
    {
        for (int idMeta = 0; idMeta < paletteOut.length; ++idMeta)
        {
            BlockState state = SchematicConversionMaps.get_1_13_2_StateForIdMeta(idMeta);

            if (state != null)
            {
                paletteOut[idMeta] = state;
            }
        }

        return true;
    }

    public BlockState[] getBlockStatePaletteForBlockPalette(String[] blockPalette)
    {
        BlockState[] palette = new BlockState[blockPalette.length * 16];
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
    public boolean createPostProcessStateFilter(BlockState[] palette)
    {
        return this.createPostProcessStateFilter(Arrays.asList(palette));
    }

    /**
     * Creates the post process state filter array.
     * @param palette
     * @return true if there are at least some states that need post processing
     */
    public boolean createPostProcessStateFilter(Collection<BlockState> palette)
    {
        boolean needsPostProcess = false;
        this.postProcessingStateFixers.clear();

        // The main reason to lazy-construct the state-to-fixer map is to check if a given
        // schematic even has any states that need fixing.

        for (BlockState state : palette)
        {
            if (this.needsPostProcess(state))
            {
                this.postProcessingStateFixers.put(state, this.getFixerFor(state));
                needsPostProcess = true;
            }
        }

        return needsPostProcess;
    }

    public IdentityHashMap<BlockState, IStateFixer> getPostProcessStateFilter()
    {
        return this.postProcessingStateFixers;
    }

    private boolean needsPostProcess(BlockState state)
    {
        return state.isAir() == false && this.fixersPerBlock.containsKey(state.getBlock().getClass());
    }

    @Nullable
    private IStateFixer getFixerFor(BlockState state)
    {
        return this.fixersPerBlock.get(state.getBlock().getClass());
    }

    public CompoundTag fixTileEntityNBT(CompoundTag tag, BlockState state)
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

    public static void postProcessBlocks(LitematicaBlockStateContainer container, @Nullable Map<BlockPos, CompoundTag> tiles,
                                         IdentityHashMap<BlockState, IStateFixer> postProcessingFilter)
    {
        final int sizeX = container.getSize().getX();
        final int sizeY = container.getSize().getY();
        final int sizeZ = container.getSize().getZ();
        BlockReaderLitematicaContainer reader = new BlockReaderLitematicaContainer(container, tiles);
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        for (int y = 0; y < sizeY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    BlockState state = container.get(x, y, z);
                    IStateFixer fixer = postProcessingFilter.get(state);

                    if (fixer != null)
                    {
                        posMutable.set(x, y, z);
                        BlockState stateFixed = fixer.fixState(reader, state, posMutable);

                        if (stateFixed != state)
                        {
                            container.set(x, y, z, stateFixed);
                        }
                    }
                }
            }
        }
    }

    private void addPostUpdateBlocksLitematica()
    {
        // Fixers to fix the state according to the adjacent blocks
        this.fixersPerBlock.put(RedstoneWireBlock.class,            SchematicConversionFixers.FIXER_REDSTONE_WIRE);
        this.fixersPerBlock.put(WallBlock.class,                    WallStateFixer.INSTANCE);

        // Fixers to get values from old TileEntity data
        this.fixersPerBlock.put(BannerBlock.class,                  SchematicConversionFixers.FIXER_BANNER);
        this.fixersPerBlock.put(WallBannerBlock.class,              SchematicConversionFixers.FIXER_BANNER_WALL);
        this.fixersPerBlock.put(BedBlock.class,                     SchematicConversionFixers.FIXER_BED);
        this.fixersPerBlock.put(FlowerPotBlock.class,               SchematicConversionFixers.FIXER_FLOWER_POT);
        this.fixersPerBlock.put(NoteBlock.class,                    SchematicConversionFixers.FIXER_NOTE_BLOCK);
        this.fixersPerBlock.put(SkullBlock.class,                   SchematicConversionFixers.FIXER_SKULL);
        this.fixersPerBlock.put(WallSkullBlock.class,               SchematicConversionFixers.FIXER_SKULL_WALL);
    }

    private void addPostUpdateBlocksSchematica()
    {
        // Fixers to fix the state according to the adjacent blocks
        this.fixersPerBlock.put(ChorusPlantBlock.class,             SchematicConversionFixers.FIXER_CHRORUS_PLANT);
        this.fixersPerBlock.put(DoorBlock.class,                    SchematicConversionFixers.FIXER_DOOR);
        this.fixersPerBlock.put(FenceBlock.class,                   SchematicConversionFixers.FIXER_FENCE);
        this.fixersPerBlock.put(FenceGateBlock.class,               SchematicConversionFixers.FIXER_FENCE_GATE);
        this.fixersPerBlock.put(FireBlock.class,                    SchematicConversionFixers.FIXER_FIRE);
        this.fixersPerBlock.put(GrassBlock.class,                   SchematicConversionFixers.FIXER_DIRT_SNOWY);
        this.fixersPerBlock.put(MyceliumBlock.class,                SchematicConversionFixers.FIXER_DIRT_SNOWY);
        this.fixersPerBlock.put(PaneBlock.class,                    SchematicConversionFixers.FIXER_PANE); // Iron Bars & Glass Pane
        this.fixersPerBlock.put(RepeaterBlock.class,                SchematicConversionFixers.FIXER_REDSTONE_REPEATER);
        this.fixersPerBlock.put(RedstoneWireBlock.class,            SchematicConversionFixers.FIXER_REDSTONE_WIRE);
        this.fixersPerBlock.put(SnowyBlock.class,                   SchematicConversionFixers.FIXER_DIRT_SNOWY); // Podzol
        this.fixersPerBlock.put(StemBlock.class,                    SchematicConversionFixers.FIXER_STEM);
        this.fixersPerBlock.put(StainedGlassPaneBlock.class,        SchematicConversionFixers.FIXER_PANE);
        this.fixersPerBlock.put(StairsBlock.class,                  SchematicConversionFixers.FIXER_STAIRS);
        this.fixersPerBlock.put(TallFlowerBlock.class,              SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(TallPlantBlock.class,               SchematicConversionFixers.FIXER_DOUBLE_PLANT);
        this.fixersPerBlock.put(TripwireBlock.class,                SchematicConversionFixers.FIXER_TRIPWIRE);
        this.fixersPerBlock.put(VineBlock.class,                    SchematicConversionFixers.FIXER_VINE);
        this.fixersPerBlock.put(WallBlock.class,                    WallStateFixer.INSTANCE);

        // Fixers to get values from old TileEntity data
        this.fixersPerBlock.put(BannerBlock.class,                  SchematicConversionFixers.FIXER_BANNER);
        this.fixersPerBlock.put(WallBannerBlock.class,              SchematicConversionFixers.FIXER_BANNER_WALL);
        this.fixersPerBlock.put(BedBlock.class,                     SchematicConversionFixers.FIXER_BED);
        this.fixersPerBlock.put(FlowerPotBlock.class,               SchematicConversionFixers.FIXER_FLOWER_POT);
        this.fixersPerBlock.put(NoteBlock.class,                    SchematicConversionFixers.FIXER_NOTE_BLOCK);
        this.fixersPerBlock.put(SkullBlock.class,                   SchematicConversionFixers.FIXER_SKULL);
        this.fixersPerBlock.put(WallSkullBlock.class,               SchematicConversionFixers.FIXER_SKULL_WALL);
    }

    public static class BlockReaderLitematicaContainer implements IBlockReaderWithData
    {
        private final LitematicaBlockStateContainer container;
        private final Map<BlockPos, CompoundTag> blockEntityData;
        private final Vec3i size;
        private final BlockState air;

        public BlockReaderLitematicaContainer(LitematicaBlockStateContainer container, @Nullable Map<BlockPos, CompoundTag> blockEntityData)
        {
            this.container = container;
            this.blockEntityData = blockEntityData != null ? blockEntityData : new HashMap<>();
            this.size = container.getSize();
            this.air = Blocks.AIR.getDefaultState();
        }

        @Override
        public BlockState getBlockState(BlockPos pos)
        {
            if (pos.getX() >= 0 && pos.getX() < this.size.getX() &&
                        pos.getY() >= 0 && pos.getY() < this.size.getY() &&
                        pos.getZ() >= 0 && pos.getZ() < this.size.getZ())
            {
                return this.container.get(pos.getX(), pos.getY(), pos.getZ());
            }

            return this.air;
        }

        @Override
        public FluidState getFluidState(BlockPos pos)
        {
            // FIXME change when fluids become completely separate
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos)
        {
            return null;
        }

        @Override
        @Nullable
        public CompoundTag getBlockEntityData(BlockPos pos)
        {
            return this.blockEntityData.get(pos);
        }
    }
}
