package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.state.properties.RedstoneSide;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public interface IMixinBlockRedstoneWire
{
    @Invoker("getSide")
    RedstoneSide invokeGetSide(IBlockReader blockReader, BlockPos pos, EnumFacing side);
}
