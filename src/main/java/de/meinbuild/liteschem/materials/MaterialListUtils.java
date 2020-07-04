package de.meinbuild.liteschem.materials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.meinbuild.liteschem.schematic.LitematicaSchematic;
import de.meinbuild.liteschem.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;

public class MaterialListUtils
{
    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic)
    {
        return createMaterialListFor(schematic, schematic.getAreas().keySet());
    }

    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();

        for (String regionName : subRegions)
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

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
                            BlockState state = container.get(x, y, z);
                            countsTotal.addTo(state, 1);
                        }
                    }
                }
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        return getMaterialList(countsTotal, countsTotal, new Object2IntOpenHashMap<>(), mc.player);
    }

    public static List<MaterialListEntry> getMaterialList(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<BlockState> countsMissing,
            Object2IntOpenHashMap<BlockState> countsMismatch,
            PlayerEntity player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (countsTotal.isEmpty() == false)
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2IntOpenHashMap<ItemType> itemTypesTotal = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMissing = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMismatch = new Object2IntOpenHashMap<>();

            convertStatesToStacks(countsTotal, itemTypesTotal, cache);
            convertStatesToStacks(countsMissing, itemTypesMissing, cache);
            convertStatesToStacks(countsMismatch, itemTypesMismatch, cache);

            Object2IntOpenHashMap<ItemType> playerInvItems = InventoryUtils.getInventoryItemCounts(player.inventory);

            for (ItemType type : itemTypesTotal.keySet())
            {
                list.add(new MaterialListEntry(type.getStack().copy(),
                        itemTypesTotal.getInt(type),
                        itemTypesMissing.getInt(type),
                        itemTypesMismatch.getInt(type),
                        playerInvItems.getInt(type)));
            }
        }

        return list;
    }

    private static void convertStatesToStacks(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            Object2IntOpenHashMap<ItemType> itemTypesOut,
            MaterialCache cache)
    {
        // Convert from counts per IBlockState to counts per different stacks
        for (BlockState state : blockStatesIn.keySet())
        {
            if (cache.requiresMultipleItems(state))
            {
                for (ItemStack stack : cache.getItems(state))
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesOut.addTo(type, blockStatesIn.getInt(state) * stack.getCount());
                }
            }
            else
            {
                ItemStack stack = cache.getItemForState(state);

                if (stack.isEmpty() == false)
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesOut.addTo(type, blockStatesIn.getInt(state) * stack.getCount());
                }
            }
        }
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list, PlayerEntity player)
    {
        Object2IntOpenHashMap<ItemType> playerInvItems = InventoryUtils.getInventoryItemCounts(player.inventory);

        for (MaterialListEntry entry : list)
        {
            ItemType type = new ItemType(entry.getStack(), false, true);
            int countAvailable = playerInvItems.getInt(type);
            entry.setCountAvailable(countAvailable);
        }
    }
}
