package fi.dy.masa.litematica.schematic.container;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BitArray;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer
{
    protected static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected BitArray storage;
    protected ILitematicaBlockStatePalette palette;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int sizeLayer;
    protected int bits;

    public LitematicaBlockStateContainer(int sizeX, int sizeY, int sizeZ)
    {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeLayer = sizeX * sizeZ;

        this.setBits(4);
    }

    public IBlockState get(int x, int y, int z)
    {
        IBlockState state = this.palette.getBlockState(this.storage.getAt(this.getIndex(x, y, z)));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    public void set(int x, int y, int z, IBlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(this.getIndex(x, y, z), id);
    }

    protected void set(int index, IBlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(index, id);
    }

    protected int getIndex(int x, int y, int z)
    {
        return (y * this.sizeLayer) + z * this.sizeX + x;
    }

    protected void setBits(int bitsIn)
    {
        if (bitsIn != this.bits)
        {
            this.bits = bitsIn;

            if (this.bits <= 4)
            {
                this.bits = 4;
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this);
            }
            else
            {
                this.palette = new LitematicaBlockStatePaletteHashMap(this.bits, this);
            }

            this.palette.idFor(AIR_BLOCK_STATE);
            this.storage = new BitArray(this.bits, this.sizeX * this.sizeY * this.sizeZ);
        }
    }

    @Override
    public int onResize(int bits, IBlockState state)
    {
        BitArray bitArray = this.storage;
        ILitematicaBlockStatePalette statePaletteOld = this.palette;
        this.setBits(bits);

        for (int id = 0; id < bitArray.size(); ++id)
        {
            IBlockState stateTmp = statePaletteOld.getBlockState(bitArray.getAt(id));

            if (stateTmp != null)
            {
                this.set(id, stateTmp);
            }
        }

        return this.palette.idFor(state);
    }

    public long[] getBackingLongArray()
    {
        return this.storage.getBackingLongArray();
    }

    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
    }
}
