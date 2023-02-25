package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import litematica.config.Configs;
import litematica.util.EasyPlaceUtils;
import litematica.util.PickBlockUtils;

@Mixin(net.minecraft.client.multiplayer.PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP
{
    @Inject(method = "processRightClickBlock", at = @At("HEAD"), cancellable = true)
    private void onProcessRightlickBlock(
            net.minecraft.client.entity.EntityPlayerSP player,
            net.minecraft.client.multiplayer.WorldClient worldIn,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.util.EnumFacing direction,
            net.minecraft.util.math.Vec3d vec,
            net.minecraft.util.EnumHand hand,
            CallbackInfoReturnable<net.minecraft.util.EnumActionResult> cir)
    {
        // Prevent recursion, since the Easy Place mode can call this code again
        if (EasyPlaceUtils.isHandling() == false)
        {
            if (EasyPlaceUtils.shouldDoEasyPlaceActions())
            {
                if (EasyPlaceUtils.handleEasyPlaceWithMessage())
                {
                    cir.setReturnValue(net.minecraft.util.EnumActionResult.FAIL);
                }
            }
            else
            {
                if (Configs.Generic.PICK_BLOCK_AUTO.getBooleanValue() &&
                    PickBlockUtils.shouldPickBlock())
                {
                    PickBlockUtils.pickBlockLast();
                }

                if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
                {
                    if (EasyPlaceUtils.handlePlacementRestriction())
                    {
                        cir.setReturnValue(net.minecraft.util.EnumActionResult.FAIL);
                    }
                }
            }
        }
    }

    // Handle right clicks on air, which needs to happen for Easy Place mode
    @Inject(method = "processRightClick", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"))
    private void onProcessRightClickPre(
            net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.world.World world,
            net.minecraft.util.EnumHand hand,
            CallbackInfoReturnable<net.minecraft.util.EnumActionResult> cir)
    {
        // Prevent recursion, since the Easy Place mode can call this code again
        if (EasyPlaceUtils.isHandling() == false)
        {
            if (EasyPlaceUtils.shouldDoEasyPlaceActions() &&
                EasyPlaceUtils.handleEasyPlaceWithMessage())
            {
                cir.setReturnValue(net.minecraft.util.EnumActionResult.FAIL);
            }
        }
    }
}
