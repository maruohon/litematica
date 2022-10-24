package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IntIdentityHashBiMap;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    protected final IntIdentityHashBiMap<IBlockState> statePaletteMap;
    protected final IPaletteResizeHandler paletteResizer;
    protected final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, IPaletteResizeHandler paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
    }

    @Override
    public int idFor(IBlockState state)
    {
        int id = this.statePaletteMap.getId(state);

        if (id == -1)
        {
            id = this.statePaletteMap.add(state);

            if (id >= (1 << this.bits))
            {
                id = this.paletteResizer.onResize(this.bits + 1, state, this);
            }
        }

        return id;
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
    public List<IBlockState> getMapping()
    {
        final int size = this.statePaletteMap.size();
        List<IBlockState> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            list.add(this.statePaletteMap.get(id));
        }

        return list;
    }

    @Override
    public boolean setMapping(List<IBlockState> list)
    {
        this.statePaletteMap.clear();
        final int size = list.size();

        for (int id = 0; id < size; ++id)
        {
            this.statePaletteMap.add(list.get(id));
        }

        return true;
    }

    @Override
    public boolean overrideMapping(int id, IBlockState state)
    {
        List<IBlockState> mapping = this.getMapping();

        if (id >= 0 && id < mapping.size())
        {
            // The put method of the map doesn't work for this, it increases the size etc. :/
            mapping.set(id, state);
            this.setMapping(mapping);
            return true;
        }

        return false;
    }

    @Override
    public LitematicaBlockStatePaletteHashMap copy(IPaletteResizeHandler resizeHandler)
    {
        LitematicaBlockStatePaletteHashMap copy = new LitematicaBlockStatePaletteHashMap(this.bits, resizeHandler);

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            copy.statePaletteMap.add(this.statePaletteMap.get(id));
        }

        return copy;
    }
}
