package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;

@Mixin(BlockFenceGate.class)
public interface IMixinBlockFenceGate
{
    @Invoker("isWall")
    boolean invokeIsWall(IBlockState state);
}
