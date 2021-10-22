package fi.dy.masa.litematica.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.Constants;

public class NbtUtils
{
    @Nullable
    public static BlockPos readBlockPosFromArrayTag(CompoundNBT tag, String tagName)
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
    public static Vector3d readVec3dFromListTag(@Nullable CompoundNBT tag)
    {
        return readVec3dFromListTag(tag, "Pos");
    }

    @Nullable
    public static Vector3d readVec3dFromListTag(@Nullable CompoundNBT tag, String tagName)
    {
        if (tag != null && tag.contains(tagName, Constants.NBT.TAG_LIST))
        {
            ListNBT tagList = tag.getList(tagName, Constants.NBT.TAG_DOUBLE);

            if (tagList.getElementType() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vector3d(tagList.getDouble(0), tagList.getDouble(1), tagList.getDouble(2));
            }
        }

        return null;
    }

    @Nullable
    public static CompoundNBT readNbtFromFile(File file)
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

        CompoundNBT nbt = null;

        if (is != null)
        {
            try
            {
                nbt = CompressedStreamTools.readCompressed(is);
            }
            catch (Exception e)
            {
                try
                {
                    is.close();
                    is = new FileInputStream(file);
                    nbt = CompressedStreamTools.read(new DataInputStream(is));
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
