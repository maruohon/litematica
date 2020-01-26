package fi.dy.masa.litematica.schematic.container;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;

public interface ILitematicaBlockStatePalette
{
    /**
     * Returns the current number of entries in the palette
     * @return
     */
    int getPaletteSize();

    /**
     * Gets the palette id for the given block state and adds
     * the state to the palette if it doesn't exist there yet.
     */
    int idFor(IBlockState state);

    /**
     * Gets the block state by the palette ID, if the provided ID exists.
     */
    @Nullable
    IBlockState getBlockState(int id);

    /**
     * Returns the current full mappings of IDs to values.
     * The ID is the position in the returned list.
     * @return
     */
    List<IBlockState> getMapping();

    /**
     * Sets the current mapping of the palette.
     * This is meant for reading the palette from file.
     * @param list
     * @return true if the mapping was set successfully, false if it failed
     */
    boolean setMapping(List<IBlockState> list);

    /**
     * Overrides the mapping for the given ID.
     * @param id
     * @param state
     * @return true if the ID was found in the palette and thus possible to override
     */
    boolean overrideMapping(int id, IBlockState state);

    /**
     * Creates a copy of this palette, using the provided resize handler
     * @param resizeHandler
     * @return
     */
    ILitematicaBlockStatePalette copy(IPaletteResizeHandler resizeHandler);
}
