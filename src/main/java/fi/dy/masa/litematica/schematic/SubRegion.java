package fi.dy.masa.litematica.schematic;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class SubRegion
{
    public final BlockPos pos;
    public final Vec3i size;

    public SubRegion(BlockPos pos, Vec3i size)
    {
        this.pos = pos;
        this.size = size;
    }
}
