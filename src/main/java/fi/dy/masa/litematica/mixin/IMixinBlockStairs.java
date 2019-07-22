package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.properties.StairsShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

@Mixin(BlockStairs.class)
public interface IMixinBlockStairs
{
    @Invoker("func_208064_n")
    public static StairsShape invokeGetStairShape(IBlockState state, IBlockReader worldIn, BlockPos pos) { return null; }
}
