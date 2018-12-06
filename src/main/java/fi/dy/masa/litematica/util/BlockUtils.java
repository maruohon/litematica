package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.text.TextFormatting;

public class BlockUtils
{
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> List<String> getFormattedBlockStateProperties(IBlockState state)
    {
        if (state.getProperties().size() > 0)
        {
            List<String> lines = new ArrayList<>();

            for (Entry <IProperty<?>, Comparable<?>> entry : state.getValues().entrySet())
            {
                IProperty<T> property = (IProperty<T>) entry.getKey();
                T value = (T) entry.getValue();
                String valueName = property.getName(value);

                if (property instanceof DirectionProperty)
                {
                    valueName = TextFormatting.GOLD + valueName;
                }
                else if (property instanceof BooleanProperty)
                {
                    String pre = value.equals(Boolean.TRUE) ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
                    valueName = pre + valueName;
                }
                else if (property instanceof IntegerProperty)
                {
                    valueName = TextFormatting.AQUA + valueName;
                }

                lines.add(property.getName() + " = " + valueName);
            }

            return lines;
        }

        return Collections.emptyList();
    }
}
