package litematica.render.infohud;

import java.util.ArrayList;
import java.util.List;

import malilib.config.value.LayerMode;
import malilib.util.StringUtils;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.data.DataManager;

public class StatusInfoRenderer implements IInfoHudRenderer
{
    private static final StatusInfoRenderer INSTANCE = new StatusInfoRenderer();

    private boolean overrideEnabled;
    private long lastOverrideTime;
    private long overrideDelay;

    public static void init()
    {
        ToolHud.getInstance().addInfoHudRenderer(INSTANCE, true);
    }

    public static StatusInfoRenderer getInstance()
    {
        return INSTANCE;
    }

    public void startOverrideDelay()
    {
        if (this.shouldOverrideShowStatusHud())
        {
            this.lastOverrideTime = System.nanoTime();
            this.overrideEnabled = true;
            this.overrideDelay = 10000;
        }
    }

    public boolean shouldRenderStatusInfoHud()
    {
        return this.overrideEnabled || Configs.InfoOverlays.STATUS_INFO_HUD_RENDERING.getBooleanValue();
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return phase == RenderPhase.POST && this.shouldRenderStatusInfoHud();
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        List<String> lines = new ArrayList<>();

        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue())
        {
            lines.add(StringUtils.translate("litematica.hud.status_info.easy_place_mode_enabled"));
        }
        else if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            lines.add(StringUtils.translate("litematica.hud.status_info.placement_restriction_enabled"));
        }

        LayerRange range = DataManager.getRenderLayerRange();
        String strMode = range.getLayerMode().getDisplayName();

        if (range.getLayerMode() == LayerMode.ALL)
        {
            lines.add(StringUtils.translate("litematica.hud.status_info.render_layer_mode_all", strMode));
        }
        else
        {
            String axisName = range.getAxis().getName().toLowerCase();
            String val = range.getCurrentLayerString();
            lines.add(StringUtils.translate("litematica.hud.status_info.render_layer_mode", strMode, axisName, val));
        }

        String strOn = StringUtils.translate("litematica.hud.value.on.colored");
        String strOff = StringUtils.translate("litematica.hud.value.off.colored");
        String strAll = Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() ? strOn : strOff;
        String strSch = Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue() ? strOn : strOff;
        String strBlk = Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue() ? strOn : strOff;
        String strOvl = Configs.Visuals.SCHEMATIC_OVERLAY.getBooleanValue() ? strOn : strOff;
        String strSel = Configs.Visuals.AREA_SELECTION_RENDERING.getBooleanValue() ? strOn : strOff;

        lines.add(StringUtils.translate("litematica.hud.status_info.renderer_status", strAll, strSch, strBlk, strOvl, strSel));

        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            lines.add(StringUtils.translate("litematica.hud.status_info.schematic_vcs_mode"));
        }

        if (this.overrideEnabled && System.nanoTime() - this.lastOverrideTime > this.overrideDelay * 1000000L)
        {
            this.overrideEnabled = false;
        }

        return lines;
    }

    private boolean shouldOverrideShowStatusHud()
    {
        if (Configs.InfoOverlays.STATUS_INFO_HUD_AUTO.getBooleanValue() == false)
        {
            return false;
        }

        return  DataManager.getRenderLayerRange().getLayerMode() != LayerMode.ALL ||
                Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() == false ||
                Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue() == false ||
                Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue() == false ||
                Configs.Visuals.SCHEMATIC_OVERLAY.getBooleanValue() == false ||
                Configs.Visuals.AREA_SELECTION_RENDERING.getBooleanValue() == false;
    }
}
