package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(net.minecraft.world.World.class)
public abstract class MixinWorld
{
    @Inject(method = "notifyNeighborsOfStateChange", at = @At("HEAD"), cancellable = true)
    private void preventBlockUpdates_notifyNeighborsOfStateChange(
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.Block blockType,
            boolean updateObservers, CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates())
        {
            ci.cancel();
        }
    }

    @Inject(method = "updateObservingBlocksAt", at = @At("HEAD"), cancellable = true)
    private void preventBlockUpdates_updateObservingBlocksAt(
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.Block blockType,
            CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates())
        {
            ci.cancel();
        }
    }

    @Inject(method = "notifyNeighborsOfStateExcept", at = @At("HEAD"), cancellable = true)
    private void preventBlockUpdates_notifyNeighborsOfStateExcept(
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.Block blockType,
            net.minecraft.util.EnumFacing skipSide,
            CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates())
        {
            ci.cancel();
        }
    }

    @Inject (method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    public void preventBlockUpdates_neighborChanged(
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.block.Block blockIn,
            net.minecraft.util.math.BlockPos fromPos,
            CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates())
        {
            ci.cancel();
        }
    }
}
