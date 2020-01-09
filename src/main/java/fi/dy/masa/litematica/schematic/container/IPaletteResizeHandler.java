package fi.dy.masa.litematica.schematic.container;

import net.minecraft.block.state.IBlockState;

public interface IPaletteResizeHandler
{
    /**
     * Called when a palette runs out of IDs in the current entry width,
     * and the underlying container needs to be resized for the new entry bit width.
     * @param newSizeBits
     * @param stateBeingAdded
     * @param oldPalette
     * @return the ID for the new state being added when the resize happens
     */
    int onResize(int newSizeBits, IBlockState stateBeingAdded, ILitematicaBlockStatePalette oldPalette);
}
