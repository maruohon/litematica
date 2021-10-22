package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(Block.class)
public class MixinBlock
{
    @Inject(method = "dropStack",
            at = @At("HEAD"), cancellable = true)
    private static void litematica_preventItemDrops(World world,
                                                    BlockPos pos,
                                                    ItemStack stack,
                                                    CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates(world))
        {
            ci.cancel();
        }
    }
}
