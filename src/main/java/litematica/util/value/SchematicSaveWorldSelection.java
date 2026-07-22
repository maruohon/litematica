package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionConfigValue;

public class SchematicSaveWorldSelection extends BaseOptionConfigValue
{
    public static final SchematicSaveWorldSelection VANILLA_ONLY         = new SchematicSaveWorldSelection("vanilla_only",         "litematica.gui.label.schematic_save_world.vanilla_only");
    public static final SchematicSaveWorldSelection SCHEMATIC_ONLY       = new SchematicSaveWorldSelection("schematic_only",       "litematica.gui.label.schematic_save_world.schematic_only");
    public static final SchematicSaveWorldSelection VANILLA_OR_SCHEMATIC = new SchematicSaveWorldSelection("vanilla_or_schematic", "litematica.gui.label.schematic_save_world.vanilla_or_schematic");
    public static final SchematicSaveWorldSelection SCHEMATIC_OR_VANILLA = new SchematicSaveWorldSelection("schematic_or_vanilla", "litematica.gui.label.schematic_save_world.schematic_or_vanilla");

    public static final ImmutableList<SchematicSaveWorldSelection> VALUES = ImmutableList.of(VANILLA_ONLY, SCHEMATIC_ONLY, VANILLA_OR_SCHEMATIC, SCHEMATIC_OR_VANILLA);

    public SchematicSaveWorldSelection(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
