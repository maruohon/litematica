package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult.BlockMismatchInfo;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.registry.IRegistry;
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

    private long infoUpdateTime;
    private List<String> blockInfoLines = new ArrayList<>();
    private int blockInfoX;
    private int blockInfoY;

    private OverlayRenderer()
    {
        this.mc = Minecraft.getInstance();
    }

    public static OverlayRenderer getInstance()
    {
        return INSTANCE;
    }

    public void updatePlacementCache()
    {
        this.placements.clear();
        List<SchematicPlacement> list = DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();

        for (SchematicPlacement placement : list)
        {
            if (placement.isEnabled())
            {
                this.placements.put(placement, placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
            }
        }
    }

    public void renderBoxes(float partialTicks)
    {
        Entity renderViewEntity = this.mc.getRenderViewEntity();
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection currentSelection = sm.getCurrentSelection();
        boolean renderAreas = currentSelection != null && Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue();
        boolean renderPlacements = this.placements.isEmpty() == false && Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING.getBooleanValue();
        boolean isProjectMode = DataManager.getSchematicProjectsManager().hasProjectOpen();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = isProjectMode ? 3f : 1.5f;

        if (renderAreas || renderPlacements || isProjectMode)
        {
            GlStateManager.depthMask(true);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
            GlStateManager.pushMatrix();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            if (renderAreas)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-1.2f, -0.2f);
                GlStateManager.depthMask(false);

                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (Box box : currentSelection.getAllSubRegionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks, null);
                }

                BlockPos origin = currentSelection.getExplicitOrigin();

                if (origin != null)
                {
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = Color4f.fromColor(this.colorAreaOrigin, 0.4f);
                        RenderUtils.renderAreaSides(origin, origin, colorTmp, renderViewEntity, partialTicks);
                    }

                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);
                }

                GlStateManager.depthMask(true);
                GlStateManager.polygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            if (renderPlacements)
            {
                SchematicPlacementManager spm = DataManager.getSchematicPlacementManager();
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
                    RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);

                    if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX.getBooleanValue())
                    {
                        Box box = schematicPlacement.getEclosingBox();

                        if (schematicPlacement.shouldRenderEnclosingBox() && box != null)
                        {
                            RenderUtils.renderAreaOutline(box.getPos1(), box.getPos2(), 1f, color, color, color, renderViewEntity, partialTicks);

                            if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX_SIDES.getBooleanValue())
                            {
                                float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
                                color = new Color4f(color.r, color.g, color.b, alpha);
                                RenderUtils.renderAreaSides(box.getPos1(), box.getPos2(), color, renderViewEntity, partialTicks);
                            }
                        }
                    }
                }
            }

            if (isProjectMode)
            {
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

                if (project != null)
                {
                    RenderUtils.renderBlockOutline(project.getOrigin(), expand, 4f, this.colorOverlapping, renderViewEntity, partialTicks);
                }
            }

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
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);

                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue())
                    ||
                     ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                       Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES.getBooleanValue()))
                {
                    RenderUtils.renderAreaSides(pos1, pos2, sideColor, renderViewEntity, partialTicks);
                }

                if (box.getSelectedCorner() == Corner.CORNER_1)
                {
                    Color4f color = Color4f.fromColor(this.colorPos1, 0.4f);
                    RenderUtils.renderAreaSides(pos1, pos1, color, renderViewEntity, partialTicks);
                }
                else if (box.getSelectedCorner() == Corner.CORNER_2)
                {
                    Color4f color = Color4f.fromColor(this.colorPos2, 0.4f);
                    RenderUtils.renderAreaSides(pos2, pos2, color, renderViewEntity, partialTicks);
                }

                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);
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

    public void renderSchematicVerifierMismatches(float partialTicks)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();

            List<MismatchRenderPos> list = verifier.getSelectedMismatchPositionsForRender();

            if (list.isEmpty() == false)
            {
                List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                RayTraceResult trace = RayTraceUtils.traceToPositions(posList, this.mc.player, 128);
                BlockPos posLook = trace != null && trace.type == RayTraceResult.Type.BLOCK ? trace.getBlockPos() : null;
                this.renderSchematicMismatches(list, posLook, partialTicks);
            }
        }
    }

    private void renderSchematicMismatches(List<MismatchRenderPos> posList, @Nullable BlockPos lookPos, float partialTicks)
    {
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();

        GlStateManager.lineWidth(2f);

        EntityPlayer player = this.mc.player;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        MismatchRenderPos lookedEntry = null;
        MismatchRenderPos prevEntry = null;
        boolean connections = Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();

        for (MismatchRenderPos entry : posList)
        {
            Color4f color = entry.type.getColor();

            if (entry.pos.equals(lookPos) == false)
            {
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.pos, color, 0.002, buffer, player, partialTicks);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, player, partialTicks);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, player, partialTicks);
            }

            tessellator.draw();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            GlStateManager.lineWidth(6f);
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPos, lookedEntry.type.getColor(), 0.002, buffer, player, partialTicks);
        }

        tessellator.draw();

        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue())
        {
            GlStateManager.enableBlend();
            GlStateManager.disableCull();

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA.getDoubleValue();

            for (MismatchRenderPos entry : posList)
            {
                Color4f color = entry.type.getColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, this.mc.player, partialTicks, buffer);
            }

            tessellator.draw();

            GlStateManager.disableBlend();
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
    }

    public void renderHoverInfo(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            boolean infoOverlayKeyActive = Hotkeys.RENDER_INFO_OVERLAY.getKeybind().isKeybindHeld();
            boolean verifierOverlayRendered = false;

            if (infoOverlayKeyActive &&
                Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue() &&
                Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED.getBooleanValue())
            {
                verifierOverlayRendered = this.renderVerifierOverlay(mc);
            }

            boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_ENABLED.getBooleanValue();
            boolean renderInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            if (renderBlockInfoLines || renderInfoOverlay)
            {
                traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 10, true);
            }

            if (traceWrapper != null)
            {
                if (renderBlockInfoLines)
                {
                    this.renderBlockInfoLines(traceWrapper, mc);
                }

                if (renderInfoOverlay)
                {
                    this.renderBlockInfoOverlay(traceWrapper, mc);
                }
            }
        }
    }

    private void renderBlockInfoLines(RayTraceWrapper traceWrapper, Minecraft mc)
    {
        long currentTime = System.currentTimeMillis();

        // Only update the text once per game tick
        if (currentTime - this.infoUpdateTime >= 50)
        {
            this.updateBlockInfoLines(traceWrapper, mc);
            this.infoUpdateTime = currentTime;
        }

        int x = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_X.getIntegerValue();
        int y = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_Y.getIntegerValue();
        double fontScale = Configs.InfoOverlays.BLOCK_INFO_LINES_FONT_SCALE.getDoubleValue();
        int textColor = 0xFFFFFFFF;
        int bgColor = 0xA0505050;
        HudAlignment alignment = (HudAlignment) Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getOptionListValue();
        boolean useBackground = true;
        boolean useShadow = false;

        fi.dy.masa.malilib.render.RenderUtils.renderText(x, y, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines);
    }

    private boolean renderVerifierOverlay(Minecraft mc)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            RayTraceResult trace = RayTraceUtils.traceToPositions(posList, mc.player, 128);

            if (trace != null && trace.type == RayTraceResult.Type.BLOCK)
            {
                BlockMismatch mismatch = verifier.getMismatchForPosition(trace.getBlockPos());

                if (mismatch != null)
                {
                    BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                    info.render(GuiUtils.getScaledWindowWidth() / 2 - info.getTotalWidth() / 2, GuiUtils.getScaledWindowHeight() / 2 + 6, mc);
                    return true;
                }
            }
        }

        return false;
    }

    private void renderBlockInfoOverlay(RayTraceWrapper traceWrapper, Minecraft mc)
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        World worldClient = WorldUtils.getBestWorld(mc);
        BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();

        IBlockState stateClient = mc.world.getBlockState(pos);
        IBlockState stateSchematic = worldSchematic.getBlockState(pos);

        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();

        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
        ItemUtils.setItemForBlock(mc.world, pos, stateClient);

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air)
        {
            int invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, worldClient, pos, mc);

            BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
        {
            int invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.CENTER, offY, worldClient, pos, mc);

            BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            TileEntity te = worldClient.getTileEntity(pos);

            if (te instanceof IInventory)
            {
                int invHeight = RenderUtils.renderInventoryOverlays(align, offY, worldSchematic, worldClient, pos, mc);

                BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
                info.render(this.blockInfoX, this.blockInfoY, mc);
            }
            else
            {
                int invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.CENTER, offY, worldSchematic, pos, mc);

                BlockInfo info = new BlockInfo(stateSchematic, "litematica.gui.label.block_info.state_schematic");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
                info.render(this.blockInfoX, this.blockInfoY, mc);
            }
        }
    }

    protected void getOverlayPosition(int width, int height, int offY, int invHeight, Minecraft mc)
    {
        BlockInfoAlignment align = (BlockInfoAlignment) Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getOptionListValue();

        switch (align)
        {
            case CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = GuiUtils.getScaledWindowHeight() / 2 + offY;
                break;
            case TOP_CENTER:
                this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
                this.blockInfoY = invHeight + offY + (invHeight > 0 ? offY : 0);
                break;
        }
    }

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper, Minecraft mc)
    {
        this.blockInfoLines.clear();

        BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();
        IBlockState stateClient = mc.world.getBlockState(pos);

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        IBlockState stateSchematic = worldSchematic.getBlockState(pos);
        String ul = GuiBase.TXT_UNDERLINE;

        if (stateSchematic != stateClient && stateClient.isAir() == false && stateSchematic.isAir() == false)
        {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);

            this.blockInfoLines.add("");
            this.blockInfoLines.add(ul + "Client:");
            this.addBlockInfoLines(stateClient);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            this.blockInfoLines.add(ul + "Schematic:");
            this.addBlockInfoLines(stateSchematic);
        }
    }

    private <T extends Comparable<T>> void addBlockInfoLines(IBlockState state)
    {
        this.blockInfoLines.add(String.valueOf(IRegistry.BLOCK.getKey(state.getBlock())));

        for (String line : BlockUtils.getFormattedBlockStateProperties(state))
        {
            this.blockInfoLines.add(line);
        }
    }

    public void renderSchematicRebuildTargetingOverlay(float partialTicks)
    {
        RayTraceWrapper traceWrapper = null;
        Color4f color = null;
        boolean direction = false;

        if (Hotkeys.SCHEMATIC_REBUILD_BREAK_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, this.mc.player, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_REBUILD_BREAK_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, this.mc.player, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY_COLOR.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_ALL.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, this.mc.player, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
        }
        else if (Hotkeys.SCHEMATIC_REBUILD_REPLACE_DIRECTION.getKeybind().isKeybindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, this.mc.player, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY_COLOR.getColor();
            direction = true;
        }

        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            Entity entity = this.mc.player;
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();

            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.disableTexture2D();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            if (direction)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                        entity, pos, trace.sideHit, trace.hitVec, color, partialTicks);
            }
            else
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlaySimple(
                        entity, pos, trace.sideHit, color, partialTicks);
            }

            GlStateManager.enableTexture2D();
            //GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
        }
    }

    public void renderPreviewFrame(Minecraft mc)
    {
        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        int x = width >= height ? (width - height) / 2 : 0;
        int y = height >= width ? (height - width) / 2 : 0;
        int longerSide = Math.min(width, height);

        fi.dy.masa.malilib.render.RenderUtils.drawOutline(x, y, longerSide, longerSide, 2, 0xFFFFFFFF);
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED;
    }
}
