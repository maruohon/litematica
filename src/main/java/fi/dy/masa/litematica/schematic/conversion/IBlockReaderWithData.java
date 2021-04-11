package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public interface IBlockReaderWithData extends BlockView
{
    @Nullable
    NbtCompound getBlockEntityData(BlockPos pos);

    // TODO 1.17: These shouldn't matter here... right?
    @Override
    default int getHeight()
    {
        return 384;
    }

    @Override
    default int getBottomY()
    {
        return -64;
    }
}
