package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockVine;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

@Mixin(BlockVine.class)
public interface IMixinBlockVine
{
    @Invoker("func_196542_b")
    boolean invokeShouldConnectUp(IBlockReader blockReader, BlockPos pos, EnumFacing side);
}
