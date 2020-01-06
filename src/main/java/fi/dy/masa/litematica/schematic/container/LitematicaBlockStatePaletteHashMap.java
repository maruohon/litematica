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
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
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

    @Override
    public boolean setIdFor(int id, IBlockState state)
    {
        if (id >= 0)
        {
            this.statePaletteMap.put(state, id);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public NBTTagList writeToLitematicaTag()
    {
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            IBlockState state = this.statePaletteMap.get(id);

            if (state != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                NBTUtil.writeBlockState(tag, state);
                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    @Override
    public NBTTagCompound writeToSpongeTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            IBlockState state = this.statePaletteMap.get(id);

            if (state != null)
            {
                tag.setInteger(state.toString(), id);
            }
        }

        return tag;
    }

    @Override
    public LitematicaBlockStatePaletteHashMap copy()
    {
        LitematicaBlockStatePaletteHashMap copy = new LitematicaBlockStatePaletteHashMap(this.bits, this.paletteResizer);

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            copy.statePaletteMap.add(this.statePaletteMap.get(id));
        }

        return copy;
    }
}
