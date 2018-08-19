package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.schematic.RenderGlobalSchematic;
import fi.dy.masa.litematica.render.shader.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private static final ShaderProgram SHADER_ALPHA = new ShaderProgram("litematica", null, "shaders/alpha.frag");
    private Minecraft mc;
    private RenderGlobalSchematic renderGlobal;
    private int frameCount;

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public RenderGlobalSchematic getRenderGlobal()
    {
        if (this.renderGlobal == null)
        {
            this.mc = Minecraft.getMinecraft();
            this.renderGlobal = new RenderGlobalSchematic(this.mc);
        }

        return this.renderGlobal;
    }

    public void loadRenderers()
    {
        this.getRenderGlobal().loadRenderers();
    }

    public void onSchematicWorldChanged(@Nullable WorldClient worldClient)
    {
        this.getRenderGlobal().setWorldAndLoadRenderers(worldClient);
    }

    public void renderSchematicWorld(float partialTicks)
    {
        if (this.mc.skipRenderWorld == false)
        {
            this.mc.mcProfiler.startSection("litematica_schematic_world_render");
            this.mc.mcProfiler.startSection("level");
            int fpsLimit = this.mc.gameSettings.limitFramerate;
            int fpsMin = Math.min(Minecraft.getDebugFPS(), fpsLimit);
            fpsMin = Math.max(fpsMin, 60);
            long finishTimeNano = Math.max((long)(1000000000 / fpsMin / 4), 0L);

            boolean translucentSchematic = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && OpenGlHelper.shadersSupported;
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();

            GlStateManager.pushMatrix();

            if (translucentSchematic)
            {
                //System.out.printf("alpha: %s\n", SHADER_ALPHA.getProgram());
                GL20.glUseProgram(SHADER_ALPHA.getProgram());
                GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"), alpha);
            }

            this.renderWorld(partialTicks, finishTimeNano);

            if (translucentSchematic)
            {
                GL20.glUseProgram(0);
            }

            GlStateManager.popMatrix();

            this.mc.mcProfiler.endSection();
            this.mc.mcProfiler.endSection();
        }
    }

    private void renderWorld(float partialTicks, long finishTimeNano)
    {
        //this.updateLightmap(partialTicks);

        if (this.mc.getRenderViewEntity() == null)
        {
            this.mc.setRenderViewEntity(this.mc.player);
        }

        //this.getMouseOver(partialTicks);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.5F);
        this.mc.mcProfiler.startSection("center");

        if (this.mc.gameSettings.anaglyph)
        {
            /*
            //anaglyphField = 0;
            GlStateManager.colorMask(false, true, true, false);
            this.renderWorldPass(0, partialTicks, finishTimeNano);

            //anaglyphField = 1;
            GlStateManager.colorMask(true, false, false, false);
            this.renderWorldPass(1, partialTicks, finishTimeNano);

            GlStateManager.colorMask(true, true, true, false);
            */
        }
        else
        {
            this.renderWorldPass(2, partialTicks, finishTimeNano);
        }

        //GlStateManager.disableAlpha();

        this.mc.mcProfiler.endSection();
    }

    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano)
    {
        RenderGlobalSchematic renderGlobal = this.getRenderGlobal();
        //ParticleManager particlemanager = this.mc.effectRenderer;
        //boolean flag = this.isDrawBlockOutline();
        //GlStateManager.enableCull();

        //this.mc.mcProfiler.endStartSection("clear");
        //GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        //this.updateFogColor(partialTicks);
        //GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        //this.mc.mcProfiler.endStartSection("camera");
        //this.setupCameraTransform(partialTicks, pass);
        //ActiveRenderInfo.updateRenderInfo(this.mc.player, this.mc.gameSettings.thirdPersonView == 2);

        //this.mc.mcProfiler.endStartSection("frustum");
        //ClippingHelperImpl.getInstance();

        this.mc.mcProfiler.endStartSection("culling");
        ICamera icamera = new Frustum();
        Entity entity = this.mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
        icamera.setPosition(x, y, z);

        /*
        if (this.mc.gameSettings.renderDistanceChunks >= 4)
        {
            this.setupFog(-1, partialTicks);
            this.mc.mcProfiler.endStartSection("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            renderGlobal.renderSky(partialTicks, pass);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.farPlaneDistance * MathHelper.SQRT_2);
            GlStateManager.matrixMode(5888);
        }
        */

        //this.setupFog(0, partialTicks);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        this.mc.mcProfiler.endStartSection("prepareterrain");
        //this.setupFog(0, partialTicks);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();

        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("renderWorldPass\n");
        this.mc.mcProfiler.endStartSection("terrain_setup");
        renderGlobal.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

        if (pass == 0 || pass == 2)
        {
            this.mc.mcProfiler.endStartSection("updatechunks");
            renderGlobal.updateChunks(finishTimeNano);
        }

        this.mc.mcProfiler.endStartSection("terrain");
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();

        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        renderGlobal.renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, pass, entity);

        renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, pass, entity);

        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        renderGlobal.renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, pass, entity);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        this.mc.mcProfiler.endStartSection("entities");
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        renderGlobal.renderEntities(entity, icamera, partialTicks);

        GlStateManager.disableBlend();
        RenderHelper.disableStandardItemLighting();
        //this.disableLightmap();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        //GlStateManager.depthMask(false);
        //GlStateManager.enableCull();

        //this.mc.mcProfiler.endStartSection("weather");
        //this.renderRainSnow(partialTicks);
        //GlStateManager.depthMask(true);
        //renderGlobal.renderWorldBorder(entity, partialTicks);

        //GlStateManager.disableBlend();
        //GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        GlStateManager.enableCull();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        //this.setupFog(0, partialTicks);

        this.mc.mcProfiler.endStartSection("translucent");
        GlStateManager.depthMask(false);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        renderGlobal.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, pass, entity);

        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();

        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-0.1f, -0.8f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth((float) Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());
        GlStateManager.color(1f, 1f, 1f, 1f);

        renderGlobal.renderBlockOverlays();

        GlStateManager.doPolygonOffset(0f, 0f);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();

        /*
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        */
    }
}
