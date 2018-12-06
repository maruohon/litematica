package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.config.Configs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.properties.RailShape;
import net.minecraft.util.Rotation;

@Mixin({BlockRail.class, BlockRailDetector.class, BlockRailPowered.class})
public abstract class MixinBlockRail extends BlockRailBase
{
    protected MixinBlockRail(boolean disableCorners, Block.Properties builder)
    {
        super(disableCorners, builder);
    }

    @Inject(method = "rotate", at = @At("HEAD"), cancellable = true)
    private void fixRailRotation(IBlockState state, Rotation rot, CallbackInfoReturnable<IBlockState> cir)
    {
        if (Configs.Generic.FIX_RAIL_ROTATION.getBooleanValue() && rot == Rotation.CLOCKWISE_180)
        {
            RailShape shape = null;

            if (((Object) this) instanceof BlockRail)
            {
                shape = state.get(BlockRail.SHAPE);
            }
            else if (((Object) this) instanceof BlockRailDetector)
            {
                shape = state.get(BlockRailDetector.SHAPE);
            }
            else if (((Object) this) instanceof BlockRailPowered)
            {
                shape = state.get(BlockRailPowered.SHAPE);
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
