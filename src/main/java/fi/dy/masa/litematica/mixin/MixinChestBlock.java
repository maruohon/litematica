package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.BlockMirror;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.BlockUtils;

@Mixin(ChestBlock.class)
public class MixinChestBlock
{
    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void litematica_fixChestMirror(BlockState state, BlockMirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        ChestType type = state.get(ChestBlock.CHEST_TYPE);

        if (Configs.Generic.FIX_CHEST_MIRROR.getBooleanValue() && type != ChestType.SINGLE)
        {
            state = BlockUtils.fixMirrorDoubleChest(state, mirror, type);
            cir.setReturnValue(state);
        }
    }
}
