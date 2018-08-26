package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.RenderUtils;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonWrapper;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class WidgetSchematicPlacement extends WidgetBase
{
    private final SchematicPlacementManager manager;
    private final WidgetListSchematicPlacements parent;
    private final SchematicPlacement placement;
    private final Minecraft mc;
    private final List<ButtonWrapper<?>> buttons = new ArrayList<>();
    private final boolean isOdd;
    private int id;
    private int buttonsStartX;

    public WidgetSchematicPlacement(int x, int y, int width, int height, float zLevel, boolean isOdd,
            SchematicPlacement placement, WidgetListSchematicPlacements parent, Minecraft mc)
    {
        super(x, y, width, height, zLevel);

        this.parent = parent;
        this.placement = placement;
        this.isOdd = isOdd;
        this.mc = mc;
        this.manager = DataManager.getInstance().getSchematicPlacementManager();

        this.id = 0;
        int posX = x + width;
        int posY = y + 1;

        // Note: These are placed from right to left

        posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);

        String labelEn = I18n.format("litematica.gui.button.schematic_placements.render_enable");
        String labelDis = I18n.format("litematica.gui.button.schematic_placements.render_disable");
        String label = this.placement.isRenderingEnabled() ? labelDis : labelEn;
        int len = Math.max(mc.fontRenderer.getStringWidth(labelEn), mc.fontRenderer.getStringWidth(labelEn)) + 10;
        posX -= (len + 2);
        ButtonListener listener = new ButtonListener(ButtonListener.ButtonType.TOGGLE_RENDER, this);
        this.addButton(new ButtonGeneric(this.id++, posX, posY, len, 20, label), listener);

        labelEn = I18n.format("litematica.gui.button.schematic_placements.enable");
        labelDis = I18n.format("litematica.gui.button.schematic_placements.disable");
        label = this.placement.isEnabled() ? labelDis : labelEn;
        len = Math.max(mc.fontRenderer.getStringWidth(labelEn), mc.fontRenderer.getStringWidth(labelEn)) + 10;
        posX -= (len + 2);
        listener = new ButtonListener(ButtonListener.ButtonType.TOGGLE_ENABLED, this);
        this.addButton(new ButtonGeneric(this.id++, posX, posY, len, 20, label), listener);

        posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);

        this.buttonsStartX = posX;
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int len = mc.fontRenderer.getStringWidth(label) + 10;
        x -= (len + 2);
        this.addButton(new ButtonGeneric(this.id++, x, y, len, 20, label), new ButtonListener(type, this));

        return x;
    }

    private <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
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
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        GlStateManager.color(1, 1, 1, 1);

        boolean placementSelected = this.manager.getSelectedSchematicPlacement() == this.placement;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || placementSelected || this.isMouseOver(mouseX, mouseY))
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0303030);
        }

        if (placementSelected)
        {
            GlStateManager.translate(0, 0, 1);
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0);
            GlStateManager.translate(0, 0, -1);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
        this.mc.fontRenderer.drawString(pre + name, this.x + 20, this.y + 7, 0xFFFFFFFF);

        Icons icon;

        if (this.placement.getSchematic().getFile() != null)
        {
            icon = Icons.SCHEMATIC_TYPE_FILE;
        }
        else
        {
            icon = Icons.SCHEMATIC_TYPE_MEMORY;
        }

        //GlStateManager.disableRescaleNormal();
        //RenderHelper.disableStandardItemLighting();
        //GlStateManager.disableLighting();
        GlStateManager.color(1, 1, 1, 1);

        this.parent.bindTexture(Icons.TEXTURE);
        icon.renderAt(this.x + 2, this.y + 5, this.zLevel, false, false);

        if (this.placement.isRegionPlacementModified())
        {
            icon = Icons.NOTICE_EXCLAMATION_11;
            icon.renderAt(this.buttonsStartX - icon.getWidth() - 2, this.y + 6, this.zLevel, false, false);
        }

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        File schematicFile = this.placement.getSchematic().getFile();
        String fileName = schematicFile != null ? schematicFile.getName() : I18n.format("litematica.gui.label.schematic_placement.in_memory");

        List<String> text = new ArrayList<>();
        text.add(I18n.format("litematica.gui.label.schematic_placement.schematic_name", this.placement.getSchematic().getMetadata().getName()));
        text.add(I18n.format("litematica.gui.label.schematic_placement.schematic_file", fileName));

        int offset = 12 + 11 + 2; // this.x + modified icon + gap to buttons

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - offset, this.height))
        {
            this.parent.drawHoveringText(text, mouseX, mouseY);
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.x + this.buttonsStartX - offset, this.y + 6, 11, 11))
        {
            String str = I18n.format("litematica.hud.schematic_placement.hover_info.placement_modified");
            this.parent.drawHoveringText(str, mouseX, mouseY);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final ButtonType type;
        private final WidgetSchematicPlacement widget;

        public ButtonListener(ButtonType type, WidgetSchematicPlacement widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.CONFIGURE)
            {
                GuiPlacementConfiguration gui = new GuiPlacementConfiguration(this.widget.placement);
                gui.setParent(this.widget.parent.getParentGui());
                Minecraft.getMinecraft().displayGuiScreen(gui);
            }
            else if (this.type == ButtonType.REMOVE)
            {
                this.widget.manager.removeSchematicPlacement(this.widget.placement);
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.TOGGLE_ENABLED)
            {
                this.widget.placement.setEnabled(! this.widget.placement.isEnabled());
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.TOGGLE_RENDER)
            {
                this.widget.placement.setRenderSchematic(! this.widget.placement.isRenderingEnabled());
                this.widget.parent.refreshEntries();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            CONFIGURE       ("litematica.gui.button.schematic_placements.configure"),
            REMOVE          ("litematica.gui.button.schematic_placements.remove"),
            TOGGLE_ENABLED  (""),
            TOGGLE_RENDER   ("");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }
}
