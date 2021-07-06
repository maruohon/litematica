package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private MinecraftClient mc;
    private WorldRendererSchematic worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private long finishTimeNano;

    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewise;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;

    private LitematicaRenderer()
    {
    }

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public WorldRendererSchematic getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = MinecraftClient.getInstance();
            this.worldRenderer = new WorldRendererSchematic(this.mc);
        }

        return this.worldRenderer;
    }

    public void loadRenderers()
    {
        this.getWorldRenderer().loadRenderers();
    }

    public void onSchematicWorldChanged(@Nullable WorldSchematic worldClient)
    {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

    private void calculateFinishTime()
    {
        // TODO 1.15+
        long fpsTarget = 60L;

        if (Configs.Generic.RENDER_THREAD_NO_TIMEOUT.getBooleanValue())
        {
            this.finishTimeNano = Long.MAX_VALUE;
        }
        else
        {
            this.finishTimeNano = System.nanoTime() + Math.max(1000000000L / fpsTarget / 2L, 0L);
        }
    }

    /*
    public void renderSchematicWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks)
    {
        if (this.mc.skipGameRender == false)
        {
            this.mc.getProfiler().push("litematica_schematic_world_render");

            if (this.mc.getCameraEntity() == null)
            {
                this.mc.setCameraEntity(this.mc.player);
            }

            RenderSystem.pushMatrix();
            RenderSystem.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(matrices, matrix, partialTicks, this.finishTimeNano);
            this.cleanup();

            RenderSystem.popMatrix();

            this.mc.getProfiler().pop();
        }
    }

    private void renderWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks, long finishTimeNano)
    {
        this.mc.getProfiler().push("culling");

        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Camera camera = this.getCamera();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        Frustum frustum = new Frustum(matrices.peek().getModel(), matrix);
        frustum.setPosition(x, y, z);

        this.mc.getProfiler().swap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        this.mc.getProfiler().swap("terrain_setup");
        worldRenderer.setupTerrain(camera, frustum, this.frameCount++, this.mc.player.isSpectator());

        this.mc.getProfiler().swap("update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        this.mc.getProfiler().swap("terrain");
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.disableAlphaTest();

        if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue())
        {
            RenderSystem.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.2f, -0.4f);
            }

            this.setupAlphaShader();
            this.enableAlphaShader();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getSolid(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutout(), matrices, camera);

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            RenderSystem.disableBlend();
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            this.mc.getProfiler().swap("entities");

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.enableDiffuseLightingForLevel(matrices);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderEntities(camera, frustum, matrices, partialTicks);

            RenderSystem.disableFog(); // Fixes Structure Blocks breaking all rendering
            RenderSystem.disableBlend();
            fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            RenderSystem.enableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);

            this.mc.getProfiler().swap("translucent");
            RenderSystem.depthMask(false);

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getTranslucent(), matrices, camera);

            RenderSystem.popMatrix();

            this.disableAlphaShader();
        }

        this.mc.getProfiler().swap("overlay");
        this.renderSchematicOverlay(matrices);

        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableCull();

        this.mc.getProfiler().pop();
    }
    */

    public void renderSchematicOverlay(MatrixStack matrices, Matrix4f projMatrix)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            //TODO: RenderSystem.alphaFunc(GL11.GL_GREATER, 0.001F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.4f, -0.8f);
            RenderSystem.lineWidth(lineWidth);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            //TODO: RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240.0F, 240.0F);

            if (renderThrough)
            {
                RenderSystem.disableDepthTest();
            }
            else
            {
                RenderSystem.enableDepthTest();
            }

            this.getWorldRenderer().renderBlockOverlays(matrices, this.getCamera(), projMatrix);

            RenderSystem.enableDepthTest();
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
        }
    }

    public void piecewisePrepareAndUpdate(Frustum frustum)
    {
        this.renderPiecewise = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                               Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                               this.mc.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (this.renderPiecewise && frustum != null && worldRenderer.hasWorld())
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();

            this.mc.getProfiler().push("litematica_culling");

            this.calculateFinishTime();

            this.mc.getProfiler().swap("litematica_terrain_setup");
            worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator());

            this.mc.getProfiler().swap("litematica_update_chunks");
            worldRenderer.updateChunks(this.finishTimeNano);

            this.mc.getProfiler().pop();

            this.frustum = frustum;
        }
    }

    public void piecewiseRenderSolid(MatrixStack matrices, Matrix4f projMatrix)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_solid");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getSolid(), matrices, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutoutMipped(MatrixStack matrices, Matrix4f projMatrix)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout_mipped");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutout(MatrixStack matrices, Matrix4f projMatrix)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutout(), matrices, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderTranslucent(MatrixStack matrices, Matrix4f projMatrix)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_translucent");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getTranslucent(), matrices, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderOverlay(MatrixStack matrices, Matrix4f matrix4f)
    {
        if (this.renderPiecewiseSchematic)
        {
            this.mc.getProfiler().push("litematica_overlay");

            Framebuffer fb = MinecraftClient.isFabulousGraphicsOrBetter() ? this.mc.worldRenderer.getTranslucentFramebuffer() : null;

            if (fb != null)
            {
                fb.beginWrite(false);
            }

            this.renderSchematicOverlay(matrices, matrix4f);

            if (fb != null)
            {
                this.mc.getFramebuffer().beginWrite(false);
            }

            this.mc.getProfiler().pop();
        }

        this.cleanup();
    }

    public void piecewiseRenderEntities(MatrixStack matrices, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_entities");

            this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrices, partialTicks);

            this.mc.getProfiler().pop();
        }
    }

    private Camera getCamera()
    {
        return this.mc.gameRenderer.getCamera();
    }

    private void cleanup()
    {
        this.renderPiecewise = false;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
    }
}
