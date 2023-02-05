package litematica.gui.widget;

import litematica.gui.util.SchematicInfoCacheBySchematic;
import litematica.schematic.ISchematic;

public class SchematicInfoWidgetBySchematic extends AbstractSchematicInfoWidget<ISchematic>
{
    public SchematicInfoWidgetBySchematic(int width, int height)
    {
        super(width, height, new SchematicInfoCacheBySchematic());
    }
}
