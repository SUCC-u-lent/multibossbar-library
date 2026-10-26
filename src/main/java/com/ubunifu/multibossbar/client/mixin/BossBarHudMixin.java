package com.ubunifu.multibossbar.client.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.ubunifu.multibossbar.common.boss_bar.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.hud.boss_bar.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Stream;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	@Final
	private static Identifier[] BACKGROUND_TEXTURES;

	@Shadow
	@Final
	private static Identifier[] NOTCHED_BACKGROUND_TEXTURES;

	@Shadow
	@Final
	private static Identifier[] PROGRESS_TEXTURES;

	@Shadow
	@Final
	private static Identifier[] NOTCHED_PROGRESS_TEXTURES;

	@Unique
	private int totalUsedSize;

	@Unique
	private static final int BAR_INSET = 91;
	@Unique
	private static final int FONT_HEIGHT = 9;
	@Unique
	private static final int PADDING = 2;
	@Unique
	private static final int BAR_WIDTH = 182;

	@ModifyVariable(
		method = "render",
		at = @At(
			value = "STORE"
		),
		name = "j")
	private int addBossBarHudOffset(int j) {
		return j + totalUsedSize;
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void render(GuiGraphics graphics, CallbackInfo ci) {
		List<MultibarContainer> containers =
			List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());

		if (!containers.isEmpty()) {
			this.client.getProfiler().push("multiBossHealth");
			int screenWidth = graphics.getScaledWindowWidth();
			int barY = 12;

			for (MultibarContainer container : containers) {
				Pair<Float, Boolean> pair = renderContainer(graphics, container, barY, screenWidth);
				barY += Math.round(pair.getFirst());
				if (pair.getSecond()) break;
			}
			totalUsedSize = barY;

			this.client.getProfiler().pop();
		}
	}

	@Unique
	private Pair<Float,Boolean> renderContainer(GuiGraphics graphics, MultibarContainer container, float barY, int screenWidth) {
		float originalY = barY;

		Text bossBarName = container.getDisplayName();

		int nameWidth = this.client.textRenderer.getWidth(bossBarName);

		int barX = screenWidth / 2 - BAR_INSET;

		int nameX = screenWidth / 2 - nameWidth / 2;
		float nameY = barY - FONT_HEIGHT;

		List<BarSection> barSections = List.copyOf(container.getBars());
		for (int i = 0; i < barSections.size(); i++) {
			BarSection section = barSections.get(i);
			if (section.getType() == RowType.WIDE)
				barY += mbbl$renderBar(graphics, section, barX, barY, BAR_WIDTH);
			else {
				int nextIndex = i + 1;
				boolean inBounds = isInBounds(barSections, nextIndex);
				BarSection nextBarSection = null;
				if (inBounds) nextBarSection = barSections.get(nextIndex);
				if (!inBounds || nextBarSection == null)
					barY += mbbl$renderBar(graphics, section, barX, barY, BAR_WIDTH);
				else {
					int barWidth = Math.round(BAR_WIDTH / 2f);
					float bar1Height = mbbl$renderBar(graphics, section, barX, barY, barWidth);
					float bar2Height = mbbl$renderBar(graphics, section, barX + barWidth, barY, barWidth);
					barY += Math.max(bar1Height, bar2Height);
				}
			}
		}

		Objects.requireNonNull(this.client.textRenderer);
		graphics.drawShadowedText(this.client.textRenderer, bossBarName, nameX, (int)nameY, 16777215);

		barY += (FONT_HEIGHT + PADDING) + FONT_HEIGHT;

		// End.
		if (barY >= graphics.getScaledWindowHeight() / 3f) {
			return Pair.of(barY - originalY, true);
		}
		return Pair.of(barY - originalY, false);
	}

	@Unique
	boolean isInBounds(List<?> tbl, int index) {
		if (tbl == null || tbl.isEmpty()) return false;
		return index >= 0 && index < tbl.size();
	}

	@Unique
	private float mbbl$renderBar(GuiGraphics graphics, BarSection section, float x, float y, int width) {
		float barHeight = MathHelper.clamp(
			section.getVerticalScale(),
			section.getName().getString().isBlank() ? 5f : 9f,
			20f
		);

		mbbl$draw(graphics, Math.round(x), Math.round(y), width, Math.round(barHeight), section, BACKGROUND_TEXTURES, NOTCHED_BACKGROUND_TEXTURES);
		int i = MathHelper.method_53063((float) section.getPercentage(), 0, width);
		if (i > 0) {
			mbbl$draw(graphics, Math.round(x), Math.round(y), i, Math.round(barHeight), section, PROGRESS_TEXTURES, NOTCHED_PROGRESS_TEXTURES);
		}

		return barHeight;
	}

	@SuppressWarnings("resource")
	@Unique
	private void mbbl$draw(GuiGraphics graphics,
	                       int x,
	                       int y,
	                       int width,
						   int height,
	                       BarSection section,
	                       Identifier[] textures,
	                       Identifier[] alternativeTextures) {
		RenderSystem.enableBlend();
		Sprite lowerSprite, upperSprite;

		lowerSprite = ((GuiGraphicsAccessor)graphics).multibossbar$getManager().getSprite(textures[section.getColor().ordinal()]);
		if (section.getStyle() == BossBar.Style.PROGRESS)
			upperSprite = null;
		else
			upperSprite = ((GuiGraphicsAccessor)graphics).multibossbar$getManager().getSprite(alternativeTextures[section.getStyle().ordinal() - 1]);

		((GuiGraphicsAccessor)graphics).multibossbar$invokeDrawSprite(lowerSprite, x, y, 0, width, height);
		if (upperSprite != null)
			((GuiGraphicsAccessor)graphics).multibossbar$invokeDrawSprite(upperSprite, x, y, 0, width, height);

		RenderSystem.disableBlend();
	}

	@Inject(method = "shouldPlayDragonMusic",at=@At("RETURN"),cancellable = true)
	private void playBossMusic(CallbackInfoReturnable<Boolean> cir)
	{
		List<MultibarContainer> bossBars = List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());
		Stream<MultibarContainer> bossBarStream = bossBars.stream();
		if (bossBars.isEmpty()){
			bossBarStream.close(); // Close stream.
			return;
		}
		if (bossBarStream.filter(MultibarContainer::isVisible).anyMatch(MultibarContainer::isDragonMusic)) cir.setReturnValue(true);
	}
	@Inject(method = "shouldDarkenSky",at=@At("RETURN"),cancellable = true)
	private void darkenSky(CallbackInfoReturnable<Boolean> cir)
	{
		List<MultibarContainer> bossBars = List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());
		Stream<MultibarContainer> bossBarStream = bossBars.stream();
		if (bossBars.isEmpty()){
			bossBarStream.close(); // Close stream.
			return;
		}
		if (bossBarStream.filter(MultibarContainer::isVisible).anyMatch(MultibarContainer::isDarkenSky)) cir.setReturnValue(true);
	}
	@Inject(method = "shouldThickenFog",at=@At("RETURN"),cancellable = true)
	private void thickenFog(CallbackInfoReturnable<Boolean> cir)
	{
		List<MultibarContainer> bossBars = List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());
		Stream<MultibarContainer> bossBarStream = bossBars.stream();
		if (bossBars.isEmpty()){
			bossBarStream.close(); // Close stream.
			return;
		}
		if (bossBarStream.filter(MultibarContainer::isVisible).anyMatch(MultibarContainer::isThickenFog)) cir.setReturnValue(true);
	}
}
