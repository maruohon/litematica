package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

@Mixin(VineBlock.class)
public interface IMixinVineBlock
{
    @Invoker("shouldHaveSide")
    boolean invokeShouldConnectUp(BlockView blockReader, BlockPos pos, Direction side);
}
