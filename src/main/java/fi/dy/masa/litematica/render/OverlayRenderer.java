package fi.dy.masa.litematica.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.Vec4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class OverlayRenderer
{
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();

    // https://stackoverflow.com/questions/470690/how-to-automatically-generate-n-distinct-colors
    public static final int[] KELLY_COLORS = {
            0xFFB300,    // Vivid Yellow
            0x803E75,    // Strong Purple
            0xFF6800,    // Vivid Orange
            0xA6BDD7,    // Very Light Blue
            0xC10020,    // Vivid Red
            0xCEA262,    // Grayish Yellow
            0x817066,    // Medium Gray
            // The following don't work well for people with defective color vision
            0x007D34,    // Vivid Green
            0xF6768E,    // Strong Purplish Pink
            0x00538A,    // Strong Blue
            0xFF7A5C,    // Strong Yellowish Pink
            0x53377A,    // Strong Violet
            0xFF8E00,    // Vivid Orange Yellow
            0xB32851,    // Strong Purplish Red
            0xF4C800,    // Vivid Greenish Yellow
            0x7F180D,    // Strong Reddish Brown
            0x93AA00,    // Vivid Yellowish Green
            0x593315,    // Deep Yellowish Brown
            0xF13A13,    // Vivid Reddish Orange
            0x232C16     // Dark Olive Green
        };

    private final Minecraft mc;
    private final Map<SchematicPlacement, ImmutableMap<String, Box>> placements = new HashMap<>();
    private Vec4f colorPos1 = new Vec4f(1f, 0.0625f, 0.0625f);
    private Vec4f colorPos2 = new Vec4f(0.0625f, 0.0625f, 1f);
    private Vec4f colorOverlapping = new Vec4f(1f, 0.0625f, 1f);
    private Vec4f colorX = new Vec4f(   1f, 0.25f, 0.25f);
    private Vec4f colorY = new Vec4f(0.25f,    1f, 0.25f);
    private Vec4f colorZ = new Vec4f(0.25f, 0.25f,    1f);
    private Vec4f colorArea = new Vec4f(1f, 1f, 1f);
    private Vec4f colorBoxPlacementSelected = new Vec4f(0x16 / 255f, 1f, 1f);
    private Vec4f colorSelectedCorner = new Vec4f(0f, 1f, 1f);
    private Vec4f colorAreaOrigin = new Vec4f(1f, 0x90 / 255f, 0x10 / 255f);

    private OverlayRenderer()
    {
        this.mc = Minecraft.getMinecraft();
    }

    public static OverlayRenderer getInstance()
    {
        return INSTANCE;
    }

    public void updatePlacementCache()
    {
        this.placements.clear();
        List<SchematicPlacement> list = DataManager.getInstance(this.mc.world).getSchematicPlacementManager().getAllSchematicsPlacements();

        for (SchematicPlacement placement : list)
        {
            if (placement.isEnabled())
            {
                this.placements.put(placement, placement.getSubRegionBoxes());
            }
        }
    }

    public void renderSelectionAreas()
    {
        Entity renderViewEntity = this.mc.getRenderViewEntity();
        float partialTicks = this.mc.getRenderPartialTicks();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = 1.5f;

        DataManager dataManager = DataManager.getInstance(this.mc.world);
        SelectionManager sm = dataManager.getSelectionManager();
        AreaSelection currentSelection = sm.getCurrentSelection();
        final boolean hasWork = currentSelection != null || this.placements.isEmpty() == false;

        if (hasWork)
        {
            GlStateManager.depthMask(true);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.pushMatrix();
        }

        if (currentSelection != null)
        {
            Box currentBox = currentSelection.getSelectedSubRegionBox();

            for (Box box : currentSelection.getAllSubRegionBoxes())
            {
                BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks, null);
            }

            Vec4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
            RenderUtils.renderBlockOutline(currentSelection.getOrigin(), expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);
        }

        if (this.placements.isEmpty() == false)
        {
            SchematicPlacementManager spm = dataManager.getSchematicPlacementManager();
            SchematicPlacement currentPlacement = spm.getSelectedSchematicPlacement();

            for (Map.Entry<SchematicPlacement, ImmutableMap<String, Box>> entry : this.placements.entrySet())
            {
                SchematicPlacement schematicPlacement = entry.getKey();
                ImmutableMap<String, Box> boxMap = entry.getValue();
                boolean origin = schematicPlacement.getSelectedSubRegionPlacement() == null;

                for (Map.Entry<String, Box> entryBox : boxMap.entrySet())
                {
                    String boxName = entryBox.getKey();
                    boolean boxSelected = schematicPlacement == currentPlacement && (origin || boxName.equals(schematicPlacement.getSelectedSubRegionName()));
                    BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                    this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, renderViewEntity, partialTicks, schematicPlacement);
                }

                Vec4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
                RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, 2f, color, renderViewEntity, partialTicks);
            }
        }

        if (hasWork)
        {
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
        }
    }

    public void renderSelectionBox(Box box, BoxType boxType, float expand,
            float lineWidthBlockBox, float lineWidthArea, Entity renderViewEntity, float partialTicks, @Nullable SchematicPlacement placement)
    {
        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        if (pos1 == null && pos2 == null)
        {
            return;
        }

        Vec4f color1;
        Vec4f color2;
        Vec4f colorX;
        Vec4f colorY;
        Vec4f colorZ;

        switch (boxType)
        {
            case AREA_SELECTED:
                colorX = this.colorX;
                colorY = this.colorY;
                colorZ = this.colorZ;
                break;
            case AREA_UNSELECTED:
                colorX = this.colorArea;
                colorY = this.colorArea;
                colorZ = this.colorArea;
                break;
            case PLACEMENT_SELECTED:
                colorX = this.colorBoxPlacementSelected;
                colorY = this.colorBoxPlacementSelected;
                colorZ = this.colorBoxPlacementSelected;
                break;
            case PLACEMENT_UNSELECTED:
                Vec4f color = placement.getBoxesBBColor();
                colorX = color;
                colorY = color;
                colorZ = color;
                break;
            default:
                return;
        }

        Vec4f sideColor;

        if (boxType == BoxType.PLACEMENT_SELECTED)
        {
            color1 = this.colorBoxPlacementSelected;
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Vec4f(color1.r, color1.g, color1.b, alpha);
        }
        else if (boxType == BoxType.PLACEMENT_UNSELECTED)
        {
            color1 = placement.getBoxesBBColor();
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Vec4f(color1.r, color1.g, color1.b, alpha);
        }
        else
        {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
            sideColor = Vec4f.fromColor(Configs.Visuals.SELECTION_BOX_SIDE_COLOR.getIntegerValue());
        }

        if (pos1 != null && pos2 != null)
        {
            if (pos1.equals(pos2) == false)
            {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);

                RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);

                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_SELECTION_BOX_SIDES.getBooleanValue())
                    ||
                     ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                       Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES.getBooleanValue()))
                {
                    RenderUtils.renderAreaSides(pos1, pos2, sideColor, renderViewEntity, partialTicks);
                }
            }
            else
            {
                RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, renderViewEntity, partialTicks);
            }
        }
        else
        {
            if (pos1 != null)
            {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
            }

            if (pos2 != null)
            {
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);
            }
        }
    }

    public void renderSchematicMismatches(float partialTicks)
    {
        List<BlockPos> list = DataManager.getSelectedMismatchPositions();
        MismatchType type = DataManager.getSelectedMismatchType();

        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();

        for (BlockPos pos : list)
        {
            Vec4f color = type.getColor();
            Vec4f colorSides = new Vec4f(color.r, color.g, color.b, (float) Configs.Visuals.ERROR_HILIGHT_ALPHA.getDoubleValue());
            RenderUtils.renderBlockOutline(pos, 0.002f, 2f, color, this.mc.player, partialTicks);
            RenderUtils.renderAreaSides(pos, pos, colorSides, this.mc.player, partialTicks);
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED;
    }
}
