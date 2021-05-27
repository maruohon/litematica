package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockReader;

@Mixin(VineBlock.class)
public interface IMixinVineBlock
{
    @Invoker("shouldHaveSide")
    boolean invokeShouldConnectUp(IBlockReader blockReader, BlockPos pos, Direction side);
}
