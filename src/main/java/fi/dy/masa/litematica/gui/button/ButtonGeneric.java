package fi.dy.masa.litematica.gui.button;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.ButtonIcon;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.LeftRight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class ButtonGeneric extends ButtonBase
{
    @Nullable
    private final ButtonIcon icon;
    private LeftRight alignment = LeftRight.LEFT;
    private boolean textCentered;

    public ButtonGeneric(int id, int x, int y, int width, int height, String text)
    {
        this(id, x, y, width, height, text, null);

        this.textCentered = true;
    }

    public ButtonGeneric(int id, int x, int y, int width, int height, String text, ButtonIcon icon)
    {
        super(id, x, y, width, height, text);

        this.icon = icon;
    }

    public ButtonGeneric setTextCentered(boolean centered)
    {
        this.textCentered = centered;
        return this;
    }

    public ButtonGeneric setIconAlignment(LeftRight alignment)
    {
        this.alignment = alignment;
        return this;
    }

    @Override
    public void onMouseClicked()
    {
    }

    @Override
    public void onMouseButtonClicked(int mouseButton)
    {
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

            FontRenderer fontRenderer = mc.fontRenderer;
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            int buttonStyle = this.getHoverState(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            this.drawTexturedModalRect(this.x, this.y, 0, 46 + buttonStyle * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + buttonStyle * 20, this.width / 2, this.height);
            this.mouseDragged(mc, mouseX, mouseY);

            if (this.icon != null)
            {
                int x = this.alignment == LeftRight.LEFT ? this.x + 4 : this.x + this.width - this.icon.getWidth() - 4;
                int y = this.y + (this.height - this.icon.getHeight()) / 2;
                int u = this.icon.getU() + buttonStyle * this.icon.getWidth();

                mc.getTextureManager().bindTexture(ButtonIcon.TEXTURE);
                this.drawTexturedModalRect(x, y, u, this.icon.getV(), this.icon.getWidth(), this.icon.getHeight());
            }

            int color = 0xE0E0E0;

            if (this.enabled == false)
            {
                color = 0xA0A0A0;
            }
            else if (this.hovered)
            {
                color = 0xFFFFA0;
            }

            int y = this.y + (this.height - 8) / 2;

            if (this.textCentered)
            {
                this.drawCenteredString(fontRenderer, this.displayString, this.x + this.width / 2, y, color);
            }
            else
            {
                int x = this.x + 6;

                if (this.icon != null && this.alignment == LeftRight.LEFT)
                {
                    x += this.icon.getWidth() + 2;
                }

                this.drawString(fontRenderer, this.displayString, x, y, color);
            }
        }
    }
}
