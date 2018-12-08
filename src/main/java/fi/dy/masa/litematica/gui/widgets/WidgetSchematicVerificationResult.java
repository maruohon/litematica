package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.util.BlockUtils;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.wrappers.ButtonWrapper;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class WidgetSchematicVerificationResult extends WidgetBase
{
    private static int maxNameLengthExpected;
    private static int maxNameLengthFound;

    private final GuiSchematicVerifier guiSchematicVerifier;
    private final BlockMismatchEntry mismatchEntry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final BlockMismatchInfo mismatchInfo;
    private final Minecraft mc;
    private final int count;
    private final boolean isOdd;
    private final List<ButtonWrapper<?>> buttons = new ArrayList<>();
    @Nullable private final ButtonGeneric buttonIgnore;
    private int id;

    public WidgetSchematicVerificationResult(int x, int y, int width, int height, float zLevel, boolean isOdd,
            BlockMismatchEntry entry, GuiSchematicVerifier guiSchematicVerifier)
    {
        super(x, y, width, height, zLevel);

        this.mc = Minecraft.getMinecraft();
        this.mismatchEntry = entry;
        this.guiSchematicVerifier = guiSchematicVerifier;
        this.isOdd = isOdd;

        if (entry.header1 != null && entry.header2 != null)
        {
            this.header1 = entry.header1;
            this.header2 = entry.header2;
            this.header3 = GuiBase.TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.count");
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        else if (entry.header1 != null)
        {
            this.header1 = entry.header1;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        else
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = new BlockMismatchInfo(entry.blockMismatch.stateExpected, entry.blockMismatch.stateFound);
            this.count = entry.blockMismatch.count;

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;

            if (entry.mismatchType != MismatchType.CORRECT_STATE)
            {
                this.buttonIgnore = this.createButton(this.x + this.width, y + 1, ButtonListener.ButtonType.IGNORE_MISMATCH);
            }
            else
            {
                this.buttonIgnore = null;
            }

            maxNameLengthExpected = Math.max(maxNameLengthExpected, font.getStringWidth(this.mismatchInfo.stackExpected.getDisplayName()));
            maxNameLengthFound = Math.max(maxNameLengthFound, font.getStringWidth(this.mismatchInfo.stackFound.getDisplayName()));
        }
    }

    public static void resetNameLengths()
    {
        maxNameLengthExpected = 60;
        maxNameLengthFound = 60;
    }

    private ButtonGeneric createButton(int x, int y, ButtonListener.ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int buttonWidth = mc.fontRenderer.getStringWidth(label) + 10;
        x -= buttonWidth;
        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(type, this.mismatchEntry, this.guiSchematicVerifier));

        return button;
    }

    private <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonWrapper<>(button, listener));
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return (this.buttonIgnore == null || mouseX < this.buttonIgnore.x) && super.canSelectAt(mouseX, mouseY, mouseButton);
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
        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
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

        Minecraft mc = Minecraft.getMinecraft();
        int x1 = this.x + 4;
        int x2 = this.x + maxNameLengthExpected + 50;
        int x3 = x2 + maxNameLengthFound + 50;
        int y = this.y + 7;
        int color = 0xFFFFFFFF;

        if (this.header1 != null && this.header2 != null)
        {
            mc.fontRenderer.drawString(this.header1, x1, y, color);
            mc.fontRenderer.drawString(this.header2, x2, y, color);
            mc.fontRenderer.drawString(this.header3, x3, y, color);
        }
        else if (this.header1 != null)
        {
            mc.fontRenderer.drawString(this.header1, this.x + 4, this.y + 7, color);
        }
        else if (this.mismatchInfo != null &&
                (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE ||
                 this.mismatchEntry.blockMismatch.stateExpected.getBlock() != Blocks.AIR)) 
        {
            mc.fontRenderer.drawString(this.mismatchInfo.stackExpected.getDisplayName(), x1 + 20, y, color);

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                mc.fontRenderer.drawString(this.mismatchInfo.stackFound.getDisplayName(), x2 + 20, y, color);
            }

            mc.fontRenderer.drawString(String.valueOf(this.count), x3, y, color);

            GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            RenderHelper.enableGUIStandardItemLighting();

            //mc.getRenderItem().zLevel -= 110;
            y = this.y + 3;
            Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.mismatchInfo.stackExpected, x1, y);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.mismatchInfo.stackExpected, x1, y, null);

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                Gui.drawRect(x2, y, x2 + 16, y + 16, 0x20FFFFFF); // light background for the item
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.mismatchInfo.stackFound, x2, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.mismatchInfo.stackFound, x2, y, null);
            }

            //mc.getRenderItem().zLevel += 110;

            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }

        for (int i = 0; i < this.buttons.size(); ++i)
        {
            this.buttons.get(i).draw(this.mc, mouseX, mouseY, 0);
        }
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.mismatchInfo != null && this.buttonIgnore != null && mouseX < this.buttonIgnore.x)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, 0f, 200f);

            Minecraft mc = Minecraft.getMinecraft();

            int x = mouseX + 10;
            int y = mouseY;
            int width = this.mismatchInfo.getTotalWidth();
            int height = this.mismatchInfo.getTotalHeight();

            if (x + width > mc.currentScreen.width)
            {
                x = mouseX - width - 10;
            }

            if (y + height > mc.currentScreen.height)
            {
                y = mouseY - height - 2;
            }

            this.mismatchInfo.render(x, y, mc);

            GlStateManager.popMatrix();
        }
    }

    public static class BlockMismatchInfo
    {
        private final IBlockState stateExpected;
        private final IBlockState stateFound;
        private final ItemStack stackExpected;
        private final ItemStack stackFound;
        private final String blockRegistrynameExpected;
        private final String blockRegistrynameFound;
        private final String stackNameExpected;
        private final String stackNameFound;
        private final int totalWidth;
        private final int totalHeight;
        private final int columnWidthExpected;

        public BlockMismatchInfo(IBlockState stateExpected, IBlockState stateFound)
        {
            this.stateExpected = stateExpected;
            this.stateFound = stateFound;

            this.stackExpected = ItemUtils.getItemForState(this.stateExpected);
            this.stackFound = ItemUtils.getItemForState(this.stateFound);

            ResourceLocation rl1 = Block.REGISTRY.getNameForObject(this.stateExpected.getBlock());
            ResourceLocation rl2 = Block.REGISTRY.getNameForObject(this.stateFound.getBlock());
            this.blockRegistrynameExpected = rl1 != null ? rl1.toString() : "<null>";
            this.blockRegistrynameFound = rl2 != null ? rl2.toString() : "<null>";

            Minecraft mc = Minecraft.getMinecraft();
            this.stackNameExpected = this.stackExpected.getDisplayName();
            this.stackNameFound = this.stackFound.getDisplayName();
            List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected);
            List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound);

            int w1 = Math.max(mc.fontRenderer.getStringWidth(this.stackNameExpected) + 20, mc.fontRenderer.getStringWidth(this.blockRegistrynameExpected));
            int w2 = Math.max(mc.fontRenderer.getStringWidth(this.stackNameFound) + 20, mc.fontRenderer.getStringWidth(this.blockRegistrynameFound));
            w1 = Math.max(w1, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsExpected, mc));
            w2 = Math.max(w2, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsFound, mc));

            this.columnWidthExpected = w1;
            this.totalWidth = this.columnWidthExpected + w2 + 40;
            this.totalHeight = Math.max(propsExpected.size(), propsFound.size()) * (mc.fontRenderer.FONT_HEIGHT + 2) + 60;
        }

        public int getTotalWidth()
        {
            return this.totalWidth;
        }

        public int getTotalHeight()
        {
            return this.totalHeight;
        }

        public void render(int x, int y, Minecraft mc)
        {
            if (this.stateExpected != null && this.stateFound != null)
            {
                GlStateManager.pushMatrix();

                RenderUtils.drawOutlinedBox(x, y, this.totalWidth, this.totalHeight, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);

                int x1 = x + 10;
                int x2 = x + this.columnWidthExpected + 30;
                y += 4;

                String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
                String strExpected = pre + I18n.format("litematica.gui.label.schematic_verifier.expected") + GuiBase.TXT_RST;
                String strFound =    pre + I18n.format("litematica.gui.label.schematic_verifier.found") + GuiBase.TXT_RST;
                mc.fontRenderer.drawString(strExpected, x1, y, 0xFFFFFFFF);
                mc.fontRenderer.drawString(strFound,    x2, y, 0xFFFFFFFF);

                y += 12;

                GlStateManager.disableLighting();
                RenderHelper.enableGUIStandardItemLighting();

                //mc.getRenderItem().zLevel += 100;
                Gui.drawRect(x1, y, x1 + 16, y + 16, 0x20FFFFFF); // light background for the item
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackExpected, x1, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackExpected, x1, y, null);

                Gui.drawRect(x2, y, x2 + 16, y + 16, 0x20FFFFFF); // light background for the item
                mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackFound, x2, y);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, this.stackFound, x2, y, null);
                //mc.getRenderItem().zLevel -= 100;

                //GlStateManager.disableBlend();
                RenderHelper.disableStandardItemLighting();

                mc.fontRenderer.drawString(this.stackNameExpected, x1 + 20, y + 4, 0xFFFFFFFF);
                mc.fontRenderer.drawString(this.stackNameFound,    x2 + 20, y + 4, 0xFFFFFFFF);

                y += 20;
                mc.fontRenderer.drawString(this.blockRegistrynameExpected, x1, y, 0xFF4060FF);
                mc.fontRenderer.drawString(this.blockRegistrynameFound,    x2, y, 0xFF4060FF);
                y += mc.fontRenderer.FONT_HEIGHT + 4;

                List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected);
                List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound);
                RenderUtils.renderText(x1, y, 0xFFB0B0B0, propsExpected, mc.fontRenderer);
                RenderUtils.renderText(x2, y, 0xFFB0B0B0, propsFound, mc.fontRenderer);

                GlStateManager.popMatrix();
            }
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final ButtonType type;
        private final GuiSchematicVerifier guiSchematicVerifier;
        private final BlockMismatchEntry mismatchEntry;

        public ButtonListener(ButtonType type, BlockMismatchEntry mismatchEntry, GuiSchematicVerifier guiSchematicVerifier)
        {
            this.type = type;
            this.mismatchEntry = mismatchEntry;
            this.guiSchematicVerifier = guiSchematicVerifier;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.IGNORE_MISMATCH)
            {
                this.guiSchematicVerifier.getPlacement().getSchematicVerifier().ignoreStateMismatch(this.mismatchEntry.blockMismatch);
                this.guiSchematicVerifier.initGui();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            IGNORE_MISMATCH ("litematica.gui.button.schematic_verifier.ignore");

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
