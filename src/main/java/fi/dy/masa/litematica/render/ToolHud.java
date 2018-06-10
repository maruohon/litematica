package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class ToolHud
{
    private static final ToolHud INSTANCE = new ToolHud();

    public static final String GREEN = TextFormatting.GREEN.toString();
    public static final String WHITE = TextFormatting.WHITE.toString();
    public static final String WHITE_ITA = TextFormatting.WHITE.toString() + TextFormatting.ITALIC.toString();
    public static final String RESET = TextFormatting.RESET.toString();

    private final Minecraft mc;
    private final List<String> lineList = new ArrayList<>();

    private ToolHud()
    {
        this.mc = Minecraft.getMinecraft();
    }

    public static ToolHud getInstance()
    {
        return INSTANCE;
    }

    public void renderHud()
    {
        if (this.mc.player != null && EntityUtils.isHoldingItem(this.mc.player, DataManager.getToolItem()))
        {
            final OperationMode mode = DataManager.getOperationMode();
            List<String> lines = this.getHudText(mode);
            renderTextLines(this.mc, 4, 4, 0xFFFFFFFF, 0x80000000, true, true, HudAlignment.BOTTOM_LEFT, lines);
        }
    }

    private List<String> getHudText(OperationMode mode)
    {
        List<String> lines = this.lineList;
        lines.clear();
        String str;
        String strYes = GuiLitematicaBase.TXT_GREEN + I18n.format("litematica.label.yes") + GuiLitematicaBase.TXT_RST;
        String strNo = GuiLitematicaBase.TXT_RED + I18n.format("litematica.label.no") + GuiLitematicaBase.TXT_RST;

        if (mode == OperationMode.AREA_SELECTION)
        {
            SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
            AreaSelection selection = sm.getCurrentSelection();

            if (selection != null)
            {
                str = I18n.format("litematica.hud.area_selection.selected_area");
                String strTmp = sm.getCurrentSelectionName();
                lines.add(String.format("%s: %s%s%s", str, GREEN, strTmp, RESET));

                str = I18n.format("litematica.hud.area_selection.box_count");
                int count = selection.getAllSubRegionBoxes().size();
                String strBoxes = String.format("%s: %s%d%s", str, GREEN, count, RESET);
                BlockPos or = selection.getOrigin();
                str = I18n.format("litematica.hud.area_selection.origin");
                String strOrigin = String.format("%s: %s%d%s, %s%d%s, %s%d%s", str, GREEN, or.getX(), WHITE, GREEN, or.getY(), WHITE, GREEN, or.getZ(), RESET);
                lines.add(strOrigin + " - " + strBoxes);

                String subRegionName = selection.getCurrentSubRegionBoxName();

                if (subRegionName != null)
                {
                    str = I18n.format("litematica.hud.area_selection.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, GREEN, subRegionName, RESET));
                }
            }
        }
        else if (mode == OperationMode.PLACEMENT)
        {
            SchematicPlacement schematicPlacement = DataManager.getInstance(this.mc.world).getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (schematicPlacement != null)
            {
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, WHITE, schematicPlacement.getName(), RESET));

                str = I18n.format("litematica.hud.schematic_placement.sub_region_count");
                int count = schematicPlacement.getSubRegionCount();
                String strCount = String.format("%s: %s%d%s", str, GREEN, count, RESET);

                str = I18n.format("litematica.hud.schematic_placement.sub_regions_modified");
                String strTmp = schematicPlacement.isRegionPlacementModified() ? strYes : strNo;
                lines.add(strCount + String.format(" - %s: %s", str, strTmp));

                str = I18n.format("litematica.hud.area_selection.origin");
                BlockPos or = schematicPlacement.getOrigin();
                lines.add(String.format("%s: %s%d%s, %s%d%s, %s%d%s", str, GREEN, or.getX(), WHITE, GREEN, or.getY(), WHITE, GREEN, or.getZ(), RESET));

                Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

                if (placement != null)
                {
                    String areaName = placement.getName();
                    str = I18n.format("litematica.hud.schematic_placement.selected_sub_region");
                    lines.add(String.format("%s: %s%s%s", str, WHITE, areaName, RESET));

                    str = I18n.format("litematica.hud.schematic_placement.sub_region_modified");
                    strTmp = placement.isRegionPlacementModified(schematicPlacement.getSchematic().getSubRegionPosition(areaName)) ? strYes : strNo;
                    lines.add(String.format("%s: %s", str, strTmp));
                }
            }
            else
            {
                String strTmp = "<" + I18n.format("litematica.label.none_lower") + ">";
                str = I18n.format("litematica.hud.schematic_placement.selected_placement");
                lines.add(String.format("%s: %s%s%s", str, WHITE, strTmp, RESET));
            }
        }

        str = I18n.format("litematica.hud.selected_mode");
        lines.add(String.format("%s [%s%d%s/%s%d%s]: %s%s%s", str, GREEN, mode.ordinal() + 1, WHITE,
                GREEN, OperationMode.values().length, WHITE, GREEN, mode.getName(), RESET));

        return lines;
    }

    public static void renderTextLines(Minecraft mc, int xOff, int yOff, int fontColor, int bgColor,
            boolean useBg, boolean useShadow, HudAlignment align, List<String> lines)
    {
        FontRenderer fontRenderer = mc.fontRenderer;
        ScaledResolution res = new ScaledResolution(mc);
        final int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        final int bgMargin = 2;
        double posX = xOff + bgMargin;
        double posY = yOff + bgMargin;

        if (align == HudAlignment.TOP_RIGHT)
        {
            Collection<PotionEffect> effects = mc.player.getActivePotionEffects();

            if (effects.isEmpty() == false)
            {
                int y1 = 0;
                int y2 = 0;

                for (PotionEffect effect : effects)
                {
                    Potion potion = effect.getPotion();

                    if (effect.doesShowParticles() && potion.hasStatusIcon())
                    {
                        if (potion.isBeneficial())
                        {
                            y1 = 26;
                        }
                        else
                        {
                            y2 = 52;
                            break;
                        }
                    }
                }

                posY += Math.max(y1, y2);
            }
        }

        switch (align)
        {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                posY = res.getScaledHeight() - (lines.size() * lineHeight) - yOff + 2;
                break;
            case CENTER:
                posY = (res.getScaledHeight() / 2.0d) - (lines.size() * lineHeight / 2.0d) + yOff;
                break;
            default:
        }

        for (String line : lines)
        {
            final int width = fontRenderer.getStringWidth(line);

            switch (align)
            {
                case TOP_RIGHT:
                case BOTTOM_RIGHT:
                    posX = res.getScaledWidth() - width - xOff - bgMargin;
                    break;
                case CENTER:
                    posX = (res.getScaledWidth() / 2) - (width / 2) - xOff;
                    break;
                default:
            }

            final int x = (int) posX;
            final int y = (int) posY;
            posY += (double) lineHeight;

            if (useBg)
            {
                Gui.drawRect(x - bgMargin, y - bgMargin, x + width + bgMargin, y + fontRenderer.FONT_HEIGHT, bgColor);
            }

            if (useShadow)
            {
                fontRenderer.drawStringWithShadow(line, x, y, fontColor);
            }
            else
            {
                fontRenderer.drawString(line, x, y, fontColor);
            }
        }
    }
}
