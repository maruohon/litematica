package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.data.SchematicHolder.SchematicEntry;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonWrapper;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicEntry extends WidgetBase
{
    private final WidgetLoadedSchematics parent;
    private final SchematicEntry schematicEntry;
    private final Minecraft mc;
    private final List<ButtonWrapper<?>> buttons = new ArrayList<>();
    private final int typeIconX;
    private final int typeIconY;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSchematicEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            SchematicEntry schematicEntry, WidgetLoadedSchematics parent, Minecraft mc)
    {
        super(x, y, width, height, zLevel);

        this.parent = parent;
        this.schematicEntry = schematicEntry;
        this.mc = mc;
        this.isOdd = isOdd;
        y += 1;

        int posX = x + width;
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

        this.buttonsStartX = posX;
        this.typeIconX = this.x + 2;
        this.typeIconY = y + 4;
    }

    protected <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonWrapper<>(button, listener));
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        for (ButtonWrapper<?> entry : this.buttons)
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
        GlStateManager.color(1, 1, 1, 1);

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiLitematicaBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x50FFFFFF);
        }

        String schematicName = this.schematicEntry.getSchematicName();
        this.mc.fontRenderer.drawString(schematicName, this.x + 20, this.y + 7, 0xFFFFFFFF);

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        String fileName = this.schematicEntry.getFileName();
        this.parent.bindTexture(Icons.TEXTURE);

        if (fileName != null)
        {
            Icons.SCHEMATIC_TYPE_FILE.renderAt(this.typeIconX, this.typeIconY, this.zLevel, false, false);

            if (GuiLitematicaBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 12, this.height))
            {
                this.parent.drawHoveringText(fileName, mouseX, mouseY);

                GlStateManager.disableRescaleNormal();
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.color(1, 1, 1, 1);
            }
        }
        else
        {
            Icons.SCHEMATIC_TYPE_MEMORY.renderAt(this.typeIconX, this.typeIconY, this.zLevel, false, false);
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
                SchematicPlacement placement = SchematicPlacement.createFor(entry.getSchematic(), pos, entry.getSchematicName());
                placement.setEnabled(true);
                placement.setRenderSchematic(GuiScreen.isShiftKeyDown() == false);
                DataManager.getInstance(dimension).getSchematicPlacementManager().addSchematicPlacement(placement, this.widget.parent.getMessageConsumer());
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                Minecraft mc = Minecraft.getMinecraft();
                SchematicEntry entry = this.widget.schematicEntry;
                GuiSchematicSave gui = new GuiSchematicSave(entry.getSchematic());

                if (mc.currentScreen instanceof GuiLitematicaBase)
                {
                    gui.setParent((GuiLitematicaBase) mc.currentScreen);
                }

                mc.displayGuiScreen(gui);
            }
            else if (this.type == Type.UNLOAD)
            {
                SchematicHolder.getInstance().removeSchematic(this.widget.schematicEntry.getId());
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
