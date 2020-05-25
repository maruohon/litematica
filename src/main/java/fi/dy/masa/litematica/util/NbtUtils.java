package fi.dy.masa.litematica.util;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.litematica.mixin.IMixinNBTBase;

public class NbtUtils
{
    /**
     * Write the compound tag, gzipped, to the output stream.
     */
    public static void writeCompressed(NBTTagCompound tag, String tagName, OutputStream outputStream) throws IOException
    {
        DataOutputStream dataoutputstream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)));

        try
        {
            write(tag, tagName, dataoutputstream);
        }
        finally
        {
            dataoutputstream.close();
        }
    }

    public static void write(NBTTagCompound tag, String tagName, DataOutput output) throws IOException
    {
        writeTag(tag, tagName, output);
    }

    private static void writeTag(NBTBase tag, String tagName, DataOutput output) throws IOException
    {
        output.writeByte(tag.getId());

        if (tag.getId() != 0)
        {
            output.writeUTF(tagName);
            ((IMixinNBTBase) tag).invokeWrite(output);
        }
    }
}
