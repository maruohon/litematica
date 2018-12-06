package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
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
    private WorldRendererSchematic worldRenderer;
    private int frameCount;

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public WorldRendererSchematic getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = Minecraft.getInstance();
            this.worldRenderer = new WorldRendererSchematic(this.mc);
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

    public void renderSchematicWorld(float partialTicks)
    {
        if (this.mc.skipRenderWorld == false)
        {
            this.mc.profiler.startSection("litematica_schematic_world_render");
            this.mc.profiler.startSection("litematica_level");
            int fpsLimit = this.mc.gameSettings.limitFramerate;
            int fpsMin = Math.min(Minecraft.getDebugFPS(), fpsLimit);
            fpsMin = Math.max(fpsMin, 60);
            long finishTimeNano = Math.max((long)(1000000000 / fpsMin / 8), 0L);

            GlStateManager.pushMatrix();

            this.renderWorld(partialTicks, System.nanoTime() + finishTimeNano);

            GlStateManager.popMatrix();

            this.mc.profiler.endSection();
            this.mc.profiler.endSection();
        }
    }

    private void renderWorld(float partialTicks, long finishTimeNano)
    {
        if (this.mc.getRenderViewEntity() == null)
        {
            this.mc.setRenderViewEntity(this.mc.player);
        }

        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.5F);
        this.mc.profiler.startSection("litematica_center");

        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        this.mc.profiler.endStartSection("litematica_culling");
        ICamera icamera = new Frustum();
        Entity entity = this.mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
        icamera.setPosition(x, y, z);

        boolean translucentSchematic = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && OpenGlHelper.shadersSupported;

        if (translucentSchematic)
        {
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            GL20.glUseProgram(SHADER_ALPHA.getProgram());
            GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"), alpha);
        }

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        this.mc.profiler.endStartSection("litematica_prepare_terrain");
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();

        this.mc.profiler.endStartSection("litematica_terrain_setup");
        worldRenderer.setupTerrain(entity, partialTicks, icamera, this.frameCount++, this.mc.player.isSpectator());

        this.mc.profiler.endStartSection("litematica_update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        this.mc.profiler.endStartSection("litematica_terrain");
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlphaTest();
        GlStateManager.enableBlend();

        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        worldRenderer.renderBlockLayer(BlockRenderLayer.SOLID, partialTicks, entity);

        worldRenderer.renderBlockLayer(BlockRenderLayer.CUTOUT_MIPPED, partialTicks, entity);

        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        worldRenderer.renderBlockLayer(BlockRenderLayer.CUTOUT, partialTicks, entity);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        this.mc.profiler.endStartSection("litematica_entities");
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        worldRenderer.renderEntities(entity, icamera, partialTicks);

        GlStateManager.disableBlend();
        RenderHelper.disableStandardItemLighting();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();

        GlStateManager.enableCull();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        this.mc.profiler.endStartSection("litematica_translucent");
        GlStateManager.depthMask(false);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        worldRenderer.renderBlockLayer(BlockRenderLayer.TRANSLUCENT, partialTicks, entity);

        GlStateManager.popMatrix();

        if (translucentSchematic)
        {
            GL20.glUseProgram(0);
        }

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED.getBooleanValue())
        {
            GlStateManager.pushMatrix();

            this.mc.profiler.endStartSection("litematica_overlay");
            GlStateManager.disableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.enablePolygonOffset();
            GlStateManager.polygonOffset(-0.1f, -0.8f);
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.lineWidth((float) Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            OpenGlHelper.glMultiTexCoord2f(OpenGlHelper.GL_TEXTURE1, 240, 240);

            if (Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() ||
                Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld())
            {
                GlStateManager.disableDepthTest();
            }

            worldRenderer.renderBlockOverlays();

            GlStateManager.enableDepthTest();
            GlStateManager.polygonOffset(0f, 0f);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }

        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();

        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();

        this.mc.profiler.endSection();
    }
}
