package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
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

    private final MinecraftClient mc;
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
        this.mc = MinecraftClient.getInstance();
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

    public void renderBoxes(MatrixStack matrices, float partialTicks)
    {
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
            GlStateManager.disableLighting();
            GlStateManager.disableTexture();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
            GlStateManager.pushMatrix();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            if (renderAreas)
            {
                GlStateManager.enablePolygonOffset();
                GlStateManager.polygonOffset(-1.2f, -0.2f);
                GlStateManager.depthMask(false);

                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (Box box : currentSelection.getAllSubRegionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, null, matrices);
                }

                BlockPos origin = currentSelection.getExplicitOrigin();

                if (origin != null)
                {
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = Color4f.fromColor(this.colorAreaOrigin, 0.4f);
                        RenderUtils.renderAreaSides(origin, origin, colorTmp, matrices, this.mc);
                    }

                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color, this.mc);
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
                        this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, schematicPlacement, matrices);
                    }

                    Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoxesBBColor();
                    RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color, this.mc);

                    if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX.getBooleanValue())
                    {
                        Box box = schematicPlacement.getEclosingBox();

                        if (schematicPlacement.shouldRenderEnclosingBox() && box != null)
                        {
                            RenderUtils.renderAreaOutline(box.getPos1(), box.getPos2(), 1f, color, color, color, this.mc);

                            if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX_SIDES.getBooleanValue())
                            {
                                float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
                                color = new Color4f(color.r, color.g, color.b, alpha);
                                RenderUtils.renderAreaSides(box.getPos1(), box.getPos2(), color, matrices, this.mc);
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
                    RenderUtils.renderBlockOutline(project.getOrigin(), expand, 4f, this.colorOverlapping, this.mc);
                }
            }

            GlStateManager.popMatrix();
            GlStateManager.enableTexture();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
        }
    }

    public void renderSelectionBox(Box box, BoxType boxType, float expand,
            float lineWidthBlockBox, float lineWidthArea, @Nullable SchematicPlacement placement, MatrixStack matrices)
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
                RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, this.mc);

                if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                      Configs.Visuals.RENDER_AREA_SELECTION_BOX_SIDES.getBooleanValue())
                    ||
                     ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                       Configs.Visuals.RENDER_PLACEMENT_BOX_SIDES.getBooleanValue()))
                {
                    RenderUtils.renderAreaSides(pos1, pos2, sideColor, matrices, this.mc);
                }

                if (box.getSelectedCorner() == Corner.CORNER_1)
                {
                    Color4f color = Color4f.fromColor(this.colorPos1, 0.4f);
                    RenderUtils.renderAreaSides(pos1, pos1, color, matrices, this.mc);
                }
                else if (box.getSelectedCorner() == Corner.CORNER_2)
                {
                    Color4f color = Color4f.fromColor(this.colorPos2, 0.4f);
                    RenderUtils.renderAreaSides(pos2, pos2, color, matrices, this.mc);
                }

                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, this.mc);
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, this.mc);
            }
            else
            {
                RenderUtils.renderBlockOutlineOverlapping(pos1, expand, lineWidthBlockBox, color1, color2, this.colorOverlapping, matrices, this.mc);
            }
        }
        else
        {
            if (pos1 != null)
            {
                RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, this.mc);
            }

            if (pos2 != null)
            {
                RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, this.mc);
            }
        }
    }

    public void renderSchematicVerifierMismatches(MatrixStack matrices, float partialTicks)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();

            List<MismatchRenderPos> list = verifier.getSelectedMismatchPositionsForRender();

            if (list.isEmpty() == false)
            {
                List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                HitResult trace = RayTraceUtils.traceToPositions(posList, this.mc.player, 128);
                BlockPos posLook = trace != null && trace.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) trace).getBlockPos() : null;
                this.renderSchematicMismatches(list, posLook, matrices, partialTicks);
            }
        }
    }

    private void renderSchematicMismatches(List<MismatchRenderPos> posList, @Nullable BlockPos lookPos, MatrixStack matrices, float partialTicks)
    {
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture();
        GlStateManager.pushMatrix();

        GlStateManager.lineWidth(2f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        MismatchRenderPos lookedEntry = null;
        MismatchRenderPos prevEntry = null;
        boolean connections = Configs.Visuals.RENDER_ERROR_MARKER_CONNECTIONS.getBooleanValue();

        for (MismatchRenderPos entry : posList)
        {
            Color4f color = entry.type.getColor();

            if (entry.pos.equals(lookPos) == false)
            {
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.pos, color, 0.002, buffer, this.mc);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, this.mc);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, this.mc);
            }

            tessellator.draw();
            buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

            GlStateManager.lineWidth(6f);
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPos, lookedEntry.type.getColor(), 0.002, buffer, this.mc);
        }

        tessellator.draw();

        if (Configs.Visuals.RENDER_ERROR_MARKER_SIDES.getBooleanValue())
        {
            GlStateManager.enableBlend();
            GlStateManager.disableCull();

            buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HILIGHT_ALPHA.getDoubleValue();

            for (MismatchRenderPos entry : posList)
            {
                Color4f color = entry.type.getColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, buffer, this.mc);
            }

            tessellator.draw();

            GlStateManager.disableBlend();
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepthTest();
    }

    public void renderHoverInfo(MinecraftClient mc)
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

            if (traceWrapper != null &&
                (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK ||
                 traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK))
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

    private void renderBlockInfoLines(RayTraceWrapper traceWrapper, MinecraftClient mc)
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

    private boolean renderVerifierOverlay(MinecraftClient mc)
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            HitResult trace = RayTraceUtils.traceToPositions(posList, mc.player, 128);

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                BlockMismatch mismatch = verifier.getMismatchForPosition(((BlockHitResult) trace).getBlockPos());

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

    private void renderBlockInfoOverlay(RayTraceWrapper traceWrapper, MinecraftClient mc)
    {
        BlockState air = Blocks.AIR.getDefaultState();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        World worldClient = WorldUtils.getBestWorld(mc);
        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();

        BlockState stateClient = mc.world.getBlockState(pos);
        BlockState stateSchematic = worldSchematic.getBlockState(pos);

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
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            int invHeight = RenderUtils.renderInventoryOverlay(align, LeftRight.CENTER, offY, worldClient, pos, mc);

            BlockInfo info = new BlockInfo(stateClient, "litematica.gui.label.block_info.state_client");
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, invHeight, mc);
            info.render(this.blockInfoX, this.blockInfoY, mc);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockEntity te = worldClient.getBlockEntity(pos);

            if (te instanceof Inventory)
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

    protected void getOverlayPosition(int width, int height, int offY, int invHeight, MinecraftClient mc)
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

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper, MinecraftClient mc)
    {
        this.blockInfoLines.clear();

        BlockPos pos = traceWrapper.getBlockHitResult().getBlockPos();
        BlockState stateClient = mc.world.getBlockState(pos);

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        BlockState stateSchematic = worldSchematic.getBlockState(pos);
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

    private <T extends Comparable<T>> void addBlockInfoLines(BlockState state)
    {
        this.blockInfoLines.add(String.valueOf(Registry.BLOCK.getId(state.getBlock())));

        for (String line : BlockUtils.getFormattedBlockStateProperties(state))
        {
            this.blockInfoLines.add(line);
        }
    }

    public void renderSchematicRebuildTargetingOverlay(MatrixStack matrixStack, float partialTicks)
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
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            BlockPos pos = trace.getBlockPos();

            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.disableTexture();
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.8f, -1.8f);

            if (direction)
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlay(
                        entity, pos, trace.getSide(), trace.getPos(), color, matrixStack, this.mc);
            }
            else
            {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockTargetingOverlaySimple(
                        entity, pos, trace.getSide(), color, matrixStack, this.mc);
            }

            RenderSystem.disablePolygonOffset();
            GlStateManager.enableTexture();
            //GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
        }
    }

    public void renderPreviewFrame(MinecraftClient mc)
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
