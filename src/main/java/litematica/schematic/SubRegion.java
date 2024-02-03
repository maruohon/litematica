package litematica.schematic;

import malilib.util.position.BlockPos;
import malilib.util.position.Vec3i;

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
