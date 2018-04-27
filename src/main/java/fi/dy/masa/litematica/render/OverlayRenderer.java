package fi.dy.masa.litematica.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.Selection;
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
    private final Minecraft mc;
    private final Map<SchematicPlacement, Selection> placementSelections = new HashMap<>();
    private SchematicPlacement currentPlacement;
    private Vec3f colorPos1 = new Vec3f(1f, 0.0625f, 0.0625f);
    private Vec3f colorPos2 = new Vec3f(0.0625f, 0.0625f, 1f);
    private Vec3f colorX = new Vec3f(   1f, 0.25f, 0.25f);
    private Vec3f colorY = new Vec3f(0.25f,    1f, 0.25f);
    private Vec3f colorZ = new Vec3f(0.25f, 0.25f,    1f);
    private Vec3f colorArea = new Vec3f(1f, 1f, 1f);
    private Vec3f colorBoxSchematicSelected = new Vec3f(0x16 / 255f, 1f, 1f);
    private Vec3f colorBoxSchematicUnselected = new Vec3f(0xCB / 255f, 0x2B / 255f, 1f);
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
        this.placementSelections.clear();
        SchematicPlacementManager manager = DataManager.getInstance(this.mc.world).getSchematicPlacementManager();
        List<SchematicPlacement> list = manager.getAllSchematicsPlacements();
        this.currentPlacement = manager.getSelectedSchematicPlacement();

        for (SchematicPlacement placement : list)
        {
            if (placement.isEnabled())
            {
                Selection selection = Selection.fromBoxes(placement.getPos(), placement.getSchematic().getAreas(), placement.getName(), true);
                this.placementSelections.put(placement, selection);
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

        SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
        Selection area = sm.getCurrentSelection();
        final boolean hasWork = area != null || this.placementSelections.isEmpty() == false;

        if (hasWork)
        {
            GlStateManager.depthMask(true);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.pushMatrix();
        }

        if (area != null)
        {
            Box currentBox = area.getSelectedSelectionBox();

            for (Box box : area.getAllSelectionsBoxes())
            {
                BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks);
            }

            if (area.getOrigin() != null)
            {
                RenderUtils.renderBlockOutline(area.getOrigin(), expand, lineWidthBlockBox, this.colorAreaOrigin, renderViewEntity, partialTicks);
            }
        }

        if (this.placementSelections.isEmpty() == false)
        {
            for (Map.Entry<SchematicPlacement, Selection> entry : this.placementSelections.entrySet())
            {
                SchematicPlacement placement = entry.getKey();
                Selection areaTmp = entry.getValue();

                for (Box box : areaTmp.getAllSelectionsBoxes())
                {
                    BoxType type = placement == this.currentPlacement ? BoxType.SCHEMATIC_SELECTED : BoxType.SCHEMATIC_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks);
                }

                if (area.getOrigin() != null)
                {
                    RenderUtils.renderBlockOutline(area.getOrigin(), expand, lineWidthBlockBox, this.colorAreaOrigin, renderViewEntity, partialTicks);
                }
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
            float lineWidthBlockBox, float lineWidthArea, Entity renderViewEntity, float partialTicks)
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
                colorX = this.colorArea; colorY = this.colorArea; colorZ = this.colorArea;
                break;
            case SCHEMATIC_SELECTED:
                colorX = this.colorBoxSchematicSelected; colorY = this.colorBoxSchematicSelected; colorZ = this.colorBoxSchematicSelected;
                break;
            case SCHEMATIC_UNSELECTED:
                colorX = this.colorBoxSchematicUnselected; colorY = this.colorBoxSchematicUnselected; colorZ = this.colorBoxSchematicUnselected;
                break;
            default: return;
        }

        if (pos1 != null && pos2 != null && pos1.equals(pos2) == false)
        {
            RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);
        }

        if (boxType == BoxType.SCHEMATIC_SELECTED)
        {
            color1 = this.colorBoxSchematicSelected;
            color2 = color1;
        }
        else if (boxType == BoxType.SCHEMATIC_UNSELECTED)
        {
            color1 = this.colorBoxSchematicUnselected;
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
        SCHEMATIC_SELECTED,
        SCHEMATIC_UNSELECTED;
    }
}
