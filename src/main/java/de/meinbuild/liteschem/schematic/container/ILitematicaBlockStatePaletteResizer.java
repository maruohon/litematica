package de.meinbuild.liteschem.schematic.container;

import net.minecraft.block.BlockState;

public interface ILitematicaBlockStatePaletteResizer
{
    int onResize(int bits, BlockState state);
}
