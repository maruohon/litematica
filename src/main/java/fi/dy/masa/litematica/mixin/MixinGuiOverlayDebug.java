package fi.dy.masa.litematica.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.renderer.WorldRenderer;

@Mixin(GuiOverlayDebug.class)
public abstract class MixinGuiOverlayDebug
{
    private boolean captureNextCall = false;

    @Inject(method = "renderDebugInfoLeft()V", at = @At(value = "HEAD"))
    private void onRenderDebugInfoLeft(CallbackInfo ci)
    {
        this.captureNextCall = true;
    }

    @Redirect(method = "renderDebugInfoLeft()V", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;size()I"), require = 0)
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

            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add("");
            list.add(String.format("%s[Litematica]%s %s", pre, rst, renderer.getDebugInfoRenders()));

            String str = String.format("E %s TE: %d", world.getDebugLoadedEntities(), world.loadedTileEntityList.size());
            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));
        }
    }
}
