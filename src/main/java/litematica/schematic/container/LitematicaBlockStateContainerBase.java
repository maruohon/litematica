package litematica.schematic.container;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import malilib.util.position.Vec3i;

public abstract class LitematicaBlockStateContainerBase implements ILitematicaBlockStateContainer
{
    public static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected static final int MAX_BITS_LINEAR = 4;

    protected ILitematicaBlockStatePalette palette;
    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final long sizeLayer;
    protected final long totalVolume;
    protected long[] blockCounts = new long[0];
    protected int bits;
    protected boolean hasSetBlockCounts;

    public LitematicaBlockStateContainerBase(Vec3i size)
    {
        this(size, 2);
    }

    protected LitematicaBlockStateContainerBase(Vec3i size, int bits)
    {
        this.size = size;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.totalVolume = (long) this.sizeX * (long) this.sizeY * (long) this.sizeZ;
        this.sizeLayer = (long) this.sizeX * (long) this.sizeZ;

        this.setBits(bits);
    }

    @Override
    public Vec3i getSize()
    {
        return this.size;
    }

    @Override
    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
    }

    @Override
    public long getTotalBlockCount()
    {
        this.calculateBlockCountsIfNeeded();

        ILitematicaBlockStatePalette palette = this.getPalette();
        IBlockState air = Blocks.AIR.getDefaultState();
        final int length = this.blockCounts.length;
        long count = 0;

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getBlockState(id);

            if (state != null && state != air)
            {
                count += this.blockCounts[id];
            }
        }

        return count;
    }

    @Override
    public Map<IBlockState, Long> getBlockCountsMap()
    {
        this.calculateBlockCountsIfNeeded();

        Object2LongOpenHashMap<IBlockState> map = new Object2LongOpenHashMap<>(this.blockCounts.length);
        ILitematicaBlockStatePalette palette = this.getPalette();
        final int length = Math.min(palette.getPaletteSize(), this.blockCounts.length);

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getBlockState(id);

            if (state != null)
            {
                map.put(state, this.blockCounts[id]);
            }
        }

        return map;
    }

    protected void setBlockCounts(long[] blockCounts)
    {
        final int length = blockCounts.length;

        if (this.blockCounts == null || this.blockCounts.length < length)
        {
            this.blockCounts = new long[length];
        }

        System.arraycopy(blockCounts, 0, this.blockCounts, 0, length);
        this.hasSetBlockCounts = true;
    }

    protected void setBits(int bitsIn)
    {
        this.bits = bitsIn;
    }

    protected abstract void calculateBlockCountsIfNeeded();

    public static ILitematicaBlockStatePalette createPalette(int bits, IPaletteResizeHandler resizeHandler)
    {
        if (bits <= MAX_BITS_LINEAR)
        {
            return new LitematicaBlockStatePaletteLinear(bits, resizeHandler);
        }
        else
        {
            return new LitematicaBlockStatePaletteHashMap(bits, resizeHandler);
        }
    }
}
