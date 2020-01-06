package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileOutputStream;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public interface ISchematic
{
    /**
     * Clears all the data in this schematic
     */
    void clear();

    /**
     * Returns the file this schematic was read from, if any.
     * @return
     */
    @Nullable File getFile();

    /**
     * Returns the file name extension this schematic should be saved with.
     * @return
     */
    String getFileNameExtension();

    /**
     * Returns the metadata object for this schematic
     * @return
     */
    SchematicMetadata getMetadata();

    /**
     * Returns the number of (sub-)regions in this schematic
     * @return
     */
    default int getSubRegionCount()
    {
        return 1;
    }

    /**
     * Returns the enclosing size of all the (sub-)regions in this schematic
     * @return
     */
    Vec3i getEnclosingSize();

    /**
     * Returns a list of all the (sub-)region names that exist in this schematic
     * @return
     */
    ImmutableList<String> getRegionNames();

    /**
     * Returns a map of all the (sub-)regions in this schematic
     * @return
     */
    ImmutableMap<String, ISchematicRegion> getRegions();

    /**
     * Returns the schematic (sub-)region by the given name, if it exists
     * @param regionName
     * @return
     */
    @Nullable ISchematicRegion getSchematicRegion(String regionName);

    /**
     * Reads the data from the provided other schematic
     * @param other
     */
    void readFrom(ISchematic other);

    /**
     * Clears the schematic, and then reads the contents from the provided compound tag
     * @param tag
     * @return
     */
    boolean fromTag(NBTTagCompound tag);

    /**
     * Writes this schematic to a compound tag for saving to a file
     * @return
     */
    NBTTagCompound toTag();

    /**
     * Writes this schematic with the provided filename, in the provided directory
     * @param dir
     * @param fileNameIn
     * @param override
     * @return
     */
    default boolean writeToFile(File dir, String fileNameIn, boolean override)
    {
        String fileName = fileNameIn;
        String extension = this.getFileNameExtension();

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        File file = new File(dir, fileName);

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath());
                return false;
            }

            if (override == false && file.exists())
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", file.getAbsolutePath());
                return false;
            }

            FileOutputStream os = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(this.toTag(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exception", file.getAbsolutePath());
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, e.getMessage());
        }

        return false;
    }

    /**
     * Tries to read the contents of this schematic from the file that was set on creation of this schematic.
     * @return
     */
    default boolean readFromFile()
    {
        File file = this.getFile();

        if (file == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.no_file");
            return false;
        }

        NBTTagCompound tag = NBTUtils.readNbtFromFile(file);

        if (tag == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.cant_read", file.getAbsolutePath());
            return false;
        }

        return this.fromTag(tag);
    }
}
