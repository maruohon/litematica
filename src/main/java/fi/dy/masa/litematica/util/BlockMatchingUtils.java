package fi.dy.masa.litematica.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import fi.dy.masa.litematica.config.Configs;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BlockMatchingUtils {
    private static final Gson gson = new Gson();
    private static String lastBadInputBlockTypeMatchingJson = "";
    private static String lastBadInputBlockStateMatchingJson = "";

    private static List<String> parseBlockTypeMatchingJson(String jsonString) {
        List<String> jsonList;
        if (jsonString.equals(lastBadInputBlockTypeMatchingJson)) {
            return null;
        }
        try {
            jsonList = gson.fromJson(jsonString, new TypeToken<List<String>>() {
            }.getType());
            //jsonList.forEach(System.out::println);
        } catch (JsonParseException e) {
            jsonList = null;
            lastBadInputBlockTypeMatchingJson = jsonString;
        }
        return jsonList;
    }

    private static List<String> parseBlockStateMatchingJson(String jsonString) {
        List<String> jsonList = null;
        if (jsonString.equals(lastBadInputBlockStateMatchingJson)) {
            return null;
        }
        try {
            jsonList = gson.fromJson(jsonString, new TypeToken<List<String>>() {
            }.getType());
            //jsonList.forEach(System.out::println);
        } catch (JsonParseException ignored) {
        }
        if (jsonList == null) {
            lastBadInputBlockStateMatchingJson = jsonString;
        }
        return jsonList;
    }

    public static boolean isTwoBlockTypesMatching(BlockState blockState1, BlockState blockState2) {
        String inputOptionString = Configs.Visuals.EQUIVALENT_BLOCK_TYPE_SUBSTITUTE.getStringValue();
        if (inputOptionString.isEmpty()) {
            return false;
        }
        List<String> blockTypeMatchingRegexList = parseBlockTypeMatchingJson(inputOptionString);
        if (blockTypeMatchingRegexList == null) {
            return false;
        }
        for (String s : blockTypeMatchingRegexList) {
            if (Registry.BLOCK.getId(blockState1.getBlock()).toString().matches(s)) {
                if (Registry.BLOCK.getId(blockState2.getBlock()).toString().matches(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    public static boolean isTwoBlockStatesMatching(BlockState blockState1, BlockState blockState2) {
        String inputOptionString = Configs.Visuals.EQUIVALENT_BLOCK_STATE_SUBSTITUTE.getStringValue();
        if (inputOptionString.isEmpty()) {
            return false;
        }
        List<String> blockStateMatchingRegexList = parseBlockStateMatchingJson(inputOptionString);
        if (blockStateMatchingRegexList == null) {
            return false;
        }
        List<String> blockProps2 = getFormattedBlockStatePropertiesWithBlockTypeBase(blockState2);
        AtomicReference<Boolean> flag = new AtomicReference<>(false);
        getFormattedBlockStatePropertiesWithBlockTypeBase(blockState1).forEach(s -> {
            blockStateMatchingRegexList.forEach(r -> {
                if (s.matches(r)){
                    blockProps2.forEach(s2 -> {
                        if (s2.matches(r)){
                            flag.set(true);
                        }
                    });
                }
            });
        });

        return flag.get();
    }
     */

    private static List<String> getFormattedBlockStatePropertiesWithBlockTypeBase(BlockState state) {
        Collection<Property<?>> properties = state.getProperties();
        String separator = ":";
        if (properties.size() > 0) {
            List<String> lines = new ArrayList<>();
            for (Property<?> prop : properties) {
                Comparable<?> val = state.get(prop);
                lines.add(Registry.BLOCK.getId(state.getBlock()).toString() +"["+ prop.getName() + separator + val.toString() +"]");
                // the format is like "BlockType[blockPropName:blockPropValue]"
            }
            return lines;
        }
        return Collections.emptyList();
    }
}
