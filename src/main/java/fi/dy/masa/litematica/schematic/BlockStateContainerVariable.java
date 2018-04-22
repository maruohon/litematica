package fi.dy.masa.litematica.schematic;

import net.minecraft.util.BitArray;
import net.minecraft.world.chunk.BlockStateContainer;

public class BlockStateContainerVariable extends BlockStateContainer
{
    public BlockStateContainerVariable(int size)
    {
        super();

        // The default size is 4 bits, at least as of 1.12.x
        this.storage = new BitArray(4, size);
    }
}
