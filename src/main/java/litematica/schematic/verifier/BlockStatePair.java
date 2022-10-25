package litematica.schematic.verifier;

import net.minecraft.block.state.IBlockState;

public class BlockStatePair
{
    public final VerifierResultType type;
    public final IBlockState expectedState;
    public final IBlockState foundState;

    public BlockStatePair(VerifierResultType type, IBlockState expectedState, IBlockState foundState)
    {
        this.type = type;
        this.expectedState = expectedState;
        this.foundState = foundState;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || this.getClass() != o.getClass()) { return false; }

        BlockStatePair that = (BlockStatePair) o;

        return this.type == that.type &&
               this.expectedState == that.expectedState &&
               this.foundState == that.foundState;
    }

    @Override
    public int hashCode()
    {
        int result = this.type.hashCode();
        result = 31 * result + this.expectedState.hashCode();
        result = 31 * result + this.foundState.hashCode();
        return result;
    }
}
