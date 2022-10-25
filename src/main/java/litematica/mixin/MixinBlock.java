package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import litematica.util.WorldUtils;

@Mixin(net.minecraft.block.Block.class)
public abstract class MixinBlock
{
    @Inject(method = "spawnAsEntity", at = @At("HEAD"), cancellable = true)
    private static void preventItemDrops(
            net.minecraft.world.World worldIn,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.item.ItemStack stack,
            CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates(worldIn))
        {
            ci.cancel();
        }
    }
}
