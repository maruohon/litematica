package fi.dy.masa.litematica.util;

import java.util.Iterator;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import com.google.common.base.Splitter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BlockUtils
{
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str)
    {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try
        {
            Identifier id = new Identifier(blockName);

            if (Registry.BLOCK.containsId(id))
            {
                Block block = Registry.BLOCK.get(id);
                BlockState state = block.getDefaultState();

                if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']')
                {
                    StateManager<Block, BlockState> stateManager = block.getStateManager();
                    String propStr = str.substring(index + 1, str.length() - 1);

                    for (String propAndVal : COMMA_SPLITTER.split(propStr))
                    {
                        Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                        if (valIter.hasNext() == false)
                        {
                            continue;
                        }

                        Property<?> prop = stateManager.getProperty(valIter.next());

                        if (prop == null || valIter.hasNext() == false)
                        {
                            continue;
                        }

                        Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                        if (val != null)
                        {
                            state = getBlockStateWithProperty(state, prop, val);
                        }
                    }
                }

                return Optional.of(state);
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop, Comparable<?> value)
    {
        return state.with(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr)
    {
        return prop.parse(valStr).orElse(null);
    }
}
