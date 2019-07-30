package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public interface IBlockReaderWithData extends IBlockReader
{
    @Nullable
    NBTTagCompound getBlockEntityData(BlockPos pos);
}
