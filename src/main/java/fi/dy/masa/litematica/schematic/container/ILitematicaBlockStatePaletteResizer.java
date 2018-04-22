package fi.dy.masa.litematica.schematic.container;

import net.minecraft.block.state.IBlockState;

public interface ILitematicaBlockStatePaletteResizer
{
    int onResize(int bits, IBlockState state);
}
