package fi.dy.masa.litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
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
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.config.value.HorizontalAlignment;
import fi.dy.masa.malilib.config.value.HudAlignment;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.render.TextRenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.Color4f;

public class OverlayRenderer
{
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();

    private final Minecraft mc;
    private final Map<SchematicPlacement, ImmutableMap<String, SelectionBox>> placements = new HashMap<>();
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
    private int blockInfoInvOffY;

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

        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getVisibleSchematicPlacements())
        {
            if (placement.isEnabled())
            {
                this.placements.put(placement, placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED));
            }
        }
    }

    public void renderBoxes(float partialTicks)
    {
        Entity renderViewEntity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection currentSelection = sm.getCurrentSelection();
        boolean renderAreas = currentSelection != null && Configs.Visuals.AREA_SELECTION_RENDERING.getBooleanValue();
        boolean renderPlacements = this.placements.isEmpty() == false && Configs.Visuals.PLACEMENT_BOX_RENDERING.getBooleanValue();
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
                GlStateManager.doPolygonOffset(-1.2f, -0.2f);
                GlStateManager.depthMask(false);

                Box currentBox = currentSelection.getSelectedSubRegionBox();

                for (SelectionBox box : currentSelection.getAllSubRegionBoxes())
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
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
            }

            if (renderPlacements)
            {
                SchematicPlacementManager spm = DataManager.getSchematicPlacementManager();
                SchematicPlacement currentPlacement = spm.getSelectedSchematicPlacement();

                for (Map.Entry<SchematicPlacement, ImmutableMap<String, SelectionBox>> entry : this.placements.entrySet())
                {
                    SchematicPlacement schematicPlacement = entry.getKey();
                    ImmutableMap<String, SelectionBox> boxMap = entry.getValue();
                    boolean origin = schematicPlacement.getSelectedSubRegionPlacement() == null;

                    for (Map.Entry<String, SelectionBox> entryBox : boxMap.entrySet())
                    {
                        String boxName = entryBox.getKey();
                        boolean boxSelected = schematicPlacement == currentPlacement && (origin || boxName.equals(schematicPlacement.getSelectedSubRegionName()));
                        BoxType type = boxSelected ? BoxType.PLACEMENT_SELECTED : BoxType.PLACEMENT_UNSELECTED;
                        this.renderSelectionBox(entryBox.getValue(), type, expand, 1f, 1f, renderViewEntity, partialTicks, schematicPlacement);
                    }

                    Color4f color = schematicPlacement == currentPlacement && origin ? this.colorSelectedCorner : schematicPlacement.getBoundingBoxColor();
                    RenderUtils.renderBlockOutline(schematicPlacement.getOrigin(), expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);

                    if (Configs.Visuals.RENDER_PLACEMENT_ENCLOSING_BOX.getBooleanValue())
                    {
                        if (schematicPlacement.shouldRenderEnclosingBox())
                        {
                            Box box = schematicPlacement.getEclosingBox();
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
            GlStateManager.depthMask(true);
        }
    }

    public void renderSelectionBox(SelectionBox box, BoxType boxType, float expand,
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
                Color4f color = placement.getBoundingBoxColor();
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
            color1 = placement.getBoundingBoxColor();
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDE_ALPHA.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else
        {
            color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;
            sideColor = Color4f.fromColor(Configs.Colors.AREA_SELECTION_BOX_SIDE.getIntegerValue());
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
        List<SchematicVerifier> activeVerifiers = SchematicVerifier.getActiveVerifiers();

        if (activeVerifiers.isEmpty() == false)
        {
            for (SchematicVerifier verifier : activeVerifiers)
            {
                List<MismatchRenderPos> list = verifier.getSelectedMismatchPositionsForRender();

                if (list.isEmpty() == false)
                {
                    List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                    Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                    RayTraceResult trace = RayTraceUtils.traceToPositions(posList, entity, 128);
                    BlockPos posLook = trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK ? trace.getBlockPos() : null;
                    this.renderSchematicMismatches(list, posLook, partialTicks);
                }
            }
        }
    }

    private void renderSchematicMismatches(List<MismatchRenderPos> posList, @Nullable BlockPos lookPos, float partialTicks)
    {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();

        GlStateManager.glLineWidth(2f);

        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
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
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.pos, color, 0.002, buffer, entity, partialTicks);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, entry.pos, false, color, buffer, entity, partialTicks);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.pos, lookedEntry.pos, false, lookedEntry.type.getColor(), buffer, entity, partialTicks);
            }

            tessellator.draw();
            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            GlStateManager.glLineWidth(6f);
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPos, lookedEntry.type.getColor(), 0.002, buffer, entity, partialTicks);
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
                RenderUtils.renderAreaSidesBatched(entry.pos, entry.pos, color, 0.002, entity, partialTicks, buffer);
            }

            tessellator.draw();

            GlStateManager.disableBlend();
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
    }

    public void renderHoverInfo(Minecraft mc)
    {
        if (mc.world != null && mc.player != null)
        {
            boolean infoOverlayKeyActive = Hotkeys.RENDER_BLOCK_INFO_OVERLAY.getKeyBind().isKeyBindHeld();
            boolean verifierOverlayRendered = false;

            if (infoOverlayKeyActive &&
                Configs.InfoOverlays.VERIFIER_OVERLAY_RENDERING.getBooleanValue() &&
                Configs.InfoOverlays.BLOCK_INFO_OVERLAY_RENDERING.getBooleanValue())
            {
                verifierOverlayRendered = this.renderVerifierOverlay(mc);
            }

            boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_RENDERING.getBooleanValue();
            boolean renderInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_RENDERING.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            if (renderBlockInfoLines || renderInfoOverlay)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                traceWrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 10, true);
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
        HudAlignment alignment = Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getValue();
        boolean useBackground = true;
        boolean useShadow = false;

        TextRenderUtils.renderText(x, y, 0, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines);
    }

    private boolean renderVerifierOverlay(Minecraft mc)
    {
        List<SchematicVerifier> activeVerifiers = SchematicVerifier.getActiveVerifiers();

        if (activeVerifiers.isEmpty() == false)
        {
            for (SchematicVerifier verifier : activeVerifiers)
            {
                List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                RayTraceResult trace = RayTraceUtils.traceToPositions(posList, entity, 32);

                if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    BlockPos pos = trace.getBlockPos();
                    BlockMismatch mismatch = verifier.getMismatchForPosition(pos);

                    /* TODO FIXME malilib refactor
                    if (mismatch != null)
                    {
                        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
                        BlockMismatchInfo info = new BlockMismatchInfo(mismatch.stateExpected, mismatch.stateFound);
                        this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, mc);
                        info.render(this.blockInfoX, this.blockInfoY, 0, mc);

                        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
                        World worldClient = WorldUtils.getBestWorld(mc);
                        BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();
                        RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, worldSchematic, worldClient, pos, mc);
                        return true;
                    }
                    */
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
        stateClient = stateClient.getActualState(mc.world, pos);

        IBlockState stateSchematic = worldSchematic.getBlockState(pos);
        stateSchematic = stateSchematic.getActualState(worldSchematic, pos);

        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();

        ItemUtils.setItemForBlock(worldSchematic, pos, stateSchematic);
        ItemUtils.setItemForBlock(mc.world, pos, stateClient);

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air)
        {
            /* TODO FIXME malilib refactor
            BlockMismatchInfo info = new BlockMismatchInfo(stateSchematic, stateClient);
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, mc);
            info.render(this.blockInfoX, this.blockInfoY, 0, mc);

            RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, worldSchematic, worldClient, pos, mc);
            */
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
        {
            BlockInfo info = new BlockInfo(stateClient, "litematica.title.hud.block_info_overlay.state_client");
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, mc);
            info.render(this.blockInfoX, this.blockInfoY, 0, mc);

            RenderUtils.renderInventoryOverlay(align, HorizontalAlignment.CENTER, this.blockInfoInvOffY, worldClient, pos);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            TileEntity te = worldClient.getTileEntity(pos);

            if (te instanceof IInventory)
            {
                BlockInfo info = new BlockInfo(stateClient, "litematica.title.hud.block_info_overlay.state_client");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, mc);
                info.render(this.blockInfoX, this.blockInfoY, 0, mc);

                RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, worldSchematic, worldClient, pos, mc);
            }
            else
            {
                BlockInfo info = new BlockInfo(stateSchematic, "litematica.title.hud.block_info_overlay.state_schematic");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY, mc);
                info.render(this.blockInfoX, this.blockInfoY, 0, mc);

                RenderUtils.renderInventoryOverlay(align, HorizontalAlignment.CENTER, this.blockInfoInvOffY, worldSchematic, pos);
            }
        }
    }

    protected void getOverlayPosition(int width, int height, int offY, Minecraft mc)
    {
        BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();

        if (align == BlockInfoAlignment.CENTER)
        {
            this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
            this.blockInfoY = GuiUtils.getScaledWindowHeight() / 2 + offY;
            this.blockInfoInvOffY = 4;
        }
        else if (align == BlockInfoAlignment.TOP_CENTER)
        {
            this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
            this.blockInfoY = offY;
            this.blockInfoInvOffY = height + offY + 4;
        }
    }

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper, Minecraft mc)
    {
        this.blockInfoLines.clear();

        BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();
        IBlockState stateClient = mc.world.getBlockState(pos);
        stateClient = stateClient.getActualState(mc.world, pos);

        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        IBlockState stateSchematic = worldSchematic.getBlockState(pos);
        stateSchematic = stateSchematic.getActualState(worldSchematic, pos);
        IBlockState air = Blocks.AIR.getDefaultState();

        if (stateSchematic != stateClient && stateSchematic != air && stateClient != air)
        {
            this.blockInfoLines.add(StringUtils.translate("litematica.title.hud.block_info_lines.schematic"));
            this.addBlockInfoLines(stateSchematic);

            this.blockInfoLines.add("");
            this.blockInfoLines.add(StringUtils.translate("litematica.title.hud.block_info_lines.client"));
            this.addBlockInfoLines(stateClient);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            this.blockInfoLines.add(StringUtils.translate("litematica.title.hud.block_info_lines.schematic"));
            this.addBlockInfoLines(stateSchematic);
        }
    }

    private void addBlockInfoLines(IBlockState state)
    {
        this.blockInfoLines.add(String.valueOf(Block.REGISTRY.getNameForObject(state.getBlock())));
        this.blockInfoLines.addAll(BlockUtils.getFormattedBlockStateProperties(state));
    }

    public void renderSchematicRebuildTargetingOverlay(float partialTicks)
    {
        RayTraceWrapper traceWrapper = null;
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        Color4f color = null;
        boolean direction = false;

        if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(this.mc.world, entity, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY.getColor();
            direction = true;
        }

        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
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

    public void renderHoveredSchematicBlock(Minecraft mc, float partialTicks)
    {
        RayTraceResult trace = mc.objectMouseOver;
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK && worldSchematic != null)
        {
            BlockPos pos = trace.getBlockPos();
            IBlockState stateClient = mc.world.getBlockState(pos).getActualState(mc.world, pos);
            IBlockState stateSchematic = worldSchematic.getBlockState(pos);

            if (stateClient != stateSchematic && stateClient.getMaterial() != Material.AIR)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                double dx = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
                double dy = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
                double dz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

                GlStateManager.pushMatrix();
                GlStateManager.translate(-dx, -dy, -dz);
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-0.8f, -0.4f);
                fi.dy.masa.malilib.render.RenderUtils.setupBlend();
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();

                LitematicaRenderer.enableAlphaShader(Configs.Visuals.TRANSLUCENT_SCHEMATIC_ALPHA.getFloatValue());

                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                LitematicaRenderer.getInstance().getWorldRenderer().renderBlock(stateSchematic, pos, worldSchematic, buffer);
                tessellator.draw();

                LitematicaRenderer.disableAlphaShader();

                GlStateManager.depthMask(true);
                GlStateManager.enableDepth();
                GlStateManager.doPolygonOffset(0f, 0f);
                GlStateManager.disablePolygonOffset();
                GlStateManager.popMatrix();
            }
        }
    }

    public void renderPreviewFrame()
    {
        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        int x = width >= height ? (width - height) / 2 : 0;
        int y = height >= width ? (height - width) / 2 : 0;
        int longerSide = Math.min(width, height);

        ShapeRenderUtils.renderOutline(x, y, 0, longerSide, longerSide, 2, 0xFFFFFFFF);
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED;
    }
}
