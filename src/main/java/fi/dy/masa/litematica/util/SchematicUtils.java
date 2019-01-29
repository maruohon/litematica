package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.GuiSchematicSave.InMemorySchematicCreator;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.malilib.gui.GuiTextInput;
import net.minecraft.client.Minecraft;

public class SchematicUtils
{
    public static boolean saveSchematic(boolean inMemoryOnly)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection area = sm.getCurrentSelection();

        if (area != null)
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (inMemoryOnly)
            {
                String title = "litematica.gui.title.create_in_memory_schematic";
                GuiTextInput gui = new GuiTextInput(512, title, area.getName(), null, new InMemorySchematicCreator(area));
                gui.setParent(mc.currentScreen);
                mc.displayGuiScreen(gui);
            }
            else
            {
                GuiSchematicSave gui = new GuiSchematicSave();
                gui.setParent(mc.currentScreen);
                mc.displayGuiScreen(gui);
            }

            return true;
        }

        return false;
    }
}
