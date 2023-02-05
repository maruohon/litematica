package litematica.gui.widget;

import java.nio.file.Path;

import litematica.gui.util.SchematicInfoCacheByPath;

public class SchematicInfoWidgetByPath extends AbstractSchematicInfoWidget<Path>
{
    public SchematicInfoWidgetByPath(int width, int height)
    {
        super(width, height, new SchematicInfoCacheByPath());
    }
}
