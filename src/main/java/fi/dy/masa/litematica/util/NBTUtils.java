package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class NBTUtils
{
    public static NBTTagCompound createBlockPosTag(BlockPos pos)
    {
        return writeBlockPosToTag(pos, new NBTTagCompound());
    }

    public static NBTTagCompound writeBlockPosToTag(BlockPos pos, NBTTagCompound tag)
    {
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
        return tag;
    }

    public static NBTTagCompound writeVec3dToTag(Vec3d vec, NBTTagCompound tag)
    {
        tag.setDouble("dx", vec.x);
        tag.setDouble("dy", vec.y);
        tag.setDouble("dz", vec.z);
        return tag;
    }

    @Nullable
    public static BlockPos readBlockPos(@Nullable NBTTagCompound tag)
    {
        if (tag != null &&
            tag.hasKey("x", Constants.NBT.TAG_INT) &&
            tag.hasKey("y", Constants.NBT.TAG_INT) &&
            tag.hasKey("z", Constants.NBT.TAG_INT))
        {
            return new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        }

        return null;
    }

    @Nullable
    public static Vec3d readVec3d(@Nullable NBTTagCompound tag)
    {
        if (tag != null &&
            tag.hasKey("dx", Constants.NBT.TAG_DOUBLE) &&
            tag.hasKey("dy", Constants.NBT.TAG_DOUBLE) &&
            tag.hasKey("dz", Constants.NBT.TAG_DOUBLE))
        {
            return new Vec3d(tag.getDouble("dx"), tag.getDouble("dy"), tag.getDouble("dz"));
        }

        return null;
    }
}
