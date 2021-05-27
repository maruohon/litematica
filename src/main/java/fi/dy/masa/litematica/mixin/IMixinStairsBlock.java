package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.state.properties.StairsShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

@Mixin(StairsBlock.class)
public interface IMixinStairsBlock
{
    @Invoker("getStairShape")
    public static StairsShape invokeGetStairShape(BlockState state, IBlockReader worldIn, BlockPos pos) { return null; }
}
