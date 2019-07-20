package fi.dy.masa.litematica.event;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.GuiUtils;

public class RenderHandler implements IRenderer
{
    public static final RenderHandler INSTANCE = new RenderHandler();

    @SubscribeEvent
    public void onRenderDebugScreen(RenderGameOverlayEvent.Text event)
    {
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo)
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

            if (world != null)
            {
                RenderGlobal renderer = LitematicaRenderer.getInstance().getWorldRenderer();

                String pre = GuiBase.TXT_GOLD;
                String rst = GuiBase.TXT_RST;

                List<String> listLeft = event.getLeft();
                listLeft.add("");
                listLeft.add(String.format("%s[Litematica]%s %s", pre, rst, renderer.getDebugInfoRenders()));
                listLeft.add(String.format("%s[Litematica]%s %s E: %s TE: %d", pre, rst, renderer.getDebugInfoEntities(), world.getDebugLoadedEntities(), world.loadedTileEntityList.size()));
            }
        }
    }

    @Override
    public void onRenderWorldLast(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

            if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert &&
                Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() == false)
            {
                LitematicaRenderer.getInstance().renderSchematicWorld(partialTicks);
            }

            OverlayRenderer.getInstance().renderBoxes(partialTicks);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(partialTicks);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(partialTicks);
            }
        }
    }

    @Override
    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            // The Info HUD renderers can decide if they want to be rendered in GUIs
            InfoHud.getInstance().renderHud();

            if (GuiUtils.getCurrentScreen() == null)
            {
                ToolHud.getInstance().renderHud();
                OverlayRenderer.getInstance().renderHoverInfo(mc);

                if (GuiSchematicManager.hasPendingPreviewTask())
                {
                    OverlayRenderer.getInstance().renderPreviewFrame(mc);
                }
            }
        }
    }
}
