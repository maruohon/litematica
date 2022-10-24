package fi.dy.masa.litematica.schematic.container;

import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3i;

public interface ILitematicaBlockStateContainer
{
    Vec3i getSize();

    long getTotalBlockCount();

    Map<IBlockState, Long> getBlockCountsMap();

    ILitematicaBlockStatePalette getPalette();

    IBlockState getBlockState(int x, int y, int z);

    void setBlockState(int x, int y, int z, IBlockState state);

    ILitematicaBlockStateContainer copy();
}
