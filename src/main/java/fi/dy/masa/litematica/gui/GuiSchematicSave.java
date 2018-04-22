package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.AreaSelectionManager;
import fi.dy.masa.litematica.util.DataManager;
import fi.dy.masa.litematica.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiSchematicSave extends GuiLitematicaBase
{
    private AreaSelectionManager selectionManager;

    public GuiSchematicSave()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null)
        {
            this.selectionManager = DataManager.getInstance(mc.world).getSelectionManager();
        }
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.save_schematic");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int xStart = LEFT;
        int x = xStart;
        int y = TOP + 20;
        int nameWidth = 300;
        int id = 0;

        String label = I18n.format("litematica.gui.button.save_area_to_schematic");
        ButtonGeneric button = new ButtonGeneric(id++, x, y, nameWidth, 20, label);
        ButtonListener listener = this.createActionListener(ButtonListener.Type.SAVE);
        this.addButton(button, listener);
    }

    private ButtonListener createActionListener(ButtonListener.Type type)
    {
        return new ButtonListener(type,  this.selectionManager, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSchematicSave gui;
        private final AreaSelectionManager selectionManager;
        private final Type type;

        public ButtonListener(Type type, AreaSelectionManager selectionManager, GuiSchematicSave gui)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.SAVE)
            {
                String title = I18n.format("litematica.gui.title.save_schematic_filename");
                SchematicSaver saver = new SchematicSaver(this.gui.mc, this.selectionManager);
                this.gui.mc.displayGuiScreen(new GuiTextInput(160, title, "", this.gui, saver));
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            SAVE;
        }
    }

    private static class SchematicSaver implements IStringConsumer
    {
        private final Minecraft mc;
        private final AreaSelectionManager selectionManager;

        public SchematicSaver(Minecraft mc, AreaSelectionManager selectionManager)
        {
            this.mc = mc;
            this.selectionManager = selectionManager;
        }

        @Override
        public void setString(String string)
        {
            AreaSelection area = this.selectionManager.getSelectedAreaSelection();

            if (area != null)
            {
                IStringConsumer feedback = InfoUtils.INFO_MESSAGE_CONSUMER;
                boolean takeEntities = true;
                String author = this.mc.player.getName();
                LitematicaSchematic schematic = LitematicaSchematic.makeSchematic(this.mc.world, area, takeEntities, author, feedback);

                if (schematic != null)
                {
                    // TODO directory browser and override option
                    File dir = new File(Minecraft.getMinecraft().mcDataDir, "schematics");
                    boolean override = false;

                    if (schematic.writeToFile(dir, string, override, feedback))
                    {
                        feedback.setString("litematica.message.schematic_saved");
                    }
                }
            }
        }
    }
}
