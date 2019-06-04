package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
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
    private final int typeIconX;
    private final int typeIconY;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSchematicEntry(int x, int y, int width, int height, boolean isOdd,
            LitematicaSchematic schematic, int listIndex, WidgetListLoadedSchematics parent)
    {
        super(x, y, width, height, schematic, listIndex);

        this.parent = parent;
        this.schematic = schematic;
        this.isOdd = isOdd;
        y += 1;

        int posX = x + width;
        int len;
        ButtonListener listener;
        String text;

        text = I18n.format("litematica.gui.button.unload");
        len = this.getStringWidth(text) + 10;
        posX -= (len + 2);
        listener = new ButtonListener(ButtonListener.Type.UNLOAD, this);
        this.addButton(new ButtonGeneric(posX, y, len, 20, text), listener);

        text = I18n.format("litematica.gui.button.save_to_file");
        len = this.getStringWidth(text) + 10;
        posX -= (len + 2);
        listener = new ButtonListener(ButtonListener.Type.SAVE_TO_FILE, this);
        this.addButton(new ButtonGeneric(posX, y, len, 20, text), listener);

        text = I18n.format("litematica.gui.button.create_placement");
        len = this.getStringWidth(text) + 10;
        posX -= (len + 2);
        String tip = I18n.format("litematica.gui.label.schematic_placement.hoverinfo.hold_shift_to_create_as_disabled");
        listener = new ButtonListener(ButtonListener.Type.CREATE_PLACEMENT, this);
        this.addButton(new ButtonGeneric(posX, y, len, 20, text, tip), listener);

        this.buttonsStartX = posX;
        this.typeIconX = this.x + 2;
        this.typeIconY = y + 4;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        GlStateManager.color4f(1f, 1f, 1f, 1f);

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
        this.drawString(schematicName, this.x + 20, this.y + 7, 0xFFFFFFFF);

        GlStateManager.color4f(1f, 1f, 1f, 1f);
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

        this.drawSubWidgets(mouseX, mouseY);

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 12, this.height))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, ImmutableList.of(text));
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final WidgetSchematicEntry widget;

        public ButtonListener(Type type, WidgetSchematicEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.CREATE_PLACEMENT)
            {
                Minecraft mc = Minecraft.getInstance();
                BlockPos pos = new BlockPos(mc.player.getPositionVector());
                LitematicaSchematic entry = this.widget.schematic;
                String name = entry.getMetadata().getName();
                boolean enabled = GuiScreen.isShiftKeyDown() == false;

                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                SchematicPlacement placement = SchematicPlacement.createFor(entry, pos, name, enabled, enabled);
                manager.addSchematicPlacement(placement, true);
                manager.setSelectedSchematicPlacement(placement);
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                Minecraft mc = Minecraft.getInstance();
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

        public enum Type
        {
            CREATE_PLACEMENT,
            SAVE_TO_FILE,
            UNLOAD;
        }
    }
}
