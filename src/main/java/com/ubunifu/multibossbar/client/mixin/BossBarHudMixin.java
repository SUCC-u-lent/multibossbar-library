package com.ubunifu.multibossbar.client.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import com.ubunifu.multibossbar.common.boss_bar.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.hud.boss_bar.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
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

	@Shadow
	protected abstract void draw(GuiGraphics graphics, int x, int y, BossBar bar, int width, Identifier[] textures, Identifier[] alternativeTextures);
	@Unique
	private int totalUsedSize;

	@ModifyVariable(
		method = "render",
		at = @At(
			value = "STORE"
		),
		name = "j")
	private int addBossBarHudOffset(int j) {
		return j + totalUsedSize;
	}

	@Inject(method = "render",at=@At("HEAD"))
	private void renderMixin(GuiGraphics graphics, CallbackInfo ci)
	{
		List<MultibarContainer> bossBars = List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());

		if (!bossBars.isEmpty()) {
			this.client.getProfiler().push("bossHealth");
			int i = graphics.getScaledWindowWidth();
			int j = 12;

			int k = i / 2 - 91;
			for(MultibarContainer clientBossBar : bossBars) {
				if (!clientBossBar.isVisible()) continue;
				if (clientBossBar.isEmpty()) continue;
				int assetHeight = mbbl$renderBar(graphics, k, j, clientBossBar);
				Text text = clientBossBar.getDisplayName();
				if (text == null) continue;
				int m = this.client.textRenderer.getWidth(text);
				int n = i / 2 - m / 2;
				int o = j - 9;
				graphics.drawShadowedText(this.client.textRenderer, text, n, o, 16777215);
				Objects.requireNonNull(this.client.textRenderer);
				j += assetHeight + 1;
				if (j >= graphics.getScaledWindowHeight() / 3) {
					break;
				}
			}
			totalUsedSize = j;

			this.client.getProfiler().pop();
		} else {
			totalUsedSize = 0;
		}
	}

	@Unique
	private int mbbl$renderBar(GuiGraphics graphics, int x, int y, MultibarContainer bar)
	{
		int PADDING = 2;
		int barWidth = 182;
		int barY = y;
		List<BarSection> sections = bar.getBars();
		for (int i = 0; i < sections.size(); i++) {
			BarSection section = sections.get( i );
			if (section == null || !section.isVisible()) continue;
			if (section.getType() == RowType.MULTI && i < sections.size() - 1) {
				BarSection nextBar = sections.get( ++i );
				if (nextBar != null && nextBar.isVisible()){
					int section1Height = mbbl$renderSection(graphics,x,barY,barWidth/2,section);
					int section2Height = mbbl$renderSection(graphics,x+(barWidth/2),barY,barWidth/2,nextBar);
					int textX = x + PADDING; // Add 1 to get rid of border
					int textY = barY + PADDING;
					barY += Math.max(section1Height, section2Height);
					Text text1 = section.getName();
					Text text2 = nextBar.getName();
					drawInBounds(graphics,this.client.textRenderer, text1, textX, textY, (barWidth / 2) - PADDING, section1Height - PADDING, 16777215, DisplayEntity.TextDisplayEntity.Alignment.CENTER, DisplayEntity.TextDisplayEntity.Alignment.CENTER);
					drawInBounds(graphics,this.client.textRenderer, text2, textX+(barWidth/2), textY, (barWidth / 2) - PADDING, section2Height - PADDING, 16777215, DisplayEntity.TextDisplayEntity.Alignment.CENTER, DisplayEntity.TextDisplayEntity.Alignment.CENTER);
					continue;
				} else i--;
			}
			int secHeight = mbbl$renderSection( graphics, x, barY, barWidth, section );
			int textX = x + PADDING; // Add 1 to get rid of border
			int textY = barY + PADDING;
			barY += secHeight;
			Text text1 = section.getName();
			drawInBounds(graphics,this.client.textRenderer, text1, textX, textY, barWidth - PADDING, secHeight - PADDING, 16777215, DisplayEntity.TextDisplayEntity.Alignment.CENTER, DisplayEntity.TextDisplayEntity.Alignment.CENTER);
		}
		return barY - y; // Considering we add Y after rendering it the last section will add its height, but we remove Y to not account that.
	}

	@Unique
	private void drawInBounds(GuiGraphics graphics, TextRenderer textRenderer, Text text, int x, int y, int width, int height, int color, DisplayEntity.TextDisplayEntity.Alignment verticalAlignment, DisplayEntity.TextDisplayEntity.Alignment horizontalAlignment) {

		// A font height of approximately 10 units is our reference size.
		float scale = height / 10.0f;

		if (scale <= 0.0f) return;

		int textWidth = textRenderer.getWidth(text);
		int textHeight = textRenderer.fontHeight;

		float scaledWidth = textWidth * scale;
		float scaledHeight = textHeight * scale;

		float drawX = switch (horizontalAlignment) {
			case LEFT -> x;
			case CENTER -> x + (width - scaledWidth) / 2.0f;
			case RIGHT -> x + width - scaledWidth;
		};

		float drawY = switch (verticalAlignment) {
			case LEFT -> y;
			case CENTER -> y + (height - scaledHeight) / 2.0f;
			case RIGHT -> y + height - scaledHeight;
		};

		graphics.enableScissor(x, y, x + width, y + height);
		graphics.getMatrices().push();

		try {
			graphics.getMatrices().translate(drawX, drawY, 0);
			graphics.getMatrices().scale(scale, scale, 1.0f);

			graphics.drawShadowedText(textRenderer, text, 0, 0, color);
		} finally {
			graphics.getMatrices().pop();
			graphics.disableScissor();
		}
	}

	@Unique
	private <T> T safeGet(T[] tbl, int index) {
		if (tbl.length == 0) return null;
		if (index < 0 || index > tbl.length - 1) return null;
		return tbl[index];
	}

	@Unique
	private int mbbl$renderSection(GuiGraphics graphics, int x, int y, int width, BarSection section) {
		int imageHeight = mbbl$drawBarSection(graphics, x, y, section, width, BACKGROUND_TEXTURES, NOTCHED_BACKGROUND_TEXTURES);
		int filledWidth = MathHelper.ceil(
			MathHelper.clamp(section.getPercentage(), 0.0F, 1.0F) * width
		);
		if (filledWidth > 0)
			mbbl$drawBarSection(graphics, x, y, section, filledWidth, PROGRESS_TEXTURES, NOTCHED_PROGRESS_TEXTURES);
		return imageHeight;
	}

	@SuppressWarnings("resource")
	@Unique
	private int mbbl$drawBarSection(GuiGraphics graphics, int x, int y, BarSection bar, int width, Identifier[] textures, Identifier[] alternativeTextures) {
		boolean isEmptyText = bar.getName() == null || bar.getName().getString().isEmpty() || bar.getName().getString().isBlank();
		int scaleClampOffset = isEmptyText ? 0 : 5;
		int barHeight = (int) MathHelper.clamp(bar.getVerticalScale(), scaleClampOffset + 5, scaleClampOffset + 20);
		RenderSystem.enableBlend();

		Sprite sprite;

		Identifier alterIdentifier = safeGet(alternativeTextures, bar.getStyle().ordinal() - 1);
		Identifier plainIdentifier = safeGet(textures, bar.getColor().ordinal());
		if (alterIdentifier == null) alterIdentifier = alternativeTextures[0];
		if (plainIdentifier == null) plainIdentifier = textures[0];

		if (bar.getStyle() != BossBar.Style.PROGRESS) sprite = ((GuiGraphicsAccessor)graphics).multibossbar$getManager().getSprite(alterIdentifier);
		else sprite = ((GuiGraphicsAccessor)graphics).multibossbar$getManager().getSprite(plainIdentifier);

		((GuiGraphicsAccessor)graphics).multibossbar$invokeDrawSprite(sprite,
			x,
			y,
			0,
			width,
			barHeight);
		RenderSystem.disableBlend();
		return barHeight;
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
