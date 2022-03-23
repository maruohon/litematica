package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.Litematica;

public interface ISchematic
{
    /**
     * Clears all the data in this schematic
     */
    void clear();

    /**
     * @return the file this schematic was read from, or null if this is an in-memory-only schematic.
     */
    @Nullable File getFile();

    /**
     * @return the metadata object for this schematic
     */
    SchematicMetadata getMetadata();

    /**
     * @return the type of this schematic
     */
    SchematicType<?> getType();

    /**
     * @return the number of (sub-)regions in this schematic
     */
    default int getSubRegionCount()
    {
        return 1;
    }

    /**
     * @return the enclosing size of all the (sub-)regions in this schematic
     */
    Vec3i getEnclosingSize();

    /**
     * @return a list of all the (sub-)region names that exist in this schematic
     */
    ImmutableList<String> getRegionNames();

    /**
     * @return a map of all the (sub-)regions in this schematic
     */
    ImmutableMap<String, ISchematicRegion> getRegions();

    /**
     * @return the schematic (sub-)region by the given name, if it exists
     */
    @Nullable ISchematicRegion getSchematicRegion(String regionName);

    /**
     * Reads the data from the provided other schematic
     */
    void readFrom(ISchematic other);

    /**
     * Clears the schematic, and then reads the contents from the provided compound tag
     */
    boolean fromTag(NBTTagCompound tag);

    /**
     * Writes this schematic to a compound tag for saving to a file
     * @return
     */
    NBTTagCompound toTag();

    /**
     * Writes this schematic to a file by the given file name, in the given directory
     * @return true on success, false on failure
     */
    default boolean writeToFile(File dir, String fileName, boolean override)
    {
        if (dir.exists() == false && dir.mkdirs() == false)
        {
            String key = "litematica.error.schematic_write_to_file_failed.directory_creation_failed";
            MessageDispatcher.error(key, dir.getAbsolutePath());
            return false;
        }

        String extension = this.getType().getFileNameExtension();

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        File file = new File(dir, fileName);

        return this.writeToFile(file, override);
    }

    /**
     * Writes this schematic to the given file.
     * @return true on success, false on failure
     */
    default boolean writeToFile(File file, boolean override)
    {
        try
        {
            if (override == false && file.exists())
            {
                MessageDispatcher.error("litematica.error.schematic_write_to_file_failed.exists",
                                        file.getAbsolutePath());
                return false;
            }

            FileOutputStream os = new FileOutputStream(file);
            this.writeToStream(this.toTag(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            MessageDispatcher.error("litematica.error.schematic_write_to_file_failed.exception",
                                    file.getAbsolutePath());
            Litematica.logger.warn("Failed to write schematic to file '{}'", file.getAbsolutePath(), e);
        }

        return false;
    }

    default void writeToStream(NBTTagCompound tag, FileOutputStream outputStream) throws IOException
    {
        CompressedStreamTools.writeCompressed(tag, outputStream);
    }

    /**
     *
     * Tries to read the contents of this schematic from the file that was set on creation of this schematic.
     * @return true on success, false on failure
     */
    default boolean readFromFile()
    {
        File file = this.getFile();

        if (file == null)
        {
            MessageDispatcher.error("litematica.error.schematic_read_from_file_failed.no_file");
            return false;
        }

        NBTTagCompound tag = NbtUtils.readNbtFromFile(file);

        if (tag == null)
        {
            MessageDispatcher.error("litematica.error.schematic_read_from_file_failed.cant_read",
                                    file.getAbsolutePath());
            return false;
        }

        return this.fromTag(tag);
    }
}
