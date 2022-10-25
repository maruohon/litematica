package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Rotation;

import litematica.config.Configs;

@Mixin({BlockRail.class, BlockRailDetector.class, BlockRailPowered.class})
public abstract class MixinBlockRail extends BlockRailBase
{
    protected MixinBlockRail(boolean isPowered)
    {
        super(isPowered);
    }

    @Inject(method = "withRotation", at = @At("HEAD"), cancellable = true)
    private void fixRailRotation(IBlockState state, Rotation rot, CallbackInfoReturnable<IBlockState> cir)
    {
        if (Configs.Generic.FIX_RAIL_ROTATION.getBooleanValue() && rot == Rotation.CLOCKWISE_180)
        {
            BlockRailBase.EnumRailDirection dir = null;

            if (((Object) this) instanceof BlockRail)
            {
                dir = state.getValue(BlockRail.SHAPE);
            }
            else if (((Object) this) instanceof BlockRailDetector)
            {
                dir = state.getValue(BlockRailDetector.SHAPE);
            }
            else if (((Object) this) instanceof BlockRailPowered)
            {
                dir = state.getValue(BlockRailPowered.SHAPE);
            }

            // Fix the incomplete switch statement causing the ccw_90 rotation being used instead
            // for the 180 degree rotation of the straight rails.
            if (dir == BlockRailBase.EnumRailDirection.EAST_WEST || dir == BlockRailBase.EnumRailDirection.NORTH_SOUTH)
            {
                cir.setReturnValue(state);
                cir.cancel();
            }
        }
    }
}
