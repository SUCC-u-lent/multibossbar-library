package com.multibossbar.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.multibossbar.bossbars.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.decoration.DisplayEntity;
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
	private void render(DrawContext graphics, CallbackInfo ci) {
		List<MultibarContainer> containers =
			List.copyOf(MultibarContainer.MULTIBAR_CONTAINERS.values());

		if (!containers.isEmpty()) {
			this.client.getProfiler().push("multiBossHealth");
			int screenWidth = graphics.getScaledWindowWidth();
			boolean isNameVisible = containers.getFirst().getDisplayName() != null && !containers.getFirst().getDisplayName().getString().isBlank();
			int barY = isNameVisible ? 12 : 3;

			for (int i = 0; i < containers.size(); i++) {
				MultibarContainer container = containers.get(i);
				if (!container.isVisible()) continue;
				MultibarContainer nextContainer = null;
				if (i + 1 < containers.size()) nextContainer = containers.get(i + 1);
				Pair<Float, Boolean> pair = renderContainer(graphics, container, nextContainer, barY, screenWidth);

				if (isNameVisible) {
					Objects.requireNonNull(this.client.textRenderer);
					int nameWidth = this.client.textRenderer.getWidth(container.getDisplayName());
					int nameX = screenWidth / 2 - nameWidth / 2;
					float nameY = barY - FONT_HEIGHT;
					graphics.drawTextWithShadow(this.client.textRenderer, container.getDisplayName(), nameX, (int)nameY, 16777215);
				}
				barY += Math.round(pair.getFirst());
				if (pair.getSecond()) break;
			}

			totalUsedSize = barY;

			this.client.getProfiler().pop();
		} else {
			totalUsedSize = 0;
		}
	}

	@Unique
	private Pair<Float,Boolean> renderContainer(DrawContext graphics, MultibarContainer container, MultibarContainer nextContainer, float barY, int screenWidth) {
		float originalY = barY;

		Text bossBarName = container.getDisplayName();

		boolean isNameVisible = bossBarName != null && !bossBarName.getString().isBlank();
		float headerTextMultiplier = isNameVisible ? 1 : 0;

		int nameWidth = this.client.textRenderer.getWidth(bossBarName);

		int barX = screenWidth / 2 - BAR_INSET;

		int nameX = screenWidth / 2 - nameWidth / 2;
		float nameY = barY - (FONT_HEIGHT * headerTextMultiplier);

		List<BarSection> barSections = List.copyOf(container.getBars());
		for (int i = 0; i < barSections.size(); i++) {
			BarSection section = barSections.get(i);
			if (!section.isVisible()) continue;
			if (section.getType() == RowType.WIDE)
				barY += mbbl$renderBar(graphics, section, barX, barY, BAR_WIDTH);
			else {
				int nextIndex = i + 1;
				boolean inBounds = isInBounds(barSections, nextIndex);
				BarSection nextBarSection = null;
				if (inBounds) nextBarSection = barSections.get(nextIndex);
				if (!inBounds || nextBarSection == null || !nextBarSection.isVisible())
					barY += mbbl$renderBar(graphics, section, barX, barY, BAR_WIDTH);
				else {
					int barWidth = Math.round(BAR_WIDTH / 2f);
					float bar1Height = mbbl$renderBar(graphics, section, barX, barY, barWidth);
					float bar2Height = mbbl$renderBar(graphics, section, barX + barWidth, barY, barWidth);
					barY += Math.max(bar1Height, bar2Height);
				}
			}
		}

		if (isNameVisible) {
			Objects.requireNonNull(this.client.textRenderer);
			graphics.drawTextWithShadow(this.client.textRenderer, bossBarName, nameX, (int)nameY, 16777215);
		}

		int nextFontHeight = 0;
		if (nextContainer != null) nextFontHeight = isNameVisible ? 9 : 0;

		barY += (nextFontHeight + PADDING) + nextFontHeight;

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
	private float mbbl$renderBar(DrawContext graphics, BarSection section, float x, float y, int width) {
		boolean hasNameField = section.getName() != null && !section.getName().getString().isBlank();
		float barHeight = MathHelper.clamp(
			section.getVerticalScale(),
			!hasNameField ? 5f : 9f,
			20f
		);

		mbbl$draw(graphics, Math.round(x), Math.round(y), width, Math.round(barHeight), section, BACKGROUND_TEXTURES, NOTCHED_BACKGROUND_TEXTURES);
		int i = (int) MathHelper.clamp(section.getPercentage(), 0, width);
		if (i > 0) {
			mbbl$draw(graphics, Math.round(x), Math.round(y), i, Math.round(barHeight), section, PROGRESS_TEXTURES, NOTCHED_PROGRESS_TEXTURES);
		}

		if (hasNameField) {
			Objects.requireNonNull(this.client.textRenderer);
			drawInBounds(graphics, this.client.textRenderer, section.getName(), Math.round(x), Math.round(y), width, Math.round(barHeight), 16777215, DisplayEntity.TextDisplayEntity.TextAlignment.CENTER, DisplayEntity.TextDisplayEntity.TextAlignment.CENTER);
		}

		return barHeight;
	}

	@SuppressWarnings("resource")
	@Unique
	private void mbbl$draw(DrawContext graphics,
	                       int x,
	                       int y,
	                       int width,
						   int height,
	                       BarSection section,
	                       Identifier[] textures,
	                       Identifier[] alternativeTextures) {
		RenderSystem.enableBlend();
		Sprite lowerSprite, upperSprite;

		lowerSprite = ((DrawContextAccessor)graphics).multibossbar$getManager().getSprite(textures[section.getColor().ordinal()]);
		if (section.getStyle() == BossBar.Style.PROGRESS)
			upperSprite = null;
		else
			upperSprite = ((DrawContextAccessor)graphics).multibossbar$getManager().getSprite(alternativeTextures[section.getStyle().ordinal() - 1]);

		((DrawContextAccessor)graphics).multibossbar$invokeDrawSprite(lowerSprite, x, y, 0, width, height);
		if (upperSprite != null)
			((DrawContextAccessor)graphics).multibossbar$invokeDrawSprite(upperSprite, x, y, 0, width, height);

		RenderSystem.disableBlend();
	}

	@Unique
	private void drawInBounds(DrawContext graphics, TextRenderer textRenderer, Text text, int x, int y, int width, int height, int color, DisplayEntity.TextDisplayEntity.TextAlignment verticalAlignment, DisplayEntity.TextDisplayEntity.TextAlignment horizontalAlignment) {

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

			graphics.drawTextWithShadow(textRenderer, text, 0, 0, color);
		} finally {
			graphics.getMatrices().pop();
			graphics.disableScissor();
		}
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