package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.config.Configs;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1010)
public class MixinServerPlayNetworkHandler
{
    @Redirect(method = "onPlayerInteractBlock", require = 0,
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d litematica_removeHitPosCheck(Vec3d hitVec, Vec3d blockCenter)
    {
        if (Configs.Generic.ITEM_USE_PACKET_CHECK_BYPASS.getBooleanValue())
        {
            return Vec3d.ZERO;
        }

        return hitVec.subtract(blockCenter);
    }
}
