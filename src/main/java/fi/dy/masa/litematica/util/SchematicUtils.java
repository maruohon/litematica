package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SchematicUtils
{
    public static List<MaterialListEntry> createMaterialListFor(SchematicPlacement placement)
    {
        Minecraft mc = Minecraft.getInstance();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        World worldClient = mc.world;

        if (mc.player == null || worldSchematic == null || worldClient == null)
        {
            return Collections.emptyList();
        }

        List<MaterialListEntry> list = new ArrayList<>();
        Object2IntOpenHashMap<IBlockState> countsTotal = new Object2IntOpenHashMap<>();
        Object2IntOpenHashMap<IBlockState> countsMissing = new Object2IntOpenHashMap<>();

        /*if (placement.getMaterialListType() == BlockInfoListType.ALL)
        {
            LitematicaSchematic schematic = placement.getSchematic();

            for (Map.Entry<String, SubRegionPlacement> entry : placement.getEnabledRelativeSubRegionPlacements().entrySet())
            {
                LitematicaBlockStateContainer container = schematic.getSubRegionContainer(entry.getKey());

                if (container != null)
                {
                    Vec3i size = container.getSize();
                    final int sizeX = size.getX();
                    final int sizeY = size.getY();
                    final int sizeZ = size.getZ();

                    for (int y = 0; y < sizeY; ++y)
                    {
                        for (int z = 0; z < sizeZ; ++z)
                        {
                            for (int x = 0; x < sizeX; ++x)
                            {
                                countsTotal.addTo(container.get(x, y, z), 1);
                            }
                        }
                    }
                }
            }
        }
        else if (placement.getMaterialListType() == BlockInfoListType.RENDER_LAYERS)*/
        {
            LayerRange range = DataManager.getRenderLayerRange();

            if (placement.getMaterialListType() == BlockInfoListType.ALL)
            {
                range = new LayerRange();
                range.setLayerMode(LayerMode.ALL);
            }

            EnumFacing.Axis axis = range.getAxis();
            ImmutableMap<String, Box> subRegionBoxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

            for (Map.Entry<String, Box> entry : subRegionBoxes.entrySet())
            {
                Box box = entry.getValue();
                BlockPos pos1 = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
                BlockPos pos2 = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());
                final int startX = axis == EnumFacing.Axis.X ? Math.max(pos1.getX(), range.getLayerMin()) : pos1.getX();
                final int startY = axis == EnumFacing.Axis.Y ? Math.max(pos1.getY(), range.getLayerMin()) : pos1.getY();
                final int startZ = axis == EnumFacing.Axis.Z ? Math.max(pos1.getZ(), range.getLayerMin()) : pos1.getZ();
                final int endX = axis == EnumFacing.Axis.X ? Math.min(pos2.getX(), range.getLayerMax()) : pos2.getX();
                final int endY = axis == EnumFacing.Axis.Y ? Math.min(pos2.getY(), range.getLayerMax()) : pos2.getY();
                final int endZ = axis == EnumFacing.Axis.Z ? Math.min(pos2.getZ(), range.getLayerMax()) : pos2.getZ();

                for (int y = startY; y <= endY; ++y)
                {
                    for (int z = startZ; z <= endZ; ++z)
                    {
                        for (int x = startX; x <= endX; ++x)
                        {
                            posMutable.setPos(x, y, z);
                            IBlockState stateSchematic = worldSchematic.getBlockState(posMutable);
                            IBlockState stateClient = worldClient.getBlockState(posMutable);
                            countsTotal.addTo(stateSchematic, 1);

                            if (stateClient != stateSchematic)
                            {
                                countsMissing.addTo(stateSchematic, 1);
                            }
                        }
                    }
                }
            }
        }

        if (countsTotal.isEmpty() == false)
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2IntOpenHashMap<ItemType> itemTypesTotal = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMissing = new Object2IntOpenHashMap<>();

            // Convert from counts per IBlockState to counts per different stacks
            for (IBlockState state : countsTotal.keySet())
            {
                ItemStack stack = cache.getItemForState(state);

                if (stack.isEmpty() == false)
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesTotal.addTo(type, countsTotal.getInt(state) * stack.getCount());
                }
            }

            // Convert from counts per IBlockState to counts per different stacks
            for (IBlockState state : countsMissing.keySet())
            {
                ItemStack stack = cache.getItemForState(state);

                if (stack.isEmpty() == false)
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesMissing.addTo(type, countsMissing.getInt(state) * stack.getCount());
                }
            }

            Object2IntOpenHashMap<ItemType> playerInvItems = InventoryUtils.getInventoryItemCounts(mc.player.inventory);

            for (ItemType type : itemTypesTotal.keySet())
            {
                int countAvailable = playerInvItems.getInt(type);
                list.add(new MaterialListEntry(type.getStack(), itemTypesTotal.getInt(type), itemTypesMissing.getInt(type), countAvailable));
            }
        }

        return list;
    }
}
