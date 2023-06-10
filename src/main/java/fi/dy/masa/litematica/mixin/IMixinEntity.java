package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

@Mixin(Entity.class)
public interface IMixinEntity {
    @Accessor("world")
    void litematica_setWorld(World world);
}
