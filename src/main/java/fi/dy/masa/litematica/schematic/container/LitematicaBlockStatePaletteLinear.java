package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    private final IBlockState[] states;
    private final ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;
    private int currentSize;

    public LitematicaBlockStatePaletteLinear(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.states = new IBlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
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
            return this.paletteResizer.onResize(this.bits + 1, state);
        }
    }

    @Override
    @Nullable
    public IBlockState getBlockState(int id)
    {
        return id >= 0 && id < this.currentSize ? this.states[id] : null;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
    }

    @Override
    public boolean setIdFor(int id, IBlockState state)
    {
        if (id >= 0 && id < this.states.length)
        {
            this.states[id] = state;
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean readFromLitematicaTag(NBTTagList tagList)
    {
        this.currentSize = tagList.tagCount();
        return ILitematicaBlockStatePalette.super.readFromLitematicaTag(tagList);
    }

    @Override
    public boolean readFromSpongeTag(NBTTagCompound tag)
    {
        this.currentSize = tag.getKeySet().size();
        return ILitematicaBlockStatePalette.super.readFromSpongeTag(tag);
    }

    @Override
    public NBTTagList writeToLitematicaTag()
    {
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < this.currentSize; ++id)
        {
            NBTTagCompound tag = new NBTTagCompound();
            NBTUtil.writeBlockState(tag, this.states[id]);
            tagList.appendTag(tag);
        }

        return tagList;
    }

    @Override
    public NBTTagCompound writeToSpongeTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < this.currentSize; ++id)
        {
            IBlockState state = this.states[id];
            tag.setInteger(state.toString(), id);
        }

        return tag;
    }

    @Override
    public LitematicaBlockStatePaletteLinear copy()
    {
        LitematicaBlockStatePaletteLinear copy = new LitematicaBlockStatePaletteLinear(this.bits, this.paletteResizer);

        System.arraycopy(this.states, 0, copy.states, 0, this.states.length);
        copy.currentSize  = this.currentSize;

        return copy;
    }
}
