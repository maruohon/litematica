package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.config.Configs;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.util.BlockRotation;

@Mixin({ RailBlock.class, DetectorRailBlock.class, PoweredRailBlock.class})
public abstract class MixinRailBlocks extends AbstractRailBlock
{
    protected MixinRailBlocks(boolean disableCorners, Block.Settings builder)
    {
        super(disableCorners, builder);
    }

    @Inject(method = "rotate", at = @At("HEAD"), cancellable = true)
    private void fixRailRotation(BlockState state, BlockRotation rot, CallbackInfoReturnable<BlockState> cir)
    {
        if (Configs.Generic.FIX_RAIL_ROTATION.getBooleanValue() && rot == BlockRotation.CLOCKWISE_180)
        {
            RailShape shape = null;

            if (((Object) this) instanceof RailBlock)
            {
                shape = state.get(RailBlock.SHAPE);
            }
            else if (((Object) this) instanceof DetectorRailBlock)
            {
                shape = state.get(DetectorRailBlock.SHAPE);
            }
            else if (((Object) this) instanceof PoweredRailBlock)
            {
                shape = state.get(PoweredRailBlock.SHAPE);
            }

            // Fix the incomplete switch statement causing the ccw_90 rotation being used instead
            // for the 180 degree rotation of the straight rails.
            if (shape == RailShape.EAST_WEST || shape == RailShape.NORTH_SOUTH)
            {
                cir.setReturnValue(state);
                cir.cancel();
            }
        }
    }
}
