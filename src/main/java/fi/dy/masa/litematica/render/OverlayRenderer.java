package fi.dy.masa.litematica.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicVerifier;
import fi.dy.masa.litematica.data.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult.BlockMismatchInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

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
    private Color4f colorPos1 = new Color4f(1f, 0.0625f, 0.0625f);
    private Color4f colorPos2 = new Color4f(0.0625f, 0.0625f, 1f);
    private Color4f colorOverlapping = new Color4f(1f, 0.0625f, 1f);
    private Color4f colorX = new Color4f(   1f, 0.25f, 0.25f);
    private Color4f colorY = new Color4f(0.25f,    1f, 0.25f);
    private Color4f colorZ = new Color4f(0.25f, 0.25f,    1f);
    private Color4f colorArea = new Color4f(1f, 1f, 1f);
    private Color4f colorBoxPlacementSelected = new Color4f(0x16 / 255f, 1f, 1f);
    private Color4f colorSelectedCorner = new Color4f(0f, 1f, 1f);
    private Color4f colorAreaOrigin = new Color4f(1f, 0x90 / 255f, 0x10 / 255f);

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
        List<SchematicPlacement> list = DataManager.getInstance().getSchematicPlacementManager().getAllSchematicsPlacements();

        for (SchematicPlacement placement : list)
        {
            if (placement.isEnabled())
            {
                this.placements.put(placement, placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
            }
        }
    }

    public void renderSelectionAreas(float partialTicks)
    {
        Entity renderViewEntity = this.mc.getRenderViewEntity();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = 1.5f;

        DataManager dataManager = DataManager.getInstance();
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

            Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
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

                Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
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

        Color4f color1;
        Color4f color2;
        Color4f colorX;
        Color4f colorY;
        Color4f colorZ;

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
                Color4f color = placement.getBoxesBBColor();
                colorX = color;
                colorY = color;
                colorZ = color;
                break;
            default:
                return;
        }

        Color4f sideColor;

        if (boxType == BoxType.PLACEMENT_SELECTED)
        {
            color1 = this.colorBoxPlacementSelected;
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else if (boxType == BoxType.PLACEMENT_UNSELECTED)
        {
            color1 = placement.getBoxesBBColor();
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else
        {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
            sideColor = Color4f.fromColor(Configs.Colors.AREA_SELECTION_BOX_SIDE_COLOR.getIntegerValue());
        }

        if (pos1 != null && pos2 != null)
        {
            if (pos1.equals(pos2) == false)
            {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);

                RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);

                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue())
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
        DataManager manager = DataManager.getInstance();

        if (manager != null)
        {
            SchematicPlacement placement = manager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null && placement.hasVerifier())
            {
                SchematicVerifier verifier = placement.getSchematicVerifier();

                if (verifier.getSelectedMismatchTypeForRender() != null)
                {
                    List<BlockPos> posList = verifier.getSelectedMismatchPositionsForRender();
                    RayTraceResult trace = RayTraceUtils.traceToPositions(posList, this.mc.player, 10);
                    BlockPos posLook = trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK ? trace.getBlockPos() : null;
                    this.renderSchematicMismatches(verifier.getSelectedMismatchTypeForRender(), posList, posLook, partialTicks);
                }
            }
        }
    }

    private void renderSchematicMismatches(MismatchType type, List<BlockPos> posList, @Nullable BlockPos lookPos, float partialTicks)
    {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();

        if (posList.isEmpty() == false)
        {
            GlStateManager.glLineWidth(2f);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

            for (BlockPos pos : posList)
            {
                if (lookPos == null || lookPos.equals(pos) == false)
                {
                    RenderUtils.renderBlockOutlineBatched(pos, 0.002, type.getColor(), this.mc.player, buffer, partialTicks);
                }
            }

            if (lookPos != null)
            {
                tessellator.draw();
                buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

                GlStateManager.glLineWidth(6f);
                RenderUtils.renderBlockOutlineBatched(lookPos, 0.002, type.getColor(), this.mc.player, buffer, partialTicks);
            }

            tessellator.draw();
        }

        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue())
        {
            GlStateManager.enableBlend();
            GlStateManager.disableCull();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            for (BlockPos pos : posList)
            {
                Color4f color = type.getColor();
                Color4f colorSides = new Color4f(color.r, color.g, color.b, (float) Configs.Visuals.ERROR_HILIGHT_ALPHA.getDoubleValue());
                RenderUtils.renderAreaSidesBatched(pos, pos, colorSides, 0.002, this.mc.player, partialTicks, buffer);
            }

            tessellator.draw();

            GlStateManager.disableBlend();
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
    }

    public static void renderHoverInfo(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            if (Configs.Visuals.ENABLE_VERIFIER_OVERLAY_RENDERING.getBooleanValue() &&
                Configs.Visuals.RENDER_INFO_OVERLAY.getBooleanValue() &&
                (Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isValid() == false ||
                 Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld()))
            {
                SchematicPlacement placement = DataManager.getInstance().getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (placement != null && placement.hasVerifier())
                {
                    SchematicVerifier verifier = placement.getSchematicVerifier();
                    List<BlockPos> posList = verifier.getSelectedMismatchPositionsForRender();
                    RayTraceResult trace = RayTraceUtils.traceToPositions(posList, mc.player, 10);

                    if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
                    {
                        BlockMismatch mismatch = verifier.getMismatchForPosition(trace.getBlockPos());

                        if (mismatch != null)
                        {
                            BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                            ScaledResolution sr = new ScaledResolution(mc);
                            info.render(sr.getScaledWidth() / 2 - info.getTotalWidth() / 2, sr.getScaledHeight() / 2 + 10, mc);
                            return;
                        }
                    }
                }
            }

            if (Configs.Visuals.ENABLE_INFO_OVERLAY_RENDERING.getBooleanValue() &&
                (Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isValid() == false ||
                 Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld()))
            {
                RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 10, true);

                if (traceWrapper != null)
                {
                    ScaledResolution sr = new ScaledResolution(mc);

                    BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();
                    IBlockState stateClient = mc.world.getBlockState(pos);
                    stateClient = stateClient.getActualState(mc.world, pos);

                    World worldSchematic = SchematicWorldHandler.getSchematicWorld();
                    IBlockState stateSchematic = worldSchematic.getBlockState(pos);
                    stateSchematic = stateSchematic.getActualState(worldSchematic, pos);
                    IBlockState air = Blocks.AIR.getDefaultState();

                    // Not just a missing block
                    if (stateSchematic != stateClient && stateClient != air && stateSchematic != air)
                    {
                        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
                        ItemUtils.setItemForBlock(mc.world, pos, stateClient);
                        BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
                        info.render(sr.getScaledWidth() / 2 - info.getTotalWidth() / 2, sr.getScaledHeight() / 2 + 10, mc);

                        RenderUtils.renderInventoryOverlay(-1, worldSchematic, pos, mc);
                        World world = WorldUtils.getBestWorld(mc);
                        RenderUtils.renderInventoryOverlay(1, world, pos, mc);
                    }
                    else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
                    {
                        ItemUtils.setItemForBlock(mc.world, pos, stateClient);
                        BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
                        info.render(sr.getScaledWidth() / 2 - info.getTotalWidth() / 2, sr.getScaledHeight() / 2 + 10, mc);
                        World world = WorldUtils.getBestWorld(mc);
                        RenderUtils.renderInventoryOverlay(0, world, pos, mc);
                    }
                    else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
                    {
                        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
                        BlockInfo info = new BlockInfo(stateSchematic, "litematica.gui.label.block_info.state_schematic");
                        info.render(sr.getScaledWidth() / 2 - info.getTotalWidth() / 2, sr.getScaledHeight() / 2 + 10, mc);

                        int xOffset = 0;
                        TileEntity te = mc.world.getTileEntity(pos);

                        if (te instanceof IInventory)
                        {
                            RenderUtils.renderInventoryOverlay(90, WorldUtils.getBestWorld(mc), pos, mc);
                            xOffset = -90;
                        }

                        RenderUtils.renderInventoryOverlay(xOffset, worldSchematic, pos, mc);
                    }
                }
            }
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
