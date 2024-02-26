package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileInputStream;
import javax.annotation.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.Constants;

public class NbtUtils
{
    @Nullable
    public static BlockPos readBlockPosFromArrayTag(NbtCompound tag, String tagName)
    {
        if (tag.contains(tagName, Constants.NBT.TAG_INT_ARRAY))
        {
            int[] pos = tag.getIntArray("Pos");

            if (pos.length == 3)
            {
                return new BlockPos(pos[0], pos[1], pos[2]);
            }
        }

        return null;
    }

    @Nullable
    public static Vec3d readVec3dFromListTag(@Nullable NbtCompound tag)
    {
        return readVec3dFromListTag(tag, "Pos");
    }

    @Nullable
    public static Vec3d readVec3dFromListTag(@Nullable NbtCompound tag, String tagName)
    {
        if (tag != null && tag.contains(tagName, Constants.NBT.TAG_LIST))
        {
            NbtList tagList = tag.getList(tagName, Constants.NBT.TAG_DOUBLE);

            if (tagList.getHeldType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    @Nullable
    public static Vec3i readVec3iFromIntArray(@Nullable NbtCompound tag, String tagName)
    {
        if (tag != null && tag.contains(tagName, Constants.NBT.TAG_INT_ARRAY))
        {
            int[] arr =  tag.getIntArray(tagName);

            if (arr != null && arr.length == 3)
            {
                return new Vec3i(arr[0], arr[1], arr[2]);
            }
        }

        return null;
    }

    @Nullable
    public static NbtCompound readNbtFromFile(File file)
    {
        if (file.exists() == false || file.canRead() == false)
        {
            return null;
        }

        FileInputStream is;

        try
        {
            is = new FileInputStream(file);
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Failed to read NBT data from file '{}' (failed to create the input stream)", file.getAbsolutePath());
            return null;
        }

        NbtCompound nbt = null;

        if (is != null)
        {
            try
            {
                nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
            }
            catch (Exception e)
            {
                try
                {
                    is.close();
                    is = new FileInputStream(file);
                    nbt = NbtIo.read(file.toPath());
                }
                catch (Exception ignore) {}
            }

            try
            {
                is.close();
            }
            catch (Exception ignore) {}
        }

        if (nbt == null)
        {
            Litematica.logger.warn("Failed to read NBT data from file '{}'", file.getAbsolutePath());
        }

        return nbt;
    }
}
