package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    private final IBlockState[] states;
    private final ILitematicaBlockStatePaletteResizer resizeHandler;
    private final int bits;
    private int currentSize;

    public LitematicaBlockStatePaletteLinear(int bitsIn, ILitematicaBlockStatePaletteResizer resizeHandler)
    {
        this.states = new IBlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.resizeHandler = resizeHandler;
    }

    @Override
    public int idFor(IBlockState state)
    {
        for (int i = 0; i < this.currentSize; ++i)
        {
            if (this.states[i] == state)
            {
                return i;
            }
        }

        final int size = this.currentSize;

        if (size < this.states.length)
        {
            this.states[size] = state;
            ++this.currentSize;
            return size;
        }
        else
        {
            return this.resizeHandler.onResize(this.bits + 1, state);
        }
    }

    @Override
    @Nullable
    public IBlockState getBlockState(int indexKey)
    {
        return indexKey >= 0 && indexKey < this.currentSize ? this.states[indexKey] : null;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
    }

    private void requestNewId(IBlockState state)
    {
        final int size = this.currentSize;

        if (size < this.states.length)
        {
            this.states[size] = state;
            ++this.currentSize;
        }
        else
        {
            int newId = this.resizeHandler.onResize(this.bits + 1, LitematicaBlockStateContainer.AIR_BLOCK_STATE);

            if (newId <= size)
            {
                this.states[size] = state;
                ++this.currentSize;
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

        for (int id = 0; id < this.currentSize; ++id)
        {
            NBTTagCompound tag = NBTUtil.writeBlockState(this.states[id]);
            tagList.add(tag);
        }

        return tagList;
    }
}
