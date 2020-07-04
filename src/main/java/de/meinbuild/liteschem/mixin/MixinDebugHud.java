package de.meinbuild.liteschem.mixin;

import java.util.List;

import de.meinbuild.liteschem.render.LitematicaRenderer;
import de.meinbuild.liteschem.world.SchematicWorldHandler;
import de.meinbuild.liteschem.world.WorldSchematic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import de.meinbuild.liteschem.render.schematic.WorldRendererSchematic;
import fi.dy.masa.malilib.gui.GuiBase;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud extends DrawableHelper
{
    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void addDebugLines(CallbackInfoReturnable<List<String>> cir)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            List<String> list = cir.getReturnValue();
            String pre = GuiBase.TXT_GOLD;
            String rst = GuiBase.TXT_RST;

            WorldRendererSchematic renderer = LitematicaRenderer.getInstance().getWorldRenderer();

            list.add(String.format("%s[Litematica]%s %s C#: %d", pre, rst, renderer.getDebugInfoRenders(), world.getChunkProvider().getLoadedChunks().size()));

            String str = String.format("E: %d TE: %d", world.getRegularEntityCount(), world.blockEntities.size());
            list.add(String.format("%s[Litematica]%s %s %s", pre, rst, renderer.getDebugInfoEntities(), str));
        }
    }
}
