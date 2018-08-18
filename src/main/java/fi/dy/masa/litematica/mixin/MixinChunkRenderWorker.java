package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ChunkRenderWorker.class)
public class MixinChunkRenderWorker
{
    @Shadow
    @Final
    private ChunkRenderDispatcher chunkRenderDispatcher;

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

    //@SuppressWarnings({ "unchecked", "rawtypes" })
    @Inject(method = "processTask", at = @At(value = "INVOKE",
                    target = "Lcom/google/common/util/concurrent/Futures;allAsList" +
                    "(Ljava/lang/Iterable;)" +
                    "Lcom/google/common/util/concurrent/ListenableFuture;"))
    private void onPostUpload(ChunkCompileTaskGenerator generator, CallbackInfo ci/*, ArrayList futures*/)
    {
        if (GuiScreen.isCtrlKeyDown()) System.out.printf("upload hook 2\n");
        RenderChunk renderChunk = generator.getRenderChunk();

        if (renderChunk instanceof RenderChunkSchematicVbo && ((RenderChunkSchematicVbo) renderChunk).hasOverlay())
        {
            // See MixinChunkRenderDispatcher.onPostUploadVertextBuffer()
            // We are smuggling in the 'outlineBuffer' boolean by setting the unused CompiledChunk parameter to null in one of the cases
            BufferBuilder buffer = ((IRegionRenderCacheBuilder) generator.getRegionRenderCacheBuilder()).getOverlayBuffer(true);
            this.chunkRenderDispatcher.uploadChunk(null, buffer, renderChunk, null, generator.getDistanceSq());

            buffer = ((IRegionRenderCacheBuilder) generator.getRegionRenderCacheBuilder()).getOverlayBuffer(false);
            this.chunkRenderDispatcher.uploadChunk(null, buffer, renderChunk, generator.getCompiledChunk(), generator.getDistanceSq());
        }
    }
}
