package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrustumWithOrigin;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.texture.SpriteAtlasTexture;
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
    private int frameCount;
    private long finishTimeNano;

    private boolean renderPiecewise;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;
    private boolean renderPiecewisePrepared;
    private boolean translucentSchematic;

    static
    {
        int program = SHADER_ALPHA.getProgram();
        GL20.glUseProgram(program);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "texture"), 0);
        GL20.glUseProgram(0);
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
        long fpsLimit = this.mc.options.maxFps;
        long fpsMin = Math.min(MinecraftClient.getCurrentFps(), fpsLimit);
        fpsMin = Math.max(fpsMin, 60L);

        if (Configs.Generic.RENDER_THREAD_NO_TIMEOUT.getBooleanValue())
        {
            this.finishTimeNano = Long.MAX_VALUE;
        }
        else
        {
            this.finishTimeNano = System.nanoTime() + Math.max(1000000000L / fpsMin / 2L, 0L);
        }
    }

    public void renderSchematicWorld(float partialTicks)
    {
        if (this.mc.skipGameRender == false)
        {
            this.mc.getProfiler().push("litematica_schematic_world_render");

            if (this.mc.getCameraEntity() == null)
            {
                this.mc.setCameraEntity(this.mc.player);
            }

            GlStateManager.pushMatrix();
            GlStateManager.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(partialTicks, this.finishTimeNano);
            this.cleanup();

            GlStateManager.popMatrix();

            this.mc.getProfiler().pop();
        }
    }

    private void renderWorld(float partialTicks, long finishTimeNano)
    {
        this.mc.getProfiler().push("culling");

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Camera camera = this.getCamera();
        VisibleRegion visibleRegion = new FrustumWithOrigin();
        visibleRegion.setOrigin(camera.getPos().x, camera.getPos().y, camera.getPos().z);

        this.mc.getProfiler().swap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        fi.dy.masa.malilib.render.RenderUtils.disableItemLighting();
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        this.mc.getProfiler().swap("terrain_setup");
        worldRenderer.setupTerrain(camera, visibleRegion, this.frameCount++, this.mc.player.isSpectator());

        this.mc.getProfiler().swap("update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        this.mc.getProfiler().swap("terrain");
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.disableAlphaTest();

        if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue())
        {
            GlStateManager.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-0.2f, -0.4f);
            }

            this.startShaderIfEnabled();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(BlockRenderLayer.SOLID, camera);

            worldRenderer.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, camera);

            this.mc.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
            worldRenderer.renderBlockLayer(BlockRenderLayer.CUTOUT, camera);
            this.mc.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).popFilter();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                GlStateManager.polygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            GlStateManager.disableBlend();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            this.mc.getProfiler().swap("entities");

            GlStateManager.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.enableItemLighting();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderEntities(camera, visibleRegion, partialTicks);

            GlStateManager.disableFog(); // Fixes Structure Blocks breaking all rendering
            GlStateManager.disableBlend();
            fi.dy.masa.malilib.render.RenderUtils.disableItemLighting();

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            GlStateManager.enableCull();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            this.mc.getProfiler().swap("translucent");
            GlStateManager.depthMask(false);

            GlStateManager.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, camera);

            GlStateManager.popMatrix();

            this.disableShader();
        }

        this.mc.getProfiler().swap("overlay");
        this.renderSchematicOverlay();

        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();

        this.mc.getProfiler().pop();
    }

    public void renderSchematicOverlay()
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture();
            GlStateManager.disableCull();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.001F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.polygonOffset(-0.4f, -0.8f);
            GlStateManager.lineWidth(lineWidth);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240.0F, 240.0F);

            if (renderThrough)
            {
                GlStateManager.disableDepthTest();
            }

            this.getWorldRenderer().renderBlockOverlays();

            GlStateManager.enableDepthTest();
            GlStateManager.polygonOffset(0f, 0f);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableTexture();
            GlStateManager.popMatrix();
        }
    }

    public void startShaderIfEnabled()
    {
        this.translucentSchematic = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();

        if (this.translucentSchematic)
        {
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            GL20.glUseProgram(SHADER_ALPHA.getProgram());
            GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"), alpha);
        }
    }

    public void disableShader()
    {
        if (this.translucentSchematic)
        {
            GL20.glUseProgram(0);
        }
    }

    public void piecewisePrepareAndUpdate(VisibleRegion visibleRegion)
    {
        this.renderPiecewise = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                               Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                               this.mc.getCameraEntity() != null;
        this.renderPiecewisePrepared = false;
        this.renderPiecewiseBlocks = false;

        if (this.renderPiecewise && visibleRegion != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();

            this.mc.getProfiler().push("litematica_culling");

            this.calculateFinishTime();
            WorldRendererSchematic worldRenderer = this.getWorldRenderer();

            this.mc.getProfiler().swap("litematica_terrain_setup");
            worldRenderer.setupTerrain(this.getCamera(), visibleRegion, this.frameCount++, this.mc.player.isSpectator());

            this.mc.getProfiler().swap("litematica_update_chunks");
            worldRenderer.updateChunks(this.finishTimeNano);

            this.mc.getProfiler().pop();

            this.renderPiecewisePrepared = true;
        }
    }

    public void piecewiseRenderSolid(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_solid");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.SOLID, this.getCamera());

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.polygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutoutMipped(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout_mipped");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, this.getCamera());

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.polygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutout(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_blocks_cutout");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT, this.getCamera());

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.polygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            this.mc.getProfiler().pop();
        }
    }

    public void piecewiseRenderTranslucent(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewisePrepared)
        {
            if (this.renderPiecewiseBlocks)
            {
                this.mc.getProfiler().push("litematica_translucent");

                if (renderColliding)
                {
                    GlStateManager.enablePolygonOffset();
                    GlStateManager.polygonOffset(-0.3f, -0.6f);
                }

                this.startShaderIfEnabled();

                this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.TRANSLUCENT, this.getCamera());

                this.disableShader();

                if (renderColliding)
                {
                    GlStateManager.polygonOffset(0f, 0f);
                    GlStateManager.disablePolygonOffset();
                }

                this.mc.getProfiler().pop();
            }

            if (this.renderPiecewiseSchematic)
            {
                this.mc.getProfiler().push("litematica_overlay");

                this.renderSchematicOverlay();

                this.mc.getProfiler().pop();
            }

            this.cleanup();
        }
    }

    public void piecewiseRenderEntities(VisibleRegion visibleRegion, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            this.mc.getProfiler().push("litematica_entities");

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderEntities(this.getCamera(), visibleRegion, partialTicks);

            this.disableShader();

            GlStateManager.disableBlend();

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
