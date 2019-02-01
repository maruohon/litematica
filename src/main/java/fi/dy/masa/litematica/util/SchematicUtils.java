package fi.dy.masa.litematica.util;

import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiTextInput;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class SchematicUtils
{
    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), null, new InMemorySchematicCreator(area));
                gui.setParent(mc.currentScreen);
                mc.displayGuiScreen(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(mc.currentScreen);
                mc.displayGuiScreen(gui);
            }

            return true;
        }

        return false;
    }

    public static boolean breakSchematicBlock(Minecraft mc)
    {
        return setTargetedSchematicBlockState(mc, Blocks.AIR.getDefaultState(), false);
    }

    public static boolean placeSchematicBlock(Minecraft mc)
    {
        ItemStack stack = mc.player.getHeldItemMainhand();

        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlock)
        {
            WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
            RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);

            if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
            {
                RayTraceResult trace = traceWrapper.getRayTraceResult();
                EnumFacing side = trace.sideHit;
                Vec3d hitVec = trace.hitVec;
                int meta = stack.getItem().getMetadata(stack.getMetadata());
                BlockPos pos = trace.getBlockPos().offset(side);

                IBlockState state = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(world, pos, side,
                        (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, meta, mc.player);

                // The state can be null in 1.13+
                if (state != null)
                {
                    return setTargetedSchematicBlockState(pos, state);
                }
            }
        }

        return false;
    }

    public static boolean setTargetedSchematicBlockState(Minecraft mc, IBlockState state, boolean offset)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);

        if (world != null && traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();

            if (offset)
            {
                pos = pos.offset(trace.sideHit);
            }

            return setTargetedSchematicBlockState(pos, state);
        }

        return false;
    }

    public static boolean setTargetedSchematicBlockState(BlockPos pos, IBlockState state)
    {
        if (pos != null)
        {
            SubChunkPos cpos = new SubChunkPos(pos);
            List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementTouchingSubChunk(cpos);

            if (list.isEmpty() == false)
            {
                for (PlacementPart part : list)
                {
                    if (part.getBox().isVecInside(pos))
                    {
                        SchematicPlacement placement = part.getPlacement();
                        String regionName = part.getSubRegionName();
                        LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                        BlockPos posSchematic = getSchematicContainerPositionFromWorldPosition(pos, placement.getSchematic(),
                                regionName, placement, placement.getRelativeSubRegionPlacement(regionName), container);

                        if (posSchematic != null)
                        {
                            container.set(posSchematic.getX(), posSchematic.getY(), posSchematic.getZ(), state);
                            DataManager.getSchematicPlacementManager().markChunkForRebuild(new ChunkPos(cpos.getX(), cpos.getZ()));
                            WorldUtils.markSchematicChunkForRenderUpdate(pos);
                            return true;
                        }

                        return false;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    public static BlockPos getSchematicContainerPositionFromWorldPosition(BlockPos worldPos, LitematicaSchematic schematic, String regionName,
            SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement, LitematicaBlockStateContainer container)
    {
        BlockPos origin = schematicPlacement.getOrigin();
        BlockPos regionPos = regionPlacement.getPos();
        BlockPos regionSize = schematic.getAreaSize(regionName);

        // These are the untransformed relative positions
        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos boxMinRel = new BlockPos(worldPos.getX() - origin.getX() - regionPosTransformed.getX(), worldPos.getY() - origin.getY(), worldPos.getZ() - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, regionPlacement.getMirror(), regionPlacement.getRotation());

        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        boxMinRel = boxMinRel.subtract(posMinRel.subtract(regionPos));

        final int startX = boxMinRel.getX();
        final int startZ = boxMinRel.getZ();

        if (startX < 0 || startZ < 0 || startX >= container.getSize().getX() || startZ >= container.getSize().getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, sx: %d, sz: %d, ex: %d, ez: %d - size x: %d z: %d =============\n",
                    regionName, startX, startZ, startX, startZ, container.getSize().getX(), container.getSize().getZ());
            return null;
        }

        return boxMinRel;
    }
}
