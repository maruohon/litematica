package litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.RenderContext;
import malilib.render.shader.ShaderProgram;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RenderWrap;
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

    public void renderSchematicWorld(RenderContext ctx, float partialTicks)
    {
        if (this.mc.skipRenderWorld == false)
        {
            GameWrap.profilerPush("litematica_schematic_world_render");

            if (this.mc.getRenderViewEntity() == null)
            {
                this.mc.setRenderViewEntity(this.mc.player);
            }

            RenderWrap.pushMatrix(ctx);
            RenderWrap.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(partialTicks, this.finishTimeNano, ctx);
            this.cleanup();

            RenderWrap.popMatrix(ctx);

            GameWrap.profilerPop();
        }
    }

    private void renderWorld(float partialTicks, long finishTimeNano, RenderContext ctx)
    {
        GameWrap.profilerPush("culling");
        Entity entity = this.mc.getRenderViewEntity();
        ICamera icamera = this.createCamera(entity, partialTicks);

        RenderWrap.shadeModel(GL11.GL_SMOOTH);

        GameWrap.profilerSwap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderWrap.disableItemLighting();

        RenderGlobalSchematic renderGlobal = this.getWorldRenderer();

        GameWrap.profilerSwap("terrain_setup");
        renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

        GameWrap.profilerSwap("update_chunks");
        renderGlobal.updateChunks(finishTimeNano);

        GameWrap.profilerSwap("terrain");
        RenderWrap.matrixMode(GL11.GL_MODELVIEW);
        RenderWrap.disableAlpha();

        if (Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue())
        {
            RenderWrap.pushMatrix(ctx);

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-0.2f, -0.4f);
            }

            this.startShaderIfEnabled();

            RenderWrap.setupBlendSeparate();

            renderGlobal.renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, entity);

            renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, entity);

            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, entity);
            this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
            }

            RenderWrap.disableBlend();
            RenderWrap.shadeModel(GL11.GL_FLAT);
            RenderWrap.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderWrap.matrixMode(GL11.GL_MODELVIEW);
            RenderWrap.popMatrix(ctx);

            GameWrap.profilerSwap("entities");

            RenderWrap.pushMatrix(ctx);

            RenderWrap.enableItemLighting();
            RenderWrap.setupBlendSeparate();

            renderGlobal.renderEntities(entity, icamera, partialTicks);

            RenderWrap.disableFog(); // Fixes Structure Blocks breaking all rendering
            RenderWrap.disableBlend();
            RenderWrap.disableItemLighting();

            RenderWrap.matrixMode(GL11.GL_MODELVIEW);
            RenderWrap.popMatrix(ctx);

            RenderWrap.enableCull();
            RenderWrap.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            RenderWrap.shadeModel(GL11.GL_SMOOTH);

            GameWrap.profilerSwap("translucent");
            RenderWrap.depthMask(false);

            RenderWrap.pushMatrix(ctx);

            RenderWrap.setupBlendSeparate();

            renderGlobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, entity);

            RenderWrap.popMatrix(ctx);

            this.disableShader();
        }

        GameWrap.profilerSwap("overlay");
        this.renderSchematicOverlay(ctx);

        RenderWrap.enableAlpha();
        RenderWrap.disableBlend();
        RenderWrap.depthMask(true);
        RenderWrap.shadeModel(GL11.GL_FLAT);
        RenderWrap.enableCull();

        GameWrap.profilerPop();
    }

    public void renderSchematicOverlay(RenderContext ctx)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeyBind().isKeyBindHeld();

        if (Configs.Visuals.SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeyBind().isKeyBindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            RenderWrap.pushMatrix(ctx);
            RenderWrap.disableTexture2D();
            RenderWrap.disableCull();
            RenderWrap.alphaFunc(GL11.GL_GREATER, 0.001F);
            RenderWrap.enablePolygonOffset();
            RenderWrap.polygonOffset(-0.4f, -0.8f);
            RenderWrap.setupBlendSeparate();
            RenderWrap.color(1f, 1f, 1f, 1f);
            RenderWrap.lineWidth(lineWidth);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);

            if (renderThrough)
            {
                RenderWrap.disableDepthTest();
            }

            this.getWorldRenderer().renderBlockOverlays();

            RenderWrap.enableDepthTest();
            RenderWrap.polygonOffset(0f, 0f);
            RenderWrap.disablePolygonOffset();
            RenderWrap.enableTexture2D();
            RenderWrap.popMatrix(ctx);
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

            GameWrap.profilerPush("litematica_culling");

            Entity entity = this.mc.getRenderViewEntity();
            ICamera icamera = this.createCamera(entity, partialTicks);

            this.calculateFinishTime();
            RenderGlobalSchematic renderGlobal = this.getWorldRenderer();

            GameWrap.profilerSwap("litematica_terrain_setup");
            renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

            GameWrap.profilerSwap("litematica_update_chunks");
            renderGlobal.updateChunks(this.finishTimeNano);

            GameWrap.profilerPop();

            this.renderPiecewisePrepared = true;
        }
    }

    public void piecewiseRenderSolid(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameWrap.profilerPush("litematica_blocks_solid");

            if (renderColliding)
            {
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
            }

            GameWrap.profilerPop();
        }
    }

    public void piecewiseRenderCutoutMipped(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameWrap.profilerPush("litematica_blocks_cutout_mipped");

            if (renderColliding)
            {
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
            }

            GameWrap.profilerPop();
        }
    }

    public void piecewiseRenderCutout(boolean renderColliding, float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameWrap.profilerPush("litematica_blocks_cutout");

            if (renderColliding)
            {
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-0.3f, -0.6f);
            }

            this.startShaderIfEnabled();

            this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, this.entity);

            this.disableShader();

            if (renderColliding)
            {
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
            }

            GameWrap.profilerPop();
        }
    }

    public void piecewiseRenderTranslucent(boolean renderColliding, float partialTicks, RenderContext ctx)
    {
        if (this.renderPiecewisePrepared)
        {
            if (this.renderPiecewiseBlocks)
            {
                GameWrap.profilerPush("litematica_translucent");

                if (renderColliding)
                {
                    RenderWrap.enablePolygonOffset();
                    RenderWrap.polygonOffset(-0.3f, -0.6f);
                }

                this.startShaderIfEnabled();

                this.getWorldRenderer().renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, this.entity);

                this.disableShader();

                if (renderColliding)
                {
                    RenderWrap.polygonOffset(0f, 0f);
                    RenderWrap.disablePolygonOffset();
                }

                GameWrap.profilerPop();
            }

            if (this.renderPiecewiseSchematic)
            {
                GameWrap.profilerPush("litematica_overlay");

                this.renderSchematicOverlay(ctx);

                GameWrap.profilerPop();
            }

            this.cleanup();
        }
    }

    public void piecewiseRenderEntities(float partialTicks)
    {
        if (this.renderPiecewiseBlocks)
        {
            GameWrap.profilerPush("litematica_entities");

            RenderWrap.setupBlendSeparate();
            this.startShaderIfEnabled();
            this.getWorldRenderer().renderEntities(this.entity, this.camera, partialTicks);
            this.disableShader();
            RenderWrap.disableBlend();

            GameWrap.profilerPop();
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
