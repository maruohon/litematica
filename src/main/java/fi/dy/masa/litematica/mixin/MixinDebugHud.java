package fi.dy.masa.litematica.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud extends DrawableHelper
{
    private boolean captureNextCall = false;

    @Inject(method = "drawLeftText()V", at = @At(value = "HEAD"))
    private void onRenderDebugInfoLeft(CallbackInfo ci)
    {
        this.captureNextCall = true;
    }

    @Redirect(method = "drawLeftText()V", require = 0,
              at = @At(value = "INVOKE", remap = false,
                       target = "Ljava/util/List;size()I"))
    private int getSize(List<String> list)
    {
        if (this.captureNextCall)
        {
            this.captureNextCall = false;
            this.addInfoLines(list);
        }

        return list.size();
    }

    private void addInfoLines(List<String> list)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            String pre = GuiBase.TXT_GOLD;
            String rst = GuiBase.TXT_RST;

            WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add("");
            list.add(String.format("%s[Litematica]%s %s", pre, rst, renderer.getDebugInfoRenders()));

            String str = String.format("E: %d TE: %d", world.getRegularEntityCount(), world.blockEntities.size());
            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));
        }
    }
}
