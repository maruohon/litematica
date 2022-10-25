package litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.RenderUtils;
import malilib.render.shader.ShaderProgram;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameUtils;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.render.schematic.RenderGlobalSchematic;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private static final ShaderProgram SHADER_ALPHA = new ShaderProgram("litematica", null, "shaders/alpha.frag");

    private Minecraft mc;
    private RenderGlobalSchematic worldRenderer;
    private int frameCount;
    private long finishTimeNano;

    private Entity entity;
    private ICamera camera;
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

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public RenderGlobalSchematic getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = Minecraft.getMinecraft();
            this.worldRenderer = new RenderGlobalSchematic(this.mc);
        }

        return this.worldRenderer;
    }

    public void loadRenderers()
    {
        this.getWorldRenderer().loadRenderers();
    }

    public void onSchematicWorldChanged(@Nullable WorldClient worldClient)
    {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

    private void calculateFinishTime()
    {
        long fpsLimit = this.mc.gameSettings.limitFramerate;
        long fpsMin = Math.min(Minecraft.getDebugFPS(), fpsLimit);
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
        if (this.mc.skipRenderWorld == false)
        {
            GameUtils.profilerPush("litematica_schematic_world_render");

            if (this.mc.getRenderViewEntity() == null)
            {
                this.mc.setRenderViewEntity(this.mc.player);
            }

            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();

            this.calculateFinishTime();
            this.renderWorld(partialTicks, this.finishTimeNano);
            this.cleanup();

            GlStateManager.popMatrix();

            GameUtils.profilerPop();
        }
    }

    private void renderWorld(float partialTicks, long finishTimeNano)
    {
        GameUtils.profilerPush("culling");
        Entity entity = this.mc.getRenderViewEntity();
        ICamera icamera = this.createCamera(entity, partialTicks);

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GameUtils.profilerSwap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderUtils.disableItemLighting();

        RenderGlobalSchematic renderGlobal = this.getWorldRenderer();

        GameUtils.profilerSwap("terrain_setup");
        renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

        GameUtils.profilerSwap("update_chunks");
        renderGlobal.updateChunks(finishTimeNano);

        GameUtils.profilerSwap("terrain");
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.disableAlpha();

        if (Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue())
        {
            GlStateManager.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-0.2f, -0.4f);
            }

            this.startShaderIfEnabled();

            RenderUtils.setupBlend();

            renderGlobal.renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, entity);

            renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, entity);

            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, entity);
            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            GlStateManager.disableBlend();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            GameUtils.profilerSwap("entities");

            GlStateManager.pushMatrix();

            RenderUtils.enableItemLighting();
            RenderUtils.setupBlend();

            renderGlobal.renderEntities(entity, icamera, partialTicks);

            GlStateManager.disableFog(); // Fixes Structure Blocks breaking all rendering
            GlStateManager.disableBlend();
            RenderUtils.disableItemLighting();

            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();

            GlStateManager.enableCull();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            GameUtils.profilerSwap("translucent");
            GlStateManager.depthMask(false);

            GlStateManager.pushMatrix();

            RenderUtils.setupBlend();

            renderGlobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, entity);

            GlStateManager.popMatrix();

            this.disableShader();
        }

        GameUtils.profilerSwap("overlay");
        this.renderSchematicOverlay();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();

        GameUtils.profilerPop();
    }

    public void renderSchematicOverlay()
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeyBind().isKeyBindHeld();

        if (Configs.Visuals.SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeyBind().isKeyBindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.001F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(-0.4f, -0.8f);
            RenderUtils.setupBlend();
            GlStateManager.glLineWidth(lineWidth);
            RenderUtils.color(1f, 1f, 1f, 1f);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            if (renderThrough)
            {
                GlStateManager.disableDepth();
            }

            this.getWorldRenderer().renderBlockOverlays();

            GlStateManager.enableDepth();
            GlStateManager.doPolygonOffset(0f, 0f);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    public void startShaderIfEnabled()
    {
        this.translucentSchematic = Configs.Visuals.TRANSLUCENT_SCHEMATIC_RENDERING.getBooleanValue() && OpenGlHelper.shadersSupported;

        if (this.translucentSchematic)
        {
            enableAlphaShader(Configs.Visuals.TRANSLUCENT_SCHEMATIC_RENDERING.getFloatValue());
        }
    }

    public void disableShader()
    {
        if (this.translucentSchematic)
        {
            disableAlphaShader();
        }
    }

    public static void enableAlphaShader(float alpha)
    {
        if (OpenGlHelper.shadersSupported)
        {
            GL20.glUseProgram(SHADER_ALPHA.getProgram());
            GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"), alpha);
        }
    }

    public static void disableAlphaShader()
    {
        if (OpenGlHelper.shadersSupported)
        {
            GL20.glUseProgram(0);
        }
    }

    public void piecewisePrepareAndUpdate(float partialTicks)
    {
        this.renderPiecewise = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                               Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
                               this.mc.getRenderViewEntity() != null;
        this.renderPiecewisePrepared = false;
        this.renderPiecewiseBlocks = false;

        if (this.renderPiecewise)
        {
            boolean invert = Hotkeys.INVERT_SCHEMATIC_RENDER_STATE.getKeyBind().isKeyBindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue();

            GameUtils.profilerPush("litematica_culling");

            Entity entity = this.mc.getRenderViewEntity();
            ICamera icamera = this.createCamera(entity, partialTicks);

            this.calculateFinishTime();
            RenderGlobalSchematic renderGlobal = this.getWorldRenderer();

            GameUtils.profilerSwap("litematica_terrain_setup");
            renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

            GameUtils.profilerSwap("litematica_update_chunks");
            renderGlobal.updateChunks(this.finishTimeNano);

            GameUtils.profilerPop();

            this.renderPiecewisePrepared = true;
        }
    }

    public void piecewiseRenderSolid(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameUtils.profilerPush("litematica_blocks_solid");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            GameUtils.profilerPop();
        }
    }

    public void piecewiseRenderCutoutMipped(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameUtils.profilerPush("litematica_blocks_cutout_mipped");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            GameUtils.profilerPop();
        }
    }

    public void piecewiseRenderCutout(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameUtils.profilerPush("litematica_blocks_cutout");

            if (renderColliding)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            GameUtils.profilerPop();
        }
    }

    public void piecewiseRenderTranslucent(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewisePrepared)
        {
            if (this.renderPiecewiseBlocks)
            {
                GameUtils.profilerPush("litematica_translucent");

                if (renderColliding)
                {
                    GlStateManager.enablePolygonOffset();
                    GlStateManager.doPolygonOffset(-0.3f, -0.6f);
                }

                this.startShaderIfEnabled();

                this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, this.entity);

                this.disableShader();

                if (renderColliding)
                {
                    GlStateManager.doPolygonOffset(0f, 0f);
                    GlStateManager.disablePolygonOffset();
                }

                GameUtils.profilerPop();
            }

            if (this.renderPiecewiseSchematic)
            {
                GameUtils.profilerPush("litematica_overlay");

                this.renderSchematicOverlay();

                GameUtils.profilerPop();
            }

            this.cleanup();
        }
    }

    public void piecewiseRenderEntities(float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameUtils.profilerPush("litematica_entities");

            RenderUtils.setupBlend();

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderEntities(this.entity, this.camera, partialTicks);

            this.disableShader();

            GlStateManager.disableBlend();

            GameUtils.profilerPop();
        }
    }

    private ICamera createCamera(Entity entity, float partialTicks)
    {
        double x = EntityWrap.lerpX(entity, partialTicks);
        double y = EntityWrap.lerpY(entity, partialTicks);
        double z = EntityWrap.lerpZ(entity, partialTicks);

        this.entity = entity;
        this.camera = new Frustum();
        this.camera.setPosition(x, y, z);

        return this.camera;
    }

    private void cleanup()
    {
        this.entity = null;
        this.camera = null;
        this.renderPiecewise = false;
        this.renderPiecewisePrepared = false;
        this.renderPiecewiseBlocks = false;
    }
}
