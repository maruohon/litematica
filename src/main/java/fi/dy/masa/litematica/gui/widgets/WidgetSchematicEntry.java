package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.gui.wrappers.ButtonWrapper;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicEntry extends WidgetListEntryBase<LitematicaSchematic>
{
    private final WidgetListLoadedSchematics parent;
    private final LitematicaSchematic schematic;
    private final Minecraft mc;
    private final List<ButtonWrapper<? extends ButtonGeneric>> buttons = new ArrayList<>();
    private final int typeIconX;
    private final int typeIconY;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSchematicEntry(int x, int y, int width, int height, float zLevel, boolean isOdd,
            LitematicaSchematic schematic, int listIndex, WidgetListLoadedSchematics parent, Minecraft mc)
    {
        super(x, y, width, height, zLevel, schematic, listIndex);

        this.parent = parent;
        this.schematic = schematic;
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
        String tip = I18n.format("litematica.gui.label.schematic_placement.hoverinfo.hold_shift_to_create_as_disabled");
        listener = new ButtonListener(ButtonListener.Type.CREATE_PLACEMENT, this);
        this.addButton(new ButtonGeneric(0, posX, y, len, 20, text, tip), listener);

        this.buttonsStartX = posX;
        this.typeIconX = this.x + 2;
        this.typeIconY = y + 4;
    }

    protected <T extends ButtonGeneric> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonWrapper<>(button, listener));
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton)
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
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0x50FFFFFF);
        }

        String schematicName = this.schematic.getMetadata().getName();
        this.mc.fontRenderer.drawString(schematicName, this.x + 20, this.y + 7, 0xFFFFFFFF);

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableBlend();
        File schematicFile = this.schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getName() : null;
        this.parent.bindTexture(Icons.TEXTURE);

        Icons icon;
        String text;

        if (fileName != null)
        {
            icon = Icons.SCHEMATIC_TYPE_FILE;
            text = fileName;
        }
        else
        {
            icon = Icons.SCHEMATIC_TYPE_MEMORY;
            text = I18n.format("litematica.gui.label.schematic_placement.in_memory");
        }

        icon.renderAt(this.typeIconX, this.typeIconY, this.zLevel, false, false);

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 12, this.height))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, ImmutableList.of(text));
        }

        for (ButtonWrapper<? extends ButtonGeneric> entry : this.buttons)
        {
            ButtonGeneric button = entry.getButton();

            if (button.hasHoverText() && button.isMouseOver())
            {
                RenderUtils.drawHoverText(mouseX, mouseY, button.getHoverStrings());
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
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
                BlockPos pos = new BlockPos(mc.player.getPositionVector());
                LitematicaSchematic entry = this.widget.schematic;
                String name = entry.getMetadata().getName();
                boolean enabled = GuiScreen.isShiftKeyDown() == false;

                SchematicPlacement placement = SchematicPlacement.createFor(entry, pos, name, enabled, enabled);
                DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, this.widget.parent.getMessageConsumer());
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                Minecraft mc = Minecraft.getMinecraft();
                LitematicaSchematic entry = this.widget.schematic;
                GuiSchematicSave gui = new GuiSchematicSave(entry);

                if (mc.currentScreen instanceof GuiBase)
                {
                    gui.setParent((GuiBase) mc.currentScreen);
                }

                mc.displayGuiScreen(gui);
            }
            else if (this.type == Type.UNLOAD)
            {
                SchematicHolder.getInstance().removeSchematic(this.widget.schematic);
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
