package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public interface IBlockReaderWithData extends BlockView
{
    @Nullable
    CompoundTag getBlockEntityData(BlockPos pos);
}
