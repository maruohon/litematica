package litematica.schematic.container;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.block.state.IBlockState;

import malilib.util.position.Vec3i;

public class LitematicaBlockStateContainerSparse extends LitematicaBlockStateContainerBase
{
    private final Long2ObjectOpenHashMap<IBlockState> blocks = new Long2ObjectOpenHashMap<>();

    public LitematicaBlockStateContainerSparse(Vec3i size)
    {
        super(size);

        this.palette = new VanillaStructurePalette();
        this.blockCounts = new long[256];
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);
        IBlockState state = this.blocks.get(pos);
        return state != null ? state : AIR_BLOCK_STATE;
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);

        IBlockState oldState = this.blocks.put(pos, state);
        int id = this.palette.idFor(state);

        if (id >= this.blockCounts.length)
        {
            long[] oldArr = this.blockCounts;
            this.blockCounts = new long[oldArr.length * 2];
            System.arraycopy(oldArr, 0, this.blockCounts, 0, oldArr.length);
        }

        if (oldState != state)
        {
            if (oldState != null)
            {
                int oldId = this.palette.idFor(oldState);
                --this.blockCounts[oldId];
            }

            ++this.blockCounts[id];
        }
    }

    @Override
    public LitematicaBlockStateContainerSparse copy()
    {
        LitematicaBlockStateContainerSparse copy = new LitematicaBlockStateContainerSparse(this.size);
        copy.blocks.putAll(this.blocks);
        copy.blockCounts = this.blockCounts.clone();
        copy.palette = this.palette.copy(null);
        return copy;
    }

    @Override
    protected void calculateBlockCountsIfNeeded()
    {
    }

    public Long2ObjectOpenHashMap<IBlockState> getBlockMap()
    {
        return this.blocks;
    }
}
