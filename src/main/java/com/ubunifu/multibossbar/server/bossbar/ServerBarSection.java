package com.ubunifu.multibossbar.server.bossbar;

import com.ubunifu.multibossbar.common.boss_bar.*;
import com.ubunifu.multibossbar.common.networking.barPacket.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;

import java.util.UUID;

public class ServerBarSection extends BarSection {
	public ServerBarSection(MultibarContainer container, UUID uuid, Text name, double percentage, BossBar.Color color, BossBar.Style style, RowType rowType, float verticalScale) {
		super(container, uuid, name, percentage, color, style, rowType, verticalScale);
	}

	protected ServerBarSection(MultibarContainer container, UUID uuid) {
		super(container, uuid);
	}

	@Override
	protected void onConstructed() {
		super.onConstructed();
		BossBarS2CPacket.sendPacket(BarPacketType.CREATE, this.getContainer(), this);
	}

	@Override
	public void setName(Text name) {
		Text prev = this.getName();
		super.setName(name);
		if (prev != name) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_NAME, this.getContainer(), this);
	}

	@Override
	public void setPercentage(double percentage) {
		double prev = this.getPercentage();
		super.setPercentage(percentage);
		if (prev != percentage) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_PERCENTAGE, this.getContainer(), this);
	}

	@Override
	public void setColor(BossBar.Color color) {
		BossBar.Color prev = this.getColor();
		super.setColor(color);
		if (prev != color) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_COLOR, this.getContainer(), this);
	}

	@Override
	public void setStyle(BossBar.Style style) {
		BossBar.Style prev = this.getStyle();
		super.setStyle(style);
		if (prev != style) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_STYLE, this.getContainer(), this);
	}

	@Override
	public void setVerticalScale(float verticalScale) {
		float prev = this.getVerticalScale();
		super.setVerticalScale(verticalScale);
		if (prev != verticalScale) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_SCALE, this.getContainer(), this);
	}

	@Override
	public void setVisible(boolean visible) {
		boolean prev = this.isVisible();
		super.setVisible(visible);
		if (prev != visible) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_VISIBILITY, this.getContainer(), this);
	}

	@Override
	public void setType(RowType type) {
		RowType prev = this.getType();
		super.setType(type);
		if (prev != type) BossBarS2CPacket.sendPacket(BarPacketType.UPDATE_TYPE, this.getContainer(), this);
	}
}
