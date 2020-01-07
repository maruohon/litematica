package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.InfoUtils;

public interface ILitematicaBlockStatePalette
{
    /**
     * Gets the palette id for the given block state and adds
     * the state to the palette if it doesn't exist there yet.
     */
    int idFor(IBlockState state);

    /**
     * Gets the block state by the palette id.
     */
    @Nullable
    IBlockState getBlockState(int indexKey);

    int getPaletteSize();

    /**
     * Sets the ID for the given state.<br>
     * <b>NOTE: This is not intended for external use!</b><br>
     * It's used when reading the Palette from NBT.
     * @param id
     * @param state
     * @return
     */
    boolean setIdFor(int id, IBlockState state);

    /**
     * Reads the palette from the Litematica format List NBT tag
     * @param tagList
     */
    default boolean readFromLitematicaTag(NBTTagList tagList)
    {
        final int size = tagList.tagCount();

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(id);
            IBlockState state = NBTUtil.readBlockState(tag);

            if (this.setIdFor(id, state) == false)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Reads the palette from the Sponge format Compound NBT tag
     * @param tag
     */
    default boolean readFromSpongeTag(NBTTagCompound tag)
    {
        for (String key : tag.getKeySet())
        {
            int id = tag.getInteger(key);
            IBlockState state = BlockUtils.getBlockStateFromString(key);

            if (state == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            if (this.setIdFor(id, state) == false)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                return false;
            }
        }

        return true;
    }

    /**
     * Writes the palette to the Litematica format List NBT tag
     * @return
     */
    NBTTagList writeToLitematicaTag();

    /**
     * Writes the palette to the Sponge format Compound NBT tag
     * @return
     */
    NBTTagCompound writeToSpongeTag();

    ILitematicaBlockStatePalette copy();
}
