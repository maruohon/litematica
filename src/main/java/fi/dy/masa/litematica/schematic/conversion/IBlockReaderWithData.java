package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public interface IBlockReaderWithData extends IBlockReader
{
    @Nullable
    CompoundNBT getTileEntityData(BlockPos pos);
}
