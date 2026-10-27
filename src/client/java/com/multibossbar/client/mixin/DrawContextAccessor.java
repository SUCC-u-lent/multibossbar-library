package com.multibossbar.client.mixin;

import net.minecraft.client.gui.*;
import net.minecraft.client.texture.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {
	@Accessor("guiAtlasManager")
    GuiAtlasManager multibossbar$getManager();
    @Invoker("drawSprite")
    void multibossbar$invokeDrawSprite(
        Sprite sprite,
        int x,
        int y,
        int z,
        int width,
        int height
    );
}