package de.meinbuild.liteschem.render.schematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.world.ChunkSchematic;
import de.meinbuild.liteschem.world.WorldSchematic;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.chunk.WorldChunk;
import de.meinbuild.liteschem.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;

public class WorldRendererSchematic
{
    private final MinecraftClient mc;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockRenderManager blockRenderManager;
    private final BlockModelRendererSchematic blockModelRenderer;
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final List<ChunkRendererSchematicVbo> renderInfos = new ArrayList<>(1024);
    private final VertexFormat vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
    private final BufferBuilderStorage bufferBuilders;
    private Set<ChunkRendererSchematicVbo> chunksToUpdate = new LinkedHashSet<>();
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
    private double lastCameraChunkUpdateX = Double.MIN_VALUE;
    private double lastCameraChunkUpdateY = Double.MIN_VALUE;
    private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
    private int cameraChunkX = Integer.MIN_VALUE;
    private int cameraChunkY = Integer.MIN_VALUE;
    private int cameraChunkZ = Integer.MIN_VALUE;
    private double lastCameraX = Double.MIN_VALUE;
    private double lastCameraY = Double.MIN_VALUE;
    private double lastCameraZ = Double.MIN_VALUE;
    private float lastCameraPitch = Float.MIN_VALUE;
    private float lastCameraYaw = Float.MIN_VALUE;
    private ChunkRenderDispatcherLitematica renderDispatcher;
    private final IChunkRendererFactory renderChunkFactory;
    //private ShaderGroup entityOutlineShader;
    //private boolean entityOutlinesRendered;

    private int renderDistanceChunks = -1;
    private int renderEntitiesStartupCounter = 2;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean displayListEntitiesDirty = true;

    public WorldRendererSchematic(MinecraftClient mc)
    {
        this.mc = mc;
        this.entityRenderDispatcher = mc.getEntityRenderManager();
        this.bufferBuilders = mc.getBufferBuilders();

        this.renderChunkFactory = new RenderChunkFactoryVbo();

        this.blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        this.blockModelRenderer = new BlockModelRendererSchematic(mc.getBlockColors());
    }

    public void markNeedsUpdate()
    {
        this.displayListEntitiesDirty = true;
    }

    public String getDebugInfoRenders()
    {
        int rcTotal = this.chunkRendererDispatcher != null ? this.chunkRendererDispatcher.renderers.length : 0;
        int rcRendered = this.chunkRendererDispatcher != null ? this.getRenderedChunks() : 0;
        return String.format("C: %d/%d %sD: %d, L: %d, %s", rcRendered, rcTotal, this.mc.chunkCullingEnabled ? "(s) " : "", this.renderDistanceChunks, 0, this.renderDispatcher == null ? "null" : this.renderDispatcher.getDebugInfo());
    }

    public String getDebugInfoEntities()
    {
        return "E: " + this.countEntitiesRendered + "/" + this.countEntitiesTotal + ", B: " + this.countEntitiesHidden;
    }

    protected int getRenderedChunks()
    {
        int count = 0;

        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
        {
            ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData;

            if (data != ChunkRenderDataSchematic.EMPTY && data.isEmpty() == false)
            {
                ++count;
            }
        }

        return count;
    }

    public void setWorldAndLoadRenderers(@Nullable WorldSchematic worldSchematic)
    {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;
        //this.renderManager.setWorld(worldClientIn);
        this.world = worldSchematic;

        if (worldSchematic != null)
        {
            this.loadRenderers();
        }
        else
        {
            this.chunksToUpdate.clear();
            this.renderInfos.clear();

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
                this.chunkRendererDispatcher = null;
            }

            if (this.renderDispatcher != null)
            {
                this.renderDispatcher.stopWorkerThreads();
            }

            this.renderDispatcher = null;
            this.blockEntities.clear();
        }
    }

    public void loadRenderers()
    {
        if (this.world != null)
        {
            if (this.renderDispatcher == null)
            {
                this.renderDispatcher = new ChunkRenderDispatcherLitematica();
            }

            this.displayListEntitiesDirty = true;
            this.renderDistanceChunks = this.mc.options.viewDistance;

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
            }

            this.stopChunkUpdates();

            synchronized (this.blockEntities)
            {
                this.blockEntities.clear();
            }

            this.chunkRendererDispatcher = new ChunkRenderDispatcherSchematic(this.world, this.renderDistanceChunks, this, this.renderChunkFactory);

            Entity entity = this.mc.getCameraEntity();

            if (entity != null)
            {
                this.chunkRendererDispatcher.updateCameraPosition(entity.getX(), entity.getZ());
            }

            this.renderEntitiesStartupCounter = 2;
        }
    }

    protected void stopChunkUpdates()
    {
        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates();
    }

    public void setupTerrain(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator)
    {
        this.world.getProfiler().push("setup_terrain");

        if (this.chunkRendererDispatcher == null || this.mc.options.viewDistance != this.renderDistanceChunks)
        {
            this.loadRenderers();
        }

        Entity entity = this.mc.getCameraEntity();

        if (entity == null)
        {
            entity = this.mc.player;
        }

        //camera.update(this.world, entity, this.mc.options.perspective > 0, this.mc.options.perspective == 2, this.mc.getTickDelta());

        this.world.getProfiler().push("camera");

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();

        double diffX = entityX - this.lastCameraChunkUpdateX;
        double diffY = entityY - this.lastCameraChunkUpdateY;
        double diffZ = entityZ - this.lastCameraChunkUpdateZ;

        if (this.cameraChunkX != entity.chunkX ||
            this.cameraChunkY != entity.chunkY ||
            this.cameraChunkZ != entity.chunkZ ||
            diffX * diffX + diffY * diffY + diffZ * diffZ > 16.0D)
        {
            this.lastCameraChunkUpdateX = entityX;
            this.lastCameraChunkUpdateY = entityY;
            this.lastCameraChunkUpdateZ = entityZ;
            this.cameraChunkX = entity.chunkX;
            this.cameraChunkY = entity.chunkY;
            this.cameraChunkZ = entity.chunkZ;
            this.chunkRendererDispatcher.updateCameraPosition(entityX, entityZ);
        }

        this.world.getProfiler().swap("renderlist_camera");

        Vec3d cameraPos = camera.getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        this.renderDispatcher.setCameraPosition(cameraPos);

        this.world.getProfiler().swap("culling");
        BlockPos viewPos = new BlockPos(cameraX, cameraY + (double) entity.getStandingEyeHeight(), cameraZ);
        final int centerChunkX = (viewPos.getX() >> 4);
        final int centerChunkZ = (viewPos.getZ() >> 4);
        final int renderDistance = this.mc.options.viewDistance;
        SubChunkPos viewSubChunk = new SubChunkPos(centerChunkX, viewPos.getY() >> 4, centerChunkZ);
        BlockPos viewPosSubChunk = new BlockPos(viewSubChunk.getX() << 4, viewSubChunk.getY() << 4, viewSubChunk.getZ() << 4);

        this.displayListEntitiesDirty = this.displayListEntitiesDirty || this.chunksToUpdate.isEmpty() == false ||
                entityX != this.lastCameraX ||
                entityY != this.lastCameraY ||
                entityZ != this.lastCameraZ ||
                entity.pitch != this.lastCameraPitch ||
                entity.yaw != this.lastCameraYaw;
        this.lastCameraX = cameraX;
        this.lastCameraY = cameraY;
        this.lastCameraZ = cameraZ;
        this.lastCameraPitch = camera.getPitch();
        this.lastCameraYaw = camera.getYaw();

        this.world.getProfiler().swap("update");

        if (this.displayListEntitiesDirty)
        {
            this.world.getProfiler().push("fetch");

            this.displayListEntitiesDirty = false;
            this.renderInfos.clear();

            Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) renderDistance / 8.0D, 1.0D, 2.5D));

            Set<SubChunkPos> set = DataManager.getSchematicPlacementManager().getAllTouchedSubChunks();
            List<SubChunkPos> positions = new ArrayList<>(set.size());
            positions.addAll(set);
            Collections.sort(positions, new SubChunkPos.DistanceComparator(viewSubChunk));

            //Queue<SubChunkPos> queuePositions = new PriorityQueue<>(new SubChunkPos.DistanceComparator(viewSubChunk));
            //queuePositions.addAll(set);

            //if (GuiBase.isCtrlDown()) System.out.printf("sorted positions: %d\n", positions.size());

            this.world.getProfiler().swap("iteration");

            //while (queuePositions.isEmpty() == false)
            for (int i = 0; i < positions.size(); ++i)
            {
                //SubChunkPos subChunk = queuePositions.poll();
                SubChunkPos subChunk = positions.get(i);

                // Only render sub-chunks that are within the client's render distance, and that
                // have been already properly loaded on the client
                if (Math.abs(subChunk.getX() - centerChunkX) <= renderDistance &&
                    Math.abs(subChunk.getZ() - centerChunkZ) <= renderDistance &&
                    this.world.getChunkProvider().isChunkLoaded(subChunk.getX(), subChunk.getZ()))
                {
                    BlockPos subChunkCornerPos = new BlockPos(subChunk.getX() << 4, subChunk.getY() << 4, subChunk.getZ() << 4);
                    ChunkRendererSchematicVbo chunkRenderer = this.chunkRendererDispatcher.getChunkRenderer(subChunkCornerPos);

                    if (chunkRenderer != null)
                    {
                        if (frustum.isVisible(chunkRenderer.getBoundingBox()))
                        {
                            //if (GuiBase.isCtrlDown()) System.out.printf("add @ %s\n", subChunk);
                            if (chunkRenderer.needsUpdate() && subChunkCornerPos.equals(viewPosSubChunk))
                            {
                                chunkRenderer.setNeedsUpdate(true);
                            }

                            this.renderInfos.add(chunkRenderer);
                        }
                    }
                }
            }

            this.world.getProfiler().pop();
        }

        this.world.getProfiler().swap("rebuild_near");
        Set<ChunkRendererSchematicVbo> set = this.chunksToUpdate;
        this.chunksToUpdate = new LinkedHashSet<>();

        for (ChunkRendererSchematicVbo chunkRendererTmp : this.renderInfos)
        {
            if (chunkRendererTmp.needsUpdate() || set.contains(chunkRendererTmp))
            {
                this.displayListEntitiesDirty = true;
                BlockPos pos = chunkRendererTmp.getOrigin().add(8, 8, 8);
                boolean isNear = pos.getSquaredDistance(viewPos) < 1024.0D;

                if (chunkRendererTmp.needsImmediateUpdate() == false && isNear == false)
                {
                    this.chunksToUpdate.add(chunkRendererTmp);
                }
                else
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("====== update now\n");
                    this.world.getProfiler().push("build_near");

                    this.renderDispatcher.updateChunkNow(chunkRendererTmp);
                    chunkRendererTmp.clearNeedsUpdate();

                    this.world.getProfiler().pop();
                }
            }
        }

        this.chunksToUpdate.addAll(set);

        this.world.getProfiler().pop();
        this.world.getProfiler().pop();
    }

    public void updateChunks(long finishTimeNano)
    {
        this.displayListEntitiesDirty |= this.renderDispatcher.runChunkUploads(finishTimeNano);

        if (this.chunksToUpdate.isEmpty() == false)
        {
            Iterator<ChunkRendererSchematicVbo> iterator = this.chunksToUpdate.iterator();

            while (iterator.hasNext())
            {
                ChunkRendererSchematicVbo renderChunk = iterator.next();
                boolean flag;

                if (renderChunk.needsImmediateUpdate())
                {
                    flag = this.renderDispatcher.updateChunkNow(renderChunk);
                }
                else
                {
                    flag = this.renderDispatcher.updateChunkLater(renderChunk);
                }

                if (!flag)
                {
                    break;
                }

                renderChunk.clearNeedsUpdate();
                iterator.remove();
                long i = finishTimeNano - System.nanoTime();

                if (i < 0L)
                {
                    break;
                }
            }
        }
    }

    public int renderBlockLayer(RenderLayer renderLayer, MatrixStack matrices, Camera camera)
    {
        this.world.getProfiler().push("render_block_layer_" + renderLayer.toString());

        boolean isTranslucent = renderLayer == RenderLayer.getTranslucent();

        renderLayer.startDrawing();
        //RenderUtils.disableDiffuseLighting();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        if (isTranslucent)
        {
            this.world.getProfiler().push("translucent_sort");
            double diffX = x - this.lastTranslucentSortX;
            double diffY = y - this.lastTranslucentSortY;
            double diffZ = z - this.lastTranslucentSortZ;

            if (diffX * diffX + diffY * diffY + diffZ * diffZ > 1.0D)
            {
                this.lastTranslucentSortX = x;
                this.lastTranslucentSortY = y;
                this.lastTranslucentSortZ = z;
                int i = 0;

                for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
                {
                    if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(renderLayer) ||
                        (chunkRenderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && chunkRenderer.hasOverlay())) && i++ < 15)
                    {
                        this.renderDispatcher.updateTransparencyLater(chunkRenderer);
                    }
                }
            }

            this.world.getProfiler().pop();
        }

        this.world.getProfiler().push("filter_empty");
        this.world.getProfiler().swap("render");

        boolean reverse = isTranslucent;
        int startIndex = reverse ? this.renderInfos.size() - 1 : 0;
        int stopIndex = reverse ? -1 : this.renderInfos.size();
        int increment = reverse ? -1 : 1;
        int count = 0;

        for (int i = startIndex; i != stopIndex; i += increment)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData().isBlockLayerEmpty(renderLayer) == false)
            {
                BlockPos chunkOrigin = renderer.getOrigin();
                VertexBuffer buffer = renderer.getBlocksVertexBufferByLayer(renderLayer);

                matrices.push();
                matrices.translate((double) chunkOrigin.getX() - x, (double) chunkOrigin.getY() - y, (double) chunkOrigin.getZ() - z);

                buffer.bind();
                this.vertexFormat.startDrawing(0L);
                buffer.draw(matrices.peek().getModel(), GL11.GL_QUADS);

                matrices.pop();
                ++count;
            }
        }

        VertexBuffer.unbind();
        RenderSystem.clearCurrentColor();
        this.vertexFormat.endDrawing();
        renderLayer.endDrawing();

        this.world.getProfiler().pop();
        this.world.getProfiler().pop();

        return count;
    }

    public void renderBlockOverlays(MatrixStack matrices, Camera camera)
    {
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, matrices, camera);
        this.renderBlockOverlay(OverlayRenderType.QUAD, matrices, camera);
    }

    private void renderBlockOverlay(OverlayRenderType type, MatrixStack matrices, Camera camera)
    {
        //RenderLayer renderLayer = RenderLayer.getTranslucent();
        //renderLayer.startDrawing();
        //RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        this.world.getProfiler().push("overlay_" + type.name());
        this.world.getProfiler().swap("render");

        for (int i = this.renderInfos.size() - 1; i >= 0; --i)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && renderer.hasOverlay())
            {
                ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();

                if (compiledChunk.isOverlayTypeEmpty(type) == false)
                {
                    BlockPos chunkOrigin = renderer.getOrigin();
                    VertexBuffer buffer = renderer.getOverlayVertexBuffer(type);

                    matrices.push();
                    matrices.translate((double) chunkOrigin.getX() - x, (double) chunkOrigin.getY() - y, (double) chunkOrigin.getZ() - z);

                    buffer.bind();
                    VertexFormats.POSITION_COLOR.startDrawing(0L);
                    buffer.draw(matrices.peek().getModel(), type.getGlMode());

                    matrices.pop();
                }
            }
        }

        VertexBuffer.unbind();
        RenderSystem.clearCurrentColor();
        VertexFormats.POSITION_COLOR.endDrawing();

        RenderSystem.disableBlend();
        //RenderSystem.enableTexture();
        //renderLayer.endDrawing();

        this.world.getProfiler().pop();
    }

    public boolean renderBlock(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrices, BufferBuilder bufferBuilderIn)
    {
        try
        {
            BlockRenderType renderType = state.getRenderType();

            if (renderType == BlockRenderType.INVISIBLE)
            {
                return false;
            }
            else
            {
                switch (renderType)
                {
                    case MODEL:
                        return this.blockModelRenderer.renderModel(world, this.getModelForState(state), state, pos, matrices, bufferBuilderIn, state.getRenderingSeed(pos));
                    case ENTITYBLOCK_ANIMATED:
                        return false;
                    default:
                        return false;
                }
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block in world");
            CrashReportSection crashreportcategory = crashreport.addElement("Block being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, pos, state);
            throw new CrashException(crashreport);
        }
    }

    public boolean renderFluid(BlockRenderView world, FluidState state, BlockPos pos, BufferBuilder bufferBuilderIn)
    {
        return this.blockRenderManager.renderFluid(pos, world, bufferBuilderIn, state);
    }

    public BakedModel getModelForState(BlockState state)
    {
        return this.blockRenderManager.getModel(state);
    }

    public void renderEntities(Camera camera, Frustum frustum, MatrixStack matrices, float partialTicks)
    {
        if (this.renderEntitiesStartupCounter > 0)
        {
            --this.renderEntitiesStartupCounter;
        }
        else
        {
            this.world.getProfiler().push("prepare");

            double cameraX = camera.getPos().x;
            double cameraY = camera.getPos().y;
            double cameraZ = camera.getPos().z;

            BlockEntityRenderDispatcher.INSTANCE.configure(this.world, this.mc.getTextureManager(), this.mc.textRenderer, camera, this.mc.crosshairTarget);
            this.entityRenderDispatcher.configure(this.world, camera, this.mc.targetedEntity);

            this.countEntitiesTotal = 0;
            this.countEntitiesRendered = 0;
            this.countEntitiesHidden = 0;

            this.countEntitiesTotal = this.world.getRegularEntityCount();

            this.world.getProfiler().swap("regular_entities");
            //List<Entity> entitiesMultipass = Lists.<Entity>newArrayList();

            VertexConsumerProvider.Immediate entityVertexConsumers = this.bufferBuilders.getEntityVertexConsumers();
            DiffuseLighting.enableForLevel(matrices.peek().getModel());
            LayerRange layerRange = DataManager.getRenderLayerRange();

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                BlockPos pos = chunkRenderer.getOrigin();
                WorldChunk chunk = (WorldChunk) this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                TypeFilterableList<Entity> list = chunk.getEntitySectionArray()[pos.getY() >> 4];

                if (list.isEmpty() == false)
                {
                    for (Entity entityTmp : list)
                    {
                        if (layerRange.isPositionWithinRange((int) entityTmp.getX(), (int) entityTmp.getY(), (int) entityTmp.getZ()) == false)
                        {
                            continue;
                        }

                        boolean shouldRender = this.entityRenderDispatcher.shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);

                        if (shouldRender)
                        {
                            double x = entityTmp.getX() - cameraX;
                            double y = entityTmp.getY() - cameraY;
                            double z = entityTmp.getZ() - cameraZ;

                            this.entityRenderDispatcher.render(entityTmp, x, y, z, entityTmp.yaw, partialTicks, matrices, entityVertexConsumers, this.entityRenderDispatcher.getLight(entityTmp, partialTicks));
                            ++this.countEntitiesRendered;
                        }
                    }
                }
            }

            this.world.getProfiler().swap("block_entities");

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                ChunkRenderDataSchematic data = chunkRenderer.getChunkRenderData();
                List<BlockEntity> tiles = data.getBlockEntities();

                if (tiles.isEmpty() == false) 
                {
                    BlockPos chunkOrigin = chunkRenderer.getOrigin();
                    ChunkSchematic chunk = this.world.getChunkProvider().getChunk(chunkOrigin.getX() >> 4, chunkOrigin.getZ() >> 4);

                    if (chunk != null && data instanceof ChunkRenderDataSchematic &&
                        data.getTimeBuilt() >= chunk.getTimeCreated())
                    {
                        for (BlockEntity te : tiles)
                        {
                            try
                            {
                                BlockPos pos = te.getPos();
                                matrices.push();
                                matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                                BlockEntityRenderDispatcher.INSTANCE.render(te, partialTicks, matrices, entityVertexConsumers);

                                matrices.pop();
                            }
                            catch (Exception e)
                            {
                            }
                        }
                    }
                }
            }

            synchronized (this.blockEntities)
            {
                for (BlockEntity te : this.blockEntities)
                {
                    try
                    {
                        BlockPos pos = te.getPos();
                        matrices.push();
                        matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                        BlockEntityRenderDispatcher.INSTANCE.render(te, partialTicks, matrices, entityVertexConsumers);

                        matrices.pop();
                    }
                    catch (Exception e)
                    {
                    }
                }
            }

            this.world.getProfiler().pop();
        }
    }

    /*
    private boolean isOutlineActive(Entity entityIn, Entity viewer, Camera camera)
    {
        boolean sleeping = viewer instanceof LivingEntity && ((LivingEntity) viewer).isSleeping();

        if (entityIn == viewer && this.mc.options.perspective == 0 && sleeping == false)
        {
            return false;
        }
        else if (entityIn.isGlowing())
        {
            return true;
        }
        else if (this.mc.player.isSpectator() && this.mc.options.keySpectatorOutlines.isPressed() && entityIn instanceof PlayerEntity)
        {
            return entityIn.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entityIn.getBoundingBox()) || entityIn.isRidingOrBeingRiddenBy(this.mc.player);
        }
        else
        {
            return false;
        }
    }
    */

    public void updateBlockEntities(Collection<BlockEntity> toRemove, Collection<BlockEntity> toAdd)
    {
        synchronized (this.blockEntities)
        {
            this.blockEntities.removeAll(toRemove);
            this.blockEntities.addAll(toAdd);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkY, int chunkZ)
    {
        this.chunkRendererDispatcher.scheduleChunkRender(chunkX, chunkY, chunkZ, false);
    }
}
