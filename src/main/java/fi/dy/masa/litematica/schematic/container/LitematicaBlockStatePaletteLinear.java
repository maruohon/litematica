package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    private final IBlockState[] states;
    private final IPaletteResizeHandler paletteResizer;
    private final int bits;
    private int currentSize;

    public LitematicaBlockStatePaletteLinear(int bitsIn, IPaletteResizeHandler paletteResizer)
    {
        this.states = new IBlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
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
            return this.paletteResizer.onResize(this.bits + 1, state, this);
        }
    }

    @Override
    @Nullable
    public IBlockState getBlockState(int id)
    {
        return id >= 0 && id < this.currentSize ? this.states[id] : null;
    }

    @Override
    public List<IBlockState> getMapping()
    {
        List<IBlockState> list = new ArrayList<>(this.currentSize);

        for (int id = 0; id < this.currentSize; ++id)
        {
            list.add(this.states[id]);
        }

        return list;
    }

    @Override
    public boolean setMapping(List<IBlockState> list)
    {
        final int size = list.size();

        if (size <= this.states.length)
        {
            for (int id = 0; id < size; ++id)
            {
                this.states[id] = list.get(id);
            }

            this.currentSize = size;

            return true;
        }

        return false;
    }

    @Override
    public boolean overrideMapping(int id, IBlockState state)
    {
        if (id >= 0 && id < this.states.length)
        {
            this.states[id] = state;
            return true;
        }

        return false;
    }

    @Override
    public LitematicaBlockStatePaletteLinear copy(IPaletteResizeHandler resizeHandler)
    {
        LitematicaBlockStatePaletteLinear copy = new LitematicaBlockStatePaletteLinear(this.bits, resizeHandler);

        System.arraycopy(this.states, 0, copy.states, 0, this.states.length);
        copy.currentSize  = this.currentSize;

        return copy;
    }
}
