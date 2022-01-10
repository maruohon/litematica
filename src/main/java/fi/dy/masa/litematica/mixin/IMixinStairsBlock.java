package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.StairShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

@Mixin(StairsBlock.class)
public interface IMixinStairsBlock
{
    @Invoker("getStairShape")
    static StairShape invokeGetStairShape(BlockState state, BlockView worldIn, BlockPos pos) { return null; }
}
