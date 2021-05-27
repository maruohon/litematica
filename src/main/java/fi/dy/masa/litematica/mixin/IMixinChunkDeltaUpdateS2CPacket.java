package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.play.server.SMultiBlockChangePacket;
import net.minecraft.util.math.SectionPos;

@Mixin(SMultiBlockChangePacket.class)
public interface IMixinChunkDeltaUpdateS2CPacket
{
    @Accessor("sectionPos")
    SectionPos litematica_getSection();
}
