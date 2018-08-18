package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicList;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkRenderDispatcher// implements IChunkRenderDispatcher
{
    /*
    @Shadow
    @Final
    private Queue<Object> queueChunkUploads;
    */

    @Shadow
    private void uploadVertexBuffer(BufferBuilder buffer, VertexBuffer vertexBufferIn) {}

    @Shadow
    private void uploadDisplayList(BufferBuilder buffer, int list, RenderChunk renderChunk) {}

    /*
    @Inject(method = "uploadChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadVertexBuffer" +
                    "(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/vertex/VertexBuffer;)V",
            shift = Shift.AFTER))
    private void onPostUploadVertextBuffer(BlockRenderLayer layer, BufferBuilder buffer, RenderChunk renderChunk,
            CompiledChunk compiledChunk, double distanceSq, CallbackInfoReturnable<ListenableFuture<Object>> cir)
    {
        if (renderChunk instanceof RenderChunkSchematicVbo)
        {
            RenderChunkSchematicVbo renderChunkSchematic = (RenderChunkSchematicVbo) renderChunk;

            if (renderChunkSchematic.hasOverlay())
            {
                if (GuiScreen.isCtrlKeyDown()) System.out.printf("uploadVertexBuffer...\n");
                this.uploadVertexBuffer(renderChunkSchematic.getOverlayBufferBuilder(true), renderChunkSchematic.getOverlayVertexBuffer(true));
                this.uploadVertexBuffer(renderChunkSchematic.getOverlayBufferBuilder(false), renderChunkSchematic.getOverlayVertexBuffer(false));
            }
        }
    }
    */

    @Inject(method = "uploadChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OpenGlHelper;useVbo()Z"), cancellable = true)
    private void onPostUploadVertextBuffer(BlockRenderLayer layer, BufferBuilder buffer, RenderChunk renderChunk,
            CompiledChunk compiledChunk, double distanceSq, CallbackInfoReturnable<ListenableFuture<Object>> cir)
    {
        // Yeah, the hackyness level here is over 9000 >_>
        // Basically we need to smuggle in the info of which buffer we are supposed to use,
        // which is done by setting CompiledChunk to null for one of the cases.
        if (layer == null)
        {
            if (OpenGlHelper.useVbo())
            {
                RenderChunkSchematicVbo rcSchem = (RenderChunkSchematicVbo) renderChunk;
                this.uploadVertexBuffer(buffer, rcSchem.getOverlayVertexBuffer(compiledChunk == null));
            }
            else
            {
                RenderChunkSchematicList rcSchem = (RenderChunkSchematicList) renderChunk;
                this.uploadDisplayList(buffer, rcSchem.getOverlayDisplayList(compiledChunk == null), renderChunk);
            }

            buffer.setTranslation(0.0D, 0.0D, 0.0D);

            cir.setReturnValue(Futures.immediateFuture(null));
            cir.cancel();
        }
    }

    /*
    @Override
    public ListenableFuture<Object> uploadChunkOverlay(final BufferBuilder buffer, final RenderChunk renderChunk, final boolean outlineBuffer, final double distanceSq)
    {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
        {
            if (OpenGlHelper.useVbo())
            {
                this.uploadVertexBuffer(buffer, ((RenderChunkSchematicVbo) renderChunk).getOverlayVertexBuffer(outlineBuffer));
            }
            else
            {
                this.uploadDisplayList(buffer, ((RenderChunkSchematicList) renderChunk).getOverlayDisplayList(outlineBuffer), renderChunk);
            }

            buffer.setTranslation(0.0D, 0.0D, 0.0D);
            return Futures.<Object>immediateFuture((Object)null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                public void run()
                {
                    ((IChunkRenderDispatcher) (Object) this).uploadChunkOverlay(buffer, renderChunk, outlineBuffer, distanceSq);
                }
            }, (Object)null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add((Object) new ChunkRenderDispatcher.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }
    */
}
