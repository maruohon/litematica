package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ChunkRenderWorker.class)
public class MixinChunkRenderWorker
{
    @Inject(method = "isChunkExisting", at = @At("HEAD"), cancellable = true)
    private void onCheckChunkExists(BlockPos pos, World worldIn, CallbackInfoReturnable<Boolean> cir)
    {
        // Avoid chunk culling issues with the schematic world.
        // This is to avoid extending and overriding the ChunkRenderWorker,
        // and instead just use the vanilla class for our rendering as well.
        if (worldIn instanceof WorldSchematic)
        {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
