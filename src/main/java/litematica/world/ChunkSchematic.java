package litematica.world;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkSchematic extends Chunk
{
    public ChunkSchematic(World worldIn, int x, int z)
    {
        super(worldIn, x, z);

        this.setLightPopulated(true);
    }

    @Override
    public IBlockState setBlockState(BlockPos pos, IBlockState state)
    {
        int x = pos.getX() & 15;
        int y = pos.getY();
        int z = pos.getZ() & 15;

        IBlockState stateOld = this.getBlockState(pos);

        if (stateOld == state)
        {
            return null;
        }
        else
        {
            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            ExtendedBlockStorage storage = this.getBlockStorageArray()[y >> 4];

            if (storage == NULL_BLOCK_STORAGE)
            {
                if (blockNew == Blocks.AIR)
                {
                    return null;
                }

                storage = new ExtendedBlockStorage(y >> 4 << 4, false);
                this.getBlockStorageArray()[y >> 4] = storage;
            }

            storage.set(x, y & 15, z, state);

            if (blockOld != blockNew)
            {
                this.getWorld().removeTileEntity(pos);
            }

            if (storage.get(x, y & 0xF, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (blockOld instanceof ITileEntityProvider)
                {
                    TileEntity tileentity = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

                    if (tileentity != null)
                    {
                        tileentity.updateContainingBlockInfo();
                    }
                }

                /*
                if (this.getWorld().isRemote && blockOld != blockNew)
                {
                    blockNew.onBlockAdded(this.getWorld(), pos, state);
                }
                */

                if (blockNew instanceof ITileEntityProvider)
                {
                    TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

                    if (te == null)
                    {
                        te = ((ITileEntityProvider) blockNew).createNewTileEntity(this.getWorld(), blockNew.getMetaFromState(state));
                        this.getWorld().setTileEntity(pos, te);
                    }

                    if (te != null)
                    {
                        te.updateContainingBlockInfo();
                    }
                }

                this.markDirty();

                return stateOld;
            }
        }
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightSubtracted(BlockPos pos, int amount)
    {
        return 15;
    }

    @Override
    public void setLightFor(EnumSkyBlock type, BlockPos pos, int value)
    {
        // NO-OP
    }

    @Override
    public void onTick(boolean skipRecheckGaps)
    {
        super.onTick(true);
    }

    @Override
    public void checkLight()
    {
        // NO-OP
    }

    @Override
    public void generateSkylightMap()
    {
        // NO-OP
    }

    @Override
    public void enqueueRelightChecks()
    {
        // NO-OP
    }
}
