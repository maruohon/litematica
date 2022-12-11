package fi.dy.masa.litematica.schematic.container;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.collection.Int2ObjectBiMap;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    private final Int2ObjectBiMap<BlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = Int2ObjectBiMap.create(1 << bitsIn);
    }

    @Override
    public int idFor(BlockState state)
    {
        int i = this.statePaletteMap.getRawId(state);

        if (i == -1)
        {
            i = this.statePaletteMap.add(state);

            if (i >= (1 << this.bits))
            {
                i = this.paletteResizer.onResize(this.bits + 1, state);
            }
        }

        return i;
    }

    @Override
    @Nullable
    public BlockState getBlockState(int indexKey)
    {
        return this.statePaletteMap.get(indexKey);
    }

    @Override
    public int getPaletteSize()
    {
        return this.statePaletteMap.size();
    }

    private void requestNewId(BlockState state)
    {
        final int origId = this.statePaletteMap.add(state);

        if (origId >= (1 << this.bits))
        {
            int newId = this.paletteResizer.onResize(this.bits + 1, LitematicaBlockStateContainer.AIR_BLOCK_STATE);

            if (newId <= origId)
            {
                this.statePaletteMap.add(state);
            }
        }
    }

    @Override
    public void readFromNBT(NbtList tagList)
    {
        RegistryEntryLookup<Block> lookup = Registries.BLOCK.getReadOnlyWrapper();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            NbtCompound tag = tagList.getCompound(i);
            BlockState state = NbtHelper.toBlockState(lookup, tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public NbtList writeToNBT()
    {
        NbtList tagList = new NbtList();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            BlockState state = this.statePaletteMap.get(id);

            if (state == null)
            {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            NbtCompound tag = NbtHelper.fromBlockState(state);
            tagList.add(tag);
        }

        return tagList;
    }

    @Override
    public boolean setMapping(List<BlockState> list)
    {
        this.statePaletteMap.clear();

        for (BlockState blockState : list)
        {
            this.statePaletteMap.add(blockState);
        }

        return true;
    }
}
