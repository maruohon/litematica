package fi.dy.masa.litematica.schematic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

public class EntityInfo
{
    public final Vec3d posVec;
    public final NBTTagCompound nbt;

    public EntityInfo(Vec3d posVec, NBTTagCompound nbt)
    {
        this.posVec = posVec;
        this.nbt = nbt;
    }

    public EntityInfo copy()
    {
        return new EntityInfo(this.posVec, this.nbt.copy());
    }
}
