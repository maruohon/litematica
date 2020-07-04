package de.meinbuild.liteschem.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtHelper;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    private final BlockState[] states;
    private final ILitematicaBlockStatePaletteResizer resizeHandler;
    private final int bits;
    private int currentSize;

    public LitematicaBlockStatePaletteLinear(int bitsIn, ILitematicaBlockStatePaletteResizer resizeHandler)
    {
        this.states = new BlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.resizeHandler = resizeHandler;
    }

    @Override
    public int idFor(BlockState state)
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
    public BlockState getBlockState(int indexKey)
    {
        return indexKey >= 0 && indexKey < this.currentSize ? this.states[indexKey] : null;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
    }

    private void requestNewId(BlockState state)
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
    public void readFromNBT(ListTag tagList)
    {
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompound(i);
            BlockState state = NbtHelper.toBlockState(tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public ListTag writeToNBT()
    {
        ListTag tagList = new ListTag();

        for (int id = 0; id < this.currentSize; ++id)
        {
            CompoundTag tag = NbtHelper.fromBlockState(this.states[id]);
            tagList.add(tag);
        }

        return tagList;
    }
}
