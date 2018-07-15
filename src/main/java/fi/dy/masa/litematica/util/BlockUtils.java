package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.text.TextFormatting;

public class BlockUtils
{
    private static final String AQUA = TextFormatting.AQUA.toString();
    private static final String GOLD = TextFormatting.GOLD.toString();
    private static final String GREEN = TextFormatting.GREEN.toString();
    private static final String RED = TextFormatting.RED.toString();

    public static List<String> getFormattedBlockStateProperties(IBlockState state)
    {
        if (state.getProperties().size() > 0)
        {
            List<String> lines = new ArrayList<>();
            UnmodifiableIterator<Map.Entry<IProperty<?>, Comparable<?>>> iter = state.getProperties().entrySet().iterator();

            while (iter.hasNext())
            {
                Map.Entry<IProperty<?>, Comparable<?>> entry = iter.next();
                IProperty<?> key = entry.getKey();
                Comparable<?> val = entry.getValue();

                if (key instanceof PropertyBool)
                {
                    String pre = val.equals(Boolean.TRUE) ? GREEN : RED;
                    lines.add(key.getName() + " = " + pre + val.toString());
                }
                else if (key instanceof PropertyDirection)
                {
                    lines.add(key.getName() + " = " + GOLD + val.toString());
                }
                else if (key instanceof PropertyInteger)
                {
                    lines.add(key.getName() + " = " + AQUA + val.toString());
                }
                else
                {
                    lines.add(key.getName() + " = " + val.toString());
                }
            }

            return lines;
        }

        return Collections.emptyList();
    }
}
