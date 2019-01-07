package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.IntIdentityHashBiMap;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    private final IntIdentityHashBiMap<IBlockState> statePaletteMap;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = new IntIdentityHashBiMap<IBlockState>(1 << bitsIn);
    }

    @Override
    public int idFor(IBlockState state)
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
    public IBlockState getBlockState(int indexKey)
    {
        return this.statePaletteMap.get(indexKey);
    }

    @Override
    public int getPaletteSize()
    {
        return this.statePaletteMap.size();
    }

    private void requestNewId(IBlockState state)
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
    public void readFromNBT(NBTTagList tagList)
    {
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompound(i);
            IBlockState state = NBTUtil.readBlockState(tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public NBTTagList writeToNBT()
    {
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            NBTTagCompound tag = NBTUtil.writeBlockState(this.statePaletteMap.get(id));
            tagList.add(tag);
        }

        return tagList;
    }
}
