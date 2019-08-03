package fi.dy.masa.litematica.schematic.conversion;

import java.util.HashMap;
import net.minecraft.nbt.NBTTagCompound;

public class StateTagFixers_1_12_to_1_13_2
{
    private static final HashMap<String, IStateTagFixer> FIXERS = new HashMap<>();

    public static final IStateTagFixer FIXER_LEVER = (tagIn) -> {
        NBTTagCompound newTag = tagIn.copy();
        NBTTagCompound tagProps = newTag.getCompound("Properties");
        String facing = tagProps.getString("facing");
        String face = facing;

        switch (facing)
        {
            case "down_x":
                facing = "west";
                face = "ceiling";
                break;
            case "down_z":
                facing = "north";
                face = "ceiling";
                break;
            case "up_x":
                facing = "west";
                face = "floor";
                break;
            case "up_z":
                facing = "north";
                face = "floor";
                break;
            default:
        }

        tagProps.putString("facing", facing);
        tagProps.putString("face", face);

        return newTag;
    };

    public static final IStateTagFixer FIXER_REDSTONE_TORCHES = (tagIn) -> {
        NBTTagCompound newTag = tagIn.copy();
        NBTTagCompound tagProps = newTag.getCompound("Properties");
        boolean isLit = newTag.getString("Name").equals("minecraft:redstone_torch");

        tagProps.putString("lit", isLit ? "true" : "false");

        if (tagProps.getString("facing").equals("up"))
        {
            newTag.putString("Name", "minecraft:redstone_torch");
            tagProps.remove("facing");
        }
        else
        {
            newTag.putString("Name", "minecraft:redstone_wall_torch");
        }

        return newTag;
    };

    public static final IStateTagFixer FIXER_TORCH = (tagIn) -> {
        NBTTagCompound newTag = tagIn.copy();

        if (newTag.getCompound("Properties").getString("facing").equals("up"))
        {
            newTag.remove("Properties");
        }
        else
        {
            newTag.putString("Name", "minecraft:wall_torch");
        }

        return newTag;
    };

    public static void init()
    {
        FIXERS.clear();

        FIXERS.put("minecraft:lever",                   FIXER_LEVER);
        FIXERS.put("minecraft:redstone_torch",          FIXER_REDSTONE_TORCHES);
        FIXERS.put("minecraft:unlit_redstone_torch",    FIXER_REDSTONE_TORCHES);
        FIXERS.put("minecraft:torch",                   FIXER_TORCH);
    }

    public static NBTTagCompound fixStateTag(NBTTagCompound tagIn)
    {
        String blockName = tagIn.getString("Name");
        IStateTagFixer fixer = FIXERS.get(blockName);

        if (fixer != null)
        {
            return fixer.fixStateTag(tagIn);
        }

        return tagIn;
    }

    public interface IStateTagFixer
    {
        NBTTagCompound fixStateTag(NBTTagCompound tagIn);
    }
}
