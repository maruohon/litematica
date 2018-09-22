package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;

public class SchematicUtils
{
    public static List<MaterialListEntry> createMaterialListFor(SchematicPlacement placement)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.player == null)
        {
            return Collections.emptyList();
        }

        List<MaterialListEntry> list = new ArrayList<>();
        LitematicaSchematic schematic = placement.getSchematic();
        Object2IntOpenHashMap<IBlockState> counts = new Object2IntOpenHashMap<>();

        for (Map.Entry<String, Placement> entry : placement.getEnabledRelativeSubRegionPlacements().entrySet())
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
                            counts.addTo(container.get(x, y, z), 1);
                        }
                    }
                }
            }
        }

        if (counts.isEmpty() == false)
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2IntOpenHashMap<ItemType> itemTypes = new Object2IntOpenHashMap<>();

            // Convert from counts per IBlockState to counts per different stacks
            for (IBlockState state : counts.keySet())
            {
                ItemStack stack = cache.getItemForState(state);

                if (stack.isEmpty() == false)
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypes.addTo(type, counts.getInt(state) * stack.getCount());
                }
            }

            Object2IntOpenHashMap<ItemType> playerInvItems = InventoryUtils.getInventoryItemCounts(mc.player.inventory);

            for (ItemType type : itemTypes.keySet())
            {
                int countAvailable = playerInvItems.getInt(type);
                list.add(new MaterialListEntry(type.getStack(), itemTypes.getInt(type), countAvailable));
            }
        }

        return list;
    }
}
