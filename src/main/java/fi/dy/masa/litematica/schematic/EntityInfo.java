package fi.dy.masa.litematica.schematic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public class EntityInfo
{
    public final Vec3d pos;
    public final NBTTagCompound nbt;

    public EntityInfo(Vec3d posVec, NBTTagCompound nbt)
    {
        this.pos = posVec;
        this.nbt = nbt;
    }

    public EntityInfo copy()
    {
        return new EntityInfo(this.pos, this.nbt.copy());
    }
}
