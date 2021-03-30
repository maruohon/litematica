package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.IntIdentityHashBiMap;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    private final IntIdentityHashBiMap<BlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
    }

    @Override
    public int idFor(BlockState state)
    {
        int i = this.statePaletteMap.getId(state);

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
    public void readFromNBT(ListNBT tagList)
    {
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundNBT tag = tagList.getCompound(i);
            BlockState state = NBTUtil.toBlockState(tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public ListNBT writeToNBT()
    {
        ListNBT tagList = new ListNBT();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            CompoundNBT tag = NBTUtil.fromBlockState(this.statePaletteMap.get(id));
            tagList.add(tag);
        }

        return tagList;
    }
}
