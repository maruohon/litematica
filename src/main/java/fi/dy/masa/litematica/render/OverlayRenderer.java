package fi.dy.masa.litematica.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.Vec3f;
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
    private Vec3f colorPos1 = new Vec3f(1f, 0.0625f, 0.0625f);
    private Vec3f colorPos2 = new Vec3f(0.0625f, 0.0625f, 1f);
    private Vec3f colorX = new Vec3f(   1f, 0.25f, 0.25f);
    private Vec3f colorY = new Vec3f(0.25f,    1f, 0.25f);
    private Vec3f colorZ = new Vec3f(0.25f, 0.25f,    1f);
    private Vec3f colorArea = new Vec3f(1f, 1f, 1f);
    private Vec3f colorBoxPlacementSelected = new Vec3f(0x16 / 255f, 1f, 1f);
    private Vec3f colorSelectedCorner = new Vec3f(0f, 1f, 1f);
    private Vec3f colorAreaOrigin = new Vec3f(1f, 0x90 / 255f, 0x10 / 255f);

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

            Vec3f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
            RenderUtils.renderBlockOutline(currentSelection.getOrigin(), expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);
        }

        if (this.placements.isEmpty() == false)
        {
            SchematicPlacementManager spm = dataManager.getSchematicPlacementManager();
            SchematicPlacement currentPlacement = spm.getSelectedSchematicPlacement();

            for (Map.Entry<SchematicPlacement, ImmutableMap<String, Box>> entry : this.placements.entrySet())
            {
                SchematicPlacement placement = entry.getKey();
                ImmutableMap<String, Box> boxMap = entry.getValue();
                boolean origin = spm.isOriginSelected();

                for (Map.Entry<String, Box> entryBox : boxMap.entrySet())
                {
                    String boxName = entryBox.getKey();
                    boolean boxSelected = placement == currentPlacement && (origin || boxName.equals(placement.getSelectedSubRegionName()));
                    BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                    this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, renderViewEntity, partialTicks, placement);
                }

                Vec3f color = placement == currentPlacement && origin ? this.colorSelectedCorner : placement.getBoxesBBColor();
                RenderUtils.renderBlockOutline(placement.getOrigin(), expand, 2f, color, renderViewEntity, partialTicks);
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

        Vec3f color1;
        Vec3f color2;
        Vec3f colorX;
        Vec3f colorY;
        Vec3f colorZ;

        switch (boxType)
        {
            case AREA_SELECTED:
                colorX = this.colorX; colorY = this.colorY; colorZ = this.colorZ;
                color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
                color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
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
                Vec3f color = placement.getBoxesBBColor();
                colorX = color;
                colorY = color;
                colorZ = color;
                break;
            default: return;
        }

        if (pos1 != null && pos2 != null && pos1.equals(pos2) == false)
        {
            RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);
        }

        if (boxType == BoxType.PLACEMENT_SELECTED)
        {
            color1 = this.colorBoxPlacementSelected;
            color2 = color1;
        }
        else if (boxType == BoxType.PLACEMENT_UNSELECTED)
        {
            color1 = placement.getBoxesBBColor();
            color2 = color1;
        }
        else
        {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
        }

        if (pos1 != null)
        {
            RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
        }

        if (pos2 != null)
        {
            RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);
        }
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED;
    }
}