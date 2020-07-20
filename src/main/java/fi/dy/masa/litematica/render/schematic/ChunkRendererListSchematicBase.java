package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.BlockPos;

public abstract class ChunkRendererListSchematicBase
{
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    protected final List<ChunkRendererSchematicVbo> chunkRenderers = Lists.newArrayListWithCapacity(17424);
    protected List<ChunkRendererSchematicVbo> overlayChunkRenderers = new ArrayList<>(128);
    protected boolean isCameraPositionSet;

    public void setCameraPosition(double cameraX, double cameraY, double cameraZ)
    {
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.isCameraPositionSet = true;

        this.chunkRenderers.clear();
        this.overlayChunkRenderers.clear();
    }

    public void preRenderChunk(ChunkRendererSchematicVbo renderChunkIn)
    {
        BlockPos blockpos = renderChunkIn.getPosition();

        GlStateManager.translatef(  (float) (blockpos.getX() - this.cameraX),
                                    (float) (blockpos.getY() - this.cameraY),
                                    (float) (blockpos.getZ() - this.cameraZ));
    }

    public void addChunkRenderer(ChunkRendererSchematicVbo renderChunkIn)
    {
        this.chunkRenderers.add(renderChunkIn);
    }

    public void addOverlayChunk(ChunkRendererSchematicVbo renderChunk)
    {
        this.overlayChunkRenderers.add(renderChunk);
    }

    public abstract void renderChunkLayer(RenderLayer layer);

    public abstract void renderBlockOverlays(OverlayRenderType type);
}
