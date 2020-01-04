package fi.dy.masa.litematica.schematic.util;

import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInput;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.LayerRange;

public class SchematicUtils
{
    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            if (DataManager.getSchematicProjectsManager().hasProjectOpen())
            {
                String title = "litematica.gui.title.schematic_projects.save_new_version";
                SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
                GuiTextInput gui = new GuiTextInput(512, title, project.getCurrentVersionName(), GuiUtils.getCurrentScreen(), new SchematicVersionCreator());
                GuiBase.openGui(gui);
            }
            else if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), GuiUtils.getCurrentScreen(), new InMemorySchematicCreator(area));
                GuiBase.openGui(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }

            return true;
        }

        return false;
    }

    @Nullable
    public static BlockPos getSchematicContainerPositionFromWorldPosition(BlockPos worldPos, LitematicaSchematic schematic, String regionName,
            SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement, LitematicaBlockStateContainer container)
    {
        BlockPos boxMinRel = getReverseTransformedWorldPosition(worldPos, schematic, schematicPlacement, regionPlacement, schematic.getSubRegionSize(regionName));

        if (boxMinRel == null)
        {
            return null;
        }

        final int startX = boxMinRel.getX();
        final int startY = boxMinRel.getY();
        final int startZ = boxMinRel.getZ();
        Vec3i size = container.getSize();

        /*
        if (startX < 0 || startY < 0 || startZ < 0 || startX >= size.getX() || startY >= size.getY() || startZ >= size.getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, startX: %d, startY %s, startZ: %d - size x: %d y: %s z: %d =============\n",
                    regionName, startX, startY, startZ, size.getX(), size.getY(), size.getZ());
            return null;
        }

        return boxMinRel;
        */

        return new BlockPos(MathHelper.clamp(startX, 0, size.getX() - 1),
                            MathHelper.clamp(startY, 0, size.getY() - 1),
                            MathHelper.clamp(startZ, 0, size.getZ() - 1));
    }

    @Nullable
    private static BlockPos getReverseTransformedWorldPosition(BlockPos worldPos, LitematicaSchematic schematic,
            SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement, Vec3i regionSize)
    {
        BlockPos origin = schematicPlacement.getOrigin();
        BlockPos regionPos = regionPlacement.getPos();

        // These are the untransformed relative positions
        BlockPos posEndRel = (new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize))).add(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos relPos = new BlockPos(worldPos.getX() - origin.getX() - regionPosTransformed.getX(),
                                       worldPos.getY() - origin.getY() - regionPosTransformed.getY(),
                                       worldPos.getZ() - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, regionPlacement.getMirror(), regionPlacement.getRotation());

        relPos = PositionUtils.getReverseTransformedBlockPos(relPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        relPos = relPos.subtract(posMinRel.subtract(regionPos));

        return relPos;
    }

    @Nullable
    public static Pair<Vec3i, Vec3i> getLayerRangeClampedSubRegion(LayerRange range,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement, Vec3i regionSize)
    {
        int minX = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.X), EnumFacing.Axis.X);
        int minY = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.Y), EnumFacing.Axis.Y);
        int minZ = range.getClampedValue(LayerRange.getWorldMinValueForAxis(EnumFacing.Axis.Z), EnumFacing.Axis.Z);
        int maxX = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.X), EnumFacing.Axis.X);
        int maxY = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.Y), EnumFacing.Axis.Y);
        int maxZ = range.getClampedValue(LayerRange.getWorldMaxValueForAxis(EnumFacing.Axis.Z), EnumFacing.Axis.Z);

        BlockPos posMinRange = new BlockPos(minX, minY, minZ);
        BlockPos posMaxRange = new BlockPos(maxX, maxY, maxZ);

        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        BlockPos pos1 = getReverseTransformedWorldPosition(posMinRange, schematic, schematicPlacement, placement, regionSize);
        BlockPos pos2 = getReverseTransformedWorldPosition(posMaxRange, schematic, schematicPlacement, placement, regionSize);

        if (pos1 == null || pos2 == null)
        {
            return null;
        }

        BlockPos posMinReversed = PositionUtils.getMinCorner(pos1, pos2);
        BlockPos posMaxReversed = PositionUtils.getMaxCorner(pos1, pos2);

        final int startX = Math.max(posMinReversed.getX(), 0);
        final int startY = Math.max(posMinReversed.getY(), 0);
        final int startZ = Math.max(posMinReversed.getZ(), 0);
        final int endX = Math.min(posMaxReversed.getX(), Math.abs(regionSize.getX()) - 1);
        final int endY = Math.min(posMaxReversed.getY(), Math.abs(regionSize.getY()) - 1);
        final int endZ = Math.min(posMaxReversed.getZ(), Math.abs(regionSize.getZ()) - 1);

        return Pair.of(new Vec3i(startX, startY, startZ), new Vec3i(endX, endY, endZ));
    }

    public static IBlockState getUntransformedBlockState(IBlockState state, SchematicPlacement schematicPlacement, String subRegionName)
    {
        SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(subRegionName);

        if (placement != null)
        {
            final Rotation rotationCombined = PositionUtils.getReverseRotation(schematicPlacement.getRotation().add(placement.getRotation()));
            final Mirror mirrorMain = schematicPlacement.getMirror();
            Mirror mirrorSub = placement.getMirror();

            if (mirrorSub != Mirror.NONE &&
                (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
                 schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
            {
                mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
            }

            if (rotationCombined != Rotation.NONE)
            {
                state = state.withRotation(rotationCombined);
            }

            if (mirrorSub != Mirror.NONE)
            {
                state = state.withMirror(mirrorSub);
            }

            if (mirrorMain != Mirror.NONE)
            {
                state = state.withMirror(mirrorMain);
            }
        }

        return state;
    }

    public static class SchematicVersionCreator implements IStringConsumerFeedback
    {
        @Override
        public boolean setString(String string)
        {
            return DataManager.getSchematicProjectsManager().commitNewVersion(string);
        }
    }
}
