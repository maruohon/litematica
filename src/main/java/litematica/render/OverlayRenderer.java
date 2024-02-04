package litematica.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import malilib.config.value.HorizontalAlignment;
import malilib.config.value.HudAlignment;
import malilib.gui.util.GuiUtils;
import malilib.gui.util.ScreenContext;
import malilib.render.BlockTargetingRenderUtils;
import malilib.render.RenderContext;
import malilib.render.ShapeRenderUtils;
import malilib.render.buffer.VanillaWrappingVertexBuilder;
import malilib.render.buffer.VertexBuilder;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.data.EnabledCondition;
import malilib.util.game.BlockUtils;
import malilib.util.game.wrap.WorldWrap;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.game.wrap.RenderWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.HitResult;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec2i;
import litematica.config.Configs;
import litematica.config.Configs.Visuals;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.gui.widget.SchematicVerifierBlockInfoWidget;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.projects.SchematicProject;
import litematica.schematic.verifier.BlockPairTypePosition;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.schematic.verifier.SchematicVerifierManager;
import litematica.schematic.verifier.VerifierResultType;
import litematica.selection.AreaSelection;
import litematica.selection.AreaSelectionManager;
import litematica.selection.BoxCorner;
import litematica.selection.CornerDefinedBox;
import litematica.selection.SelectionBox;
import litematica.util.ItemUtils;
import litematica.util.RayTraceUtils;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.util.value.BlockInfoAlignment;
import litematica.world.SchematicWorldHandler;

public class OverlayRenderer
{
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();

    private final Minecraft mc;
    private final Map<SchematicPlacement, ImmutableMap<String, SelectionBox>> placements = new HashMap<>();
    private final List<String> blockInfoLines = new ArrayList<>();
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
                this.placements.put(placement, placement.getSubRegionBoxes(EnabledCondition.ENABLED));
            }
        }
    }

    public void renderBoxes(RenderContext ctx, float partialTicks)
    {
        Entity renderViewEntity = GameWrap.getCameraEntity();
        AreaSelectionManager sm = DataManager.getAreaSelectionManager();
        AreaSelection currentSelection = sm.getCurrentSelection();
        boolean renderAreas = currentSelection != null && Configs.Visuals.AREA_SELECTION_RENDERING.getBooleanValue();
        boolean renderPlacements = this.placements.isEmpty() == false && Configs.Visuals.PLACEMENT_BOX_RENDERING.getBooleanValue();
        boolean isProjectMode = DataManager.getSchematicProjectsManager().hasProjectOpen();
        float expand = 0.001f;
        float lineWidthBlockBox = 2f;
        float lineWidthArea = isProjectMode ? 3f : 1.5f;

        if (renderAreas || renderPlacements || isProjectMode)
        {
            RenderWrap.depthMask(true);
            RenderWrap.disableLighting();
            RenderWrap.disableTexture2D();
            RenderWrap.alphaFunc(GL11.GL_GREATER, 0.01F);
            RenderWrap.pushMatrix(ctx);
            RenderWrap.setupBlendSeparate();

            if (renderAreas)
            {
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-1.2f, -0.2f);
                RenderWrap.depthMask(false);

                CornerDefinedBox currentBox = currentSelection.getSelectedSelectionBox();

                for (SelectionBox box : currentSelection.getAllSelectionBoxes())
                {
                    BoxType type = box == currentBox ? BoxType.AREA_SELECTED : BoxType.AREA_UNSELECTED;
                    this.renderSelectionBox(box, type, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks, null);
                }

                BlockPos origin = currentSelection.getManualOrigin();

                if (origin != null)
                {
                    if (currentSelection.isOriginSelected())
                    {
                        Color4f colorTmp = this.colorAreaOrigin.withAlpha(0.4F);
                        RenderUtils.renderAreaSides(origin, origin, colorTmp, renderViewEntity, partialTicks);
                    }

                    Color4f color = currentSelection.isOriginSelected() ? this.colorSelectedCorner : this.colorAreaOrigin;
                    RenderUtils.renderBlockOutline(origin, expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);
                }

                RenderWrap.depthMask(true);
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
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
                    RenderUtils.renderBlockOutline(schematicPlacement.getPosition(), expand, lineWidthBlockBox, color, renderViewEntity, partialTicks);

                    if (schematicPlacement.shouldRenderEnclosingBox())
                    {
                        IntBoundingBox box = schematicPlacement.getEnclosingBox();
                        RenderUtils.renderAreaOutline(box, 1f, color, color, color, renderViewEntity, partialTicks);

                        if (Configs.Visuals.PLACEMENT_ENCLOSING_BOX_SIDES.getBooleanValue())
                        {
                            float alpha = (float) Visuals.PLACEMENT_ENCLOSING_BOX_SIDES.getDoubleValue();
                            color = new Color4f(color.r, color.g, color.b, alpha);
                            RenderUtils.renderAreaSides(box, color, renderViewEntity, partialTicks);
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

            RenderWrap.popMatrix(ctx);
            RenderWrap.enableTexture2D();
            RenderWrap.enableCull();
            RenderWrap.depthMask(true);
        }
    }

    public void renderSelectionBox(SelectionBox box, BoxType boxType, float expand,
            float lineWidthBlockBox, float lineWidthArea, Entity renderViewEntity, float partialTicks, @Nullable SchematicPlacement placement)
    {
        BlockPos pos1 = box.getCorner1();
        BlockPos pos2 = box.getCorner2();
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
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDES.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else if (boxType == BoxType.PLACEMENT_UNSELECTED)
        {
            color1 = placement.getBoundingBoxColor();
            color2 = color1;
            float alpha = (float) Configs.Visuals.PLACEMENT_BOX_SIDES.getDoubleValue();
            sideColor = new Color4f(color1.r, color1.g, color1.b, alpha);
        }
        else
        {
            color1 = box.isCornerSelected(BoxCorner.CORNER_1) ? this.colorSelectedCorner : this.colorPos1;
            color2 = box.isCornerSelected(BoxCorner.CORNER_2) ? this.colorSelectedCorner : this.colorPos2;
            sideColor = Color4f.fromColor(Configs.Colors.AREA_SELECTION_BOX_SIDE.getIntegerValue());
        }

        if (pos1.equals(pos2) == false)
        {
            RenderUtils.renderAreaOutlineNoCorners(pos1, pos2, lineWidthArea, colorX, colorY, colorZ, renderViewEntity, partialTicks);

            if (((boxType == BoxType.AREA_SELECTED || boxType == BoxType.AREA_UNSELECTED) &&
                  Configs.Visuals.AREA_SELECTION_BOX_SIDES.getBooleanValue())
                ||
                 ((boxType == BoxType.PLACEMENT_SELECTED || boxType == BoxType.PLACEMENT_UNSELECTED) &&
                   Configs.Visuals.PLACEMENT_BOX_SIDES.getBooleanValue()))
            {
                RenderUtils.renderAreaSides(pos1, pos2, sideColor, renderViewEntity, partialTicks);
            }

            if (box.isCornerSelected(BoxCorner.CORNER_1))
            {
                Color4f color = this.colorPos1.withAlpha(0.4F);
                RenderUtils.renderAreaSides(pos1, pos1, color, renderViewEntity, partialTicks);
            }
            else if (box.isCornerSelected(BoxCorner.CORNER_2))
            {
                Color4f color = this.colorPos2.withAlpha(0.4F);
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

    public void renderSchematicVerifierMismatches(RenderContext ctx, float partialTicks)
    {
        List<SchematicVerifier> activeVerifiers = SchematicVerifierManager.INSTANCE.getActiveVerifiers();

        if (activeVerifiers.isEmpty() == false)
        {
            BlockPos cameraPos = EntityWrap.getCameraEntityBlockPos();

            for (SchematicVerifier verifier : activeVerifiers)
            {
                List<BlockPairTypePosition> list = verifier.getClosestSelectedPositions(cameraPos);

                if (list.isEmpty() == false)
                {
                    List<BlockPairTypePosition> posList = verifier.getClosestSelectedPositions(cameraPos);
                    Entity entity = GameWrap.getCameraEntity();
                    BlockPairTypePosition lookPos = RayTraceUtils.traceToVerifierResultPositions(posList, entity, 128);
                    this.renderSchematicMismatches(list, lookPos, partialTicks, ctx);
                }
            }
        }
    }

    private void renderSchematicMismatches(List<BlockPairTypePosition> posList,
                                           @Nullable BlockPairTypePosition lookPos,
                                           float partialTicks, RenderContext ctx)
    {
        RenderWrap.disableDepthTest();
        RenderWrap.depthMask(false);
        RenderWrap.disableLighting();
        RenderWrap.pushMatrix(ctx);

        RenderWrap.lineWidth(2f);

        Entity entity = GameWrap.getCameraEntity();
        BlockPairTypePosition lookedEntry = null;
        BlockPairTypePosition prevEntry = null;
        long lookPosLong = lookPos != null ? lookPos.posLong : -1;
        boolean connections = Configs.Visuals.VERIFIER_HIGHLIGHT_CONNECTIONS.getBooleanValue();

        VertexBuilder lineBuilder = VanillaWrappingVertexBuilder.coloredLines();

        for (BlockPairTypePosition entry : posList)
        {
            Color4f color = entry.type.getOverlayColor().withAlpha(1f);

            if (entry.posLong != lookPosLong)
            {
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(entry.posLong, color, 0.002, lineBuilder, entity, partialTicks);
            }
            else
            {
                lookedEntry = entry;
            }

            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.posLong, entry.posLong, false, color,
                                                           lineBuilder, entity, partialTicks);
            }

            prevEntry = entry;
        }

        if (lookedEntry != null)
        {
            Color4f color = lookedEntry.type.getOverlayColor().withAlpha(1f);

            if (connections && prevEntry != null)
            {
                RenderUtils.drawConnectingLineBatchedLines(prevEntry.posLong, lookedEntry.posLong, false,
                                                           color, lineBuilder, entity, partialTicks);
            }

            lineBuilder.draw();
            lineBuilder.start();

            RenderWrap.lineWidth(6f);
            RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(lookPosLong, color, 0.002, lineBuilder, entity, partialTicks);
        }

        lineBuilder.draw();

        if (Configs.Visuals.VERIFIER_HIGHLIGHT_SIDES.getBooleanValue())
        {
            RenderWrap.enableBlend();
            RenderWrap.disableCull();

            VertexBuilder quadBuilder = VanillaWrappingVertexBuilder.coloredQuads();
            float alpha = (float) Configs.InfoOverlays.VERIFIER_ERROR_HIGHLIGHT_ALPHA.getDoubleValue();

            for (BlockPairTypePosition entry : posList)
            {
                Color4f color = entry.type.getOverlayColor();
                color = new Color4f(color.r, color.g, color.b, alpha);
                RenderUtils.renderAreaSidesBatched(entry.posLong, entry.posLong, color, 0.002, entity, partialTicks, quadBuilder);
            }

            quadBuilder.draw();

            RenderWrap.disableBlend();
        }

        RenderWrap.popMatrix(ctx);
        RenderWrap.enableTexture2D();
        RenderWrap.enableCull();
        RenderWrap.depthMask(true);
        RenderWrap.enableDepthTest();
    }

    public void renderHoverInfo(RenderContext ctx)
    {
        World world = GameWrap.getClientWorld();

        if (world != null && GameWrap.getClientPlayer() != null)
        {
            boolean infoOverlayKeyActive = Hotkeys.RENDER_BLOCK_INFO_OVERLAY.getKeyBind().isKeyBindHeld();
            boolean verifierOverlayRendered = false;

            if (infoOverlayKeyActive &&
                Configs.InfoOverlays.VERIFIER_OVERLAY_RENDERING.getBooleanValue() &&
                Configs.InfoOverlays.BLOCK_INFO_OVERLAY_RENDERING.getBooleanValue())
            {
                verifierOverlayRendered = this.renderVerifierOverlay();
            }

            boolean renderBlockInfoLines = Configs.InfoOverlays.BLOCK_INFO_LINES_RENDERING.getBooleanValue();
            boolean renderInfoOverlay = verifierOverlayRendered == false && infoOverlayKeyActive && Configs.InfoOverlays.BLOCK_INFO_OVERLAY_RENDERING.getBooleanValue();
            RayTraceWrapper traceWrapper = null;

            if (renderBlockInfoLines || renderInfoOverlay)
            {
                Entity entity = GameWrap.getCameraEntity();
                traceWrapper = RayTraceUtils.getGenericTrace(world, entity, 10, true);
            }

            if (traceWrapper != null)
            {
                if (renderBlockInfoLines)
                {
                    this.renderBlockInfoLines(traceWrapper, ctx);
                }

                if (renderInfoOverlay)
                {
                    this.renderBlockInfoOverlay(traceWrapper, ctx);
                }
            }
        }
    }

    private void renderBlockInfoLines(RayTraceWrapper traceWrapper, RenderContext ctx)
    {
        long currentTime = System.nanoTime();

        // Only update the text once per game tick
        if (currentTime - this.infoUpdateTime >= 50000000L)
        {
            this.updateBlockInfoLines(traceWrapper);
            this.infoUpdateTime = currentTime;
        }

        Vec2i offset = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET.getValue();
        int x = offset.x;
        int y = offset.y;
        double fontScale = Configs.InfoOverlays.BLOCK_INFO_LINES_FONT_SCALE.getDoubleValue();
        int textColor = 0xFFFFFFFF;
        int bgColor = 0xA0505050;
        HudAlignment alignment = Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getValue();
        boolean useBackground = true;
        boolean useShadow = false;

        // TODO FIXME use a StringListRenderer
        //TextRenderUtils.renderText(x, y, 0, fontScale, textColor, bgColor, alignment, useBackground, useShadow, this.blockInfoLines);
    }

    private boolean renderVerifierOverlay()
    {
        List<SchematicVerifier> activeVerifiers = SchematicVerifierManager.INSTANCE.getActiveVerifiers();

        if (activeVerifiers.isEmpty() == false)
        {
            BlockPos cameraPos = EntityWrap.getCameraEntityBlockPos();

            for (SchematicVerifier verifier : activeVerifiers)
            {
                Entity entity = GameWrap.getCameraEntity();
                List<BlockPairTypePosition> posList = verifier.getClosestSelectedPositions(cameraPos);
                BlockPairTypePosition lookPos = RayTraceUtils.traceToVerifierResultPositions(posList, entity, 32);

                if (lookPos != null)
                {
                    int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
                    SchematicVerifierBlockInfoWidget widget = new SchematicVerifierBlockInfoWidget(
                            lookPos.type, lookPos.pair.expectedState, lookPos.pair.foundState);
                    this.getOverlayPosition(widget.getWidth(), widget.getHeight(), offY);
                    widget.renderAt(this.blockInfoX, this.blockInfoY, 0, new ScreenContext(0, 0, 0, false));

                    World worldSchematic = SchematicWorldHandler.getSchematicWorld();
                    World worldClient = WorldWrap.getBestWorld();
                    BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();
                    RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, worldSchematic,
                                                        worldClient, lookPos.getBlockPos());

                    return true;
                }
            }
        }

        return false;
    }

    private void renderBlockInfoOverlay(RayTraceWrapper traceWrapper, RenderContext ctx)
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        World schematicWorld = SchematicWorldHandler.getSchematicWorld();
        World clientWorld = GameWrap.getClientWorld();
        World bestWorld = WorldWrap.getBestWorld();
        BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();

        IBlockState stateClient = clientWorld.getBlockState(pos);
        stateClient = stateClient.getActualState(clientWorld, pos);

        IBlockState stateSchematic = schematicWorld.getBlockState(pos);
        stateSchematic = stateSchematic.getActualState(schematicWorld, pos);

        int offY = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_OFFSET_Y.getIntegerValue();
        BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();

        ItemUtils.setItemForBlock(schematicWorld, pos, stateSchematic);
        ItemUtils.setItemForBlock(clientWorld, pos, stateClient);

        // Not just a missing block
        if (stateSchematic != stateClient && stateClient != air && stateSchematic != air)
        {
            SchematicVerifierBlockInfoWidget widget = new SchematicVerifierBlockInfoWidget(
                    VerifierResultType.from(stateSchematic, stateClient), stateSchematic, stateClient);
            this.getOverlayPosition(widget.getWidth(), widget.getHeight(), offY);
            widget.renderAt(this.blockInfoX, this.blockInfoY, 0, new ScreenContext(0, 0, 0, false));
            RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, schematicWorld, bestWorld, pos);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA)
        {
            BlockInfo info = new BlockInfo(stateClient, "litematica.title.hud.block_info_overlay.state_client");
            this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY);
            info.render(this.blockInfoX, this.blockInfoY, 0, ctx);

            RenderUtils.renderInventoryOverlay(align, HorizontalAlignment.CENTER, this.blockInfoInvOffY, bestWorld, pos);
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            TileEntity te = bestWorld.getTileEntity(pos);

            if (te instanceof IInventory)
            {
                BlockInfo info = new BlockInfo(stateClient, "litematica.title.hud.block_info_overlay.state_client");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY);
                info.render(this.blockInfoX, this.blockInfoY, 0, ctx);

                RenderUtils.renderInventoryOverlays(align, this.blockInfoInvOffY, schematicWorld, bestWorld, pos);
            }
            else
            {
                BlockInfo info = new BlockInfo(stateSchematic, "litematica.title.hud.block_info_overlay.state_schematic");
                this.getOverlayPosition(info.getTotalWidth(), info.getTotalHeight(), offY);
                info.render(this.blockInfoX, this.blockInfoY, 0, ctx);

                RenderUtils.renderInventoryOverlay(align, HorizontalAlignment.CENTER, this.blockInfoInvOffY, schematicWorld, pos);
            }
        }
    }

    protected void getOverlayPosition(int width, int height, int offY)
    {
        BlockInfoAlignment align = Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ALIGNMENT.getValue();

        if (align == BlockInfoAlignment.CENTER)
        {
            this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
            this.blockInfoY = GuiUtils.getScaledWindowHeight() / 2 + offY;
            this.blockInfoInvOffY = 2;
        }
        else if (align == BlockInfoAlignment.TOP_CENTER)
        {
            this.blockInfoX = GuiUtils.getScaledWindowWidth() / 2 - width / 2;
            this.blockInfoY = offY;
            this.blockInfoInvOffY = height + offY;
        }
    }

    private void updateBlockInfoLines(RayTraceWrapper traceWrapper)
    {
        this.blockInfoLines.clear();

        World world = GameWrap.getClientWorld();
        BlockPos pos = traceWrapper.getRayTraceResult().getBlockPos();
        IBlockState stateClient = world.getBlockState(pos);
        stateClient = stateClient.getActualState(world, pos);

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
        String id = RegistryUtils.getBlockIdStr(state.getBlock());

        if (id != null)
        {
            this.blockInfoLines.add(id);
            this.blockInfoLines.addAll(BlockUtils.getFormattedBlockStateProperties(state));
        }
    }

    public void renderSchematicRebuildTargetingOverlay(float partialTicks, RenderContext ctx)
    {
        RayTraceWrapper traceWrapper = null;
        Entity entity = GameWrap.getCameraEntity();
        World world = GameWrap.getClientWorld();
        Color4f color = null;
        boolean direction = false;

        if (Hotkeys.SCHEMATIC_EDIT_BREAK_ALL.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(world, entity, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_BREAK_DIRECTION.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(world, entity, 20, true);
            color = Configs.Colors.REBUILD_BREAK_OVERLAY.getColor();
            direction = true;
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_ALL.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(world, entity, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY.getColor();
        }
        else if (Hotkeys.SCHEMATIC_EDIT_REPLACE_DIRECTION.getKeyBind().isKeyBindHeld())
        {
            traceWrapper = RayTraceUtils.getGenericTrace(world, entity, 20, true);
            color = Configs.Colors.REBUILD_REPLACE_OVERLAY.getColor();
            direction = true;
        }

        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            HitResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.blockPos;

            RenderWrap.depthMask(false);
            RenderWrap.disableLighting();
            RenderWrap.disableCull();
            RenderWrap.disableTexture2D();
            RenderWrap.setupBlendSeparate();

            if (direction)
            {
                BlockTargetingRenderUtils.render5WayBlockTargetingOverlay(entity, pos, trace.side,
                                                                          trace.pos, color, partialTicks, ctx);
            }
            else
            {
                BlockTargetingRenderUtils.renderSimpleSquareBlockTargetingOverlay(entity, pos, trace.side,
                                                                                  color, partialTicks, ctx);
            }

            RenderWrap.enableTexture2D();
            //RenderWrap.enableDepth();
            RenderWrap.disableBlend();
            RenderWrap.enableCull();
            RenderWrap.depthMask(true);
        }
    }

    public void renderHoveredSchematicBlock(RenderContext ctx, float tickDelta)
    {
        Minecraft mc = GameWrap.getClient();
        HitResult hitResult = GameWrap.getHitResult();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (hitResult.type == HitResult.Type.BLOCK && worldSchematic != null)
        {
            BlockPos pos = hitResult.blockPos;
            IBlockState stateClient = mc.world.getBlockState(pos).getActualState(mc.world, pos);
            IBlockState stateSchematic = worldSchematic.getBlockState(pos);

            if (stateClient != stateSchematic && stateClient.getMaterial() != Material.AIR)
            {
                Entity entity = GameWrap.getCameraEntity();
                double dx = EntityWrap.lerpX(entity, tickDelta);
                double dy = EntityWrap.lerpY(entity, tickDelta);
                double dz = EntityWrap.lerpZ(entity, tickDelta);

                RenderWrap.pushMatrix(ctx);
                RenderWrap.translate(-dx, -dy, -dz, ctx);
                RenderWrap.enablePolygonOffset();
                RenderWrap.polygonOffset(-0.8f, -0.4f);
                RenderWrap.setupBlendSeparate();
                RenderWrap.disableDepthTest();
                RenderWrap.depthMask(false);

                LitematicaRenderer.enableAlphaShader(Configs.Visuals.TRANSLUCENT_SCHEMATIC_RENDERING.getFloatValue());

                VertexBuilder builder = VanillaWrappingVertexBuilder.create(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                LitematicaRenderer.getInstance().getWorldRenderer().renderBlock(stateSchematic, pos, worldSchematic, builder);
                builder.draw();

                LitematicaRenderer.disableAlphaShader();

                RenderWrap.depthMask(true);
                RenderWrap.enableDepthTest();
                RenderWrap.polygonOffset(0f, 0f);
                RenderWrap.disablePolygonOffset();
                RenderWrap.popMatrix(ctx);
            }
        }
    }

    public void renderPreviewFrame(RenderContext ctx)
    {
        int width = GuiUtils.getScaledWindowWidth();
        int height = GuiUtils.getScaledWindowHeight();
        int x = width >= height ? (width - height) / 2 : 0;
        int y = height >= width ? (height - width) / 2 : 0;
        int longerSide = Math.min(width, height);

        ShapeRenderUtils.renderOutline(x, y, 0, longerSide, longerSide, 2, 0xFFFFFFFF, ctx);
    }

    private enum BoxType
    {
        AREA_SELECTED,
        AREA_UNSELECTED,
        PLACEMENT_SELECTED,
        PLACEMENT_UNSELECTED;
    }
}
