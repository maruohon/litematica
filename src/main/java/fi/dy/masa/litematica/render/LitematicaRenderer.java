package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.render.shader.ShaderProgram;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private static final ShaderProgram SHADER_ALPHA = new ShaderProgram("litematica", null, "shaders/alpha.frag");

    private MinecraftClient mc;
    private WorldRendererSchematic worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private int originalShaderProgram;
    private float translucencyAlpha;
    private long finishTimeNano;

    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewise;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;
    private boolean renderPiecewisePrepared;
    private boolean translucentSchematic;

    static
    {
        int program = SHADER_ALPHA.getProgram();
        int oldProgram = GlStateManager.getInteger(GL20.GL_CURRENT_PROGRAM);
        GlStateManager.useProgram(program);
        GlStateManager.uniform1(GlStateManager.getUniformLocation(program, "texture"), 0);
        GlStateManager.useProgram(oldProgram);
    }

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

    public void renderSchematicOverlay(MatrixStack matrices)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            RenderSystem.pushMatrix();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.001F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.4f, -0.8f);
            RenderSystem.lineWidth(lineWidth);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240.0F, 240.0F);

            if (renderThrough)
            {
                RenderSystem.disableDepthTest();
            }
            else
            {
                RenderSystem.enableDepthTest();
            }

            this.getWorldRenderer().renderBlockOverlays(matrices, this.getCamera());

            RenderSystem.enableDepthTest();
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableTexture();
            RenderSystem.popMatrix();
        }
    }

    private void setupAlphaShader()
    {
        this.translucentSchematic = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();

        if (this.translucentSchematic)
        {
            this.originalShaderProgram = GlStateManager.getInteger(GL20.GL_CURRENT_PROGRAM);
            this.translucencyAlpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            GlStateManager.useProgram(SHADER_ALPHA.getProgram());
            GL20.glUniform1f(GlStateManager.getUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"), this.translucencyAlpha);
        }
    }

    private void enableAlphaShader()
    {
        if (this.translucentSchematic)
        {
            GlStateManager.useProgram(SHADER_ALPHA.getProgram());
        }
    }

    private void disableAlphaShader()
    {
        if (this.translucentSchematic)
        {
            GlStateManager.useProgram(this.originalShaderProgram);
        }
    }

    public void piecewisePrepareAndUpdate(Frustum frustum)
    {
        this.renderPiecewise = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                               Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                               this.mc.getCameraEntity() != null;
        this.renderPiecewisePrepared = false;
        this.renderPiecewiseBlocks = false;

        if (this.renderPiecewise && frustum != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();

            this.mc.getProfiler().push("litematica_culling");

            this.calculateFinishTime();
            WorldRendererSchematic worldRenderer = this.getWorldRenderer();

            this.mc.getProfiler().swap("litematica_terrain_setup");
            worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator());

            this.mc.getProfiler().swap("litematica_update_chunks");
            worldRenderer.updateChunks(this.finishTimeNano);

            this.mc.getProfiler().pop();

            this.frustum = frustum;
            this.renderPiecewisePrepared = true;
        }
    }

    public void piecewiseRenderSolid(MatrixStack matrices)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_solid");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.setupAlphaShader();
            //this.enableAlphaShader();

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getSolid(), matrices, this.getCamera());

            this.disableAlphaShader();

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutoutMipped(MatrixStack matrices)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout_mipped");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.enableAlphaShader();

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, this.getCamera());

            this.disableAlphaShader();

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutout(MatrixStack matrices)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.enableAlphaShader();

            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutout(), matrices, this.getCamera());

            this.disableAlphaShader();

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderTranslucent(MatrixStack matrices)
    {
        if (this.renderPiecewisePrepared)
        {
            if (this.renderPiecewiseBlocks)
            {
                this.mc.getProfiler().push("litematica_translucent");

                if (this.renderCollidingSchematicBlocks)
                {
                    RenderSystem.enablePolygonOffset();
                    RenderSystem.polygonOffset(-0.3f, -0.6f);
                }

                this.enableAlphaShader();

                this.getWorldRenderer().renderBlockLayer(RenderLayer.getTranslucent(), matrices, this.getCamera());

                this.disableAlphaShader();

                if (this.renderCollidingSchematicBlocks)
                {
                    RenderSystem.polygonOffset(0f, 0f);
                    RenderSystem.disablePolygonOffset();
                }

                this.mc.getProfiler().pop();
            }

            if (this.renderPiecewiseSchematic)
            {
                this.mc.getProfiler().push("litematica_overlay");

                this.renderSchematicOverlay(matrices);

                this.mc.getProfiler().pop();
            }

            this.cleanup();
        }
    }

    public void piecewiseRenderEntities(MatrixStack matrices, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_entities");

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            //this.enableAlphaShader();

            this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrices, partialTicks);

            //this.disableAlphaShader();

            RenderSystem.disableBlend();

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
        this.renderPiecewisePrepared = false;
        this.renderPiecewiseBlocks = false;
    }
}
