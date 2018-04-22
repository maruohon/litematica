package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagList;

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

    void readFromNBT(NBTTagList tagList);

    NBTTagList writeToNBT();
}
