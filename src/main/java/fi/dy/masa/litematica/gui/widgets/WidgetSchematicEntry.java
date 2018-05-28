package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.data.SchematicHolder.SchematicEntry;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.base.ButtonEntry;
import fi.dy.masa.litematica.gui.button.ButtonBase;
import fi.dy.masa.litematica.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicEntry extends WidgetBase
{
    private final WidgetLoadedSchematics parent;
    private final SchematicEntry schematicEntry;
    private final Minecraft mc;
    private final List<ButtonEntry<?>> buttons = new ArrayList<>();

    public WidgetSchematicEntry(int x, int y, int width, int height, float zLevel,
            SchematicEntry schematicEntry, WidgetLoadedSchematics parent, Minecraft mc)
    {
        super(x, y, width, height, zLevel);

        this.parent = parent;
        this.schematicEntry = schematicEntry;
        this.mc = mc;

        int posX = x + width - 10;
        int len;
        ButtonListener listener;
        String text;

        text = I18n.format("litematica.gui.button.unload");
        len = mc.fontRenderer.getStringWidth(text) + 10;
        posX -= (len + 4);
        listener = new ButtonListener(ButtonListener.Type.UNLOAD, this);
        this.addButton(new ButtonGeneric(0, posX, y, len, 20, text), listener);

        text = I18n.format("litematica.gui.button.save_to_file");
        len = mc.fontRenderer.getStringWidth(text) + 10;
        posX -= (len + 4);
        listener = new ButtonListener(ButtonListener.Type.SAVE_TO_FILE, this);
        this.addButton(new ButtonGeneric(0, posX, y, len, 20, text), listener);

        text = I18n.format("litematica.gui.button.create_placement");
        len = mc.fontRenderer.getStringWidth(text) + 10;
        posX -= (len + 4);
        listener = new ButtonListener(ButtonListener.Type.CREATE_PLACEMENT, this);
        this.addButton(new ButtonGeneric(0, posX, y, len, 20, text), listener);
    }

    protected <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonEntry<>(button, listener));
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        for (ButtonEntry<?> entry : this.buttons)
        {
            if (entry.mousePressed(this.mc, mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                return true;
            }
        }

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        String name = this.schematicEntry.name;
        this.mc.fontRenderer.drawString(name, this.x + 4, this.y + 3, 0xFFFFFFFF);

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final WidgetSchematicEntry widget;

        public ButtonListener(Type type, WidgetSchematicEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.CREATE_PLACEMENT)
            {
                Minecraft mc = Minecraft.getMinecraft();
                int dimension = mc.world.provider.getDimensionType().getId();
                BlockPos pos = new BlockPos(mc.player.getPositionVector());
                SchematicEntry entry = this.widget.schematicEntry;
                SchematicPlacement placement = new SchematicPlacement(entry.schematic, pos, entry.name);
                placement.setEnabled(GuiScreen.isShiftKeyDown() == false);
                placement.setBoxesBBColorNext();
                DataManager.getInstance(dimension).getSchematicPlacementManager().addSchematicPlacement(placement, this.widget.parent.getMessageConsumer());
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {

            }
            else if (this.type == Type.UNLOAD)
            {
                SchematicHolder.getInstance().removeSchematic(this.widget.schematicEntry.schematicId);
                this.widget.parent.refreshEntries();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            CREATE_PLACEMENT,
            SAVE_TO_FILE,
            UNLOAD;
        }
    }
}
