package fi.dy.masa.litematica.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import fi.dy.masa.litematica.Litematica;

public class NbtUtils
{
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
                nbt = NbtIo.readCompressed(is);
            }
            catch (Exception e)
            {
                try
                {
                    is.close();
                    is = new FileInputStream(file);
                    nbt = NbtIo.read(new DataInputStream(is));
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
