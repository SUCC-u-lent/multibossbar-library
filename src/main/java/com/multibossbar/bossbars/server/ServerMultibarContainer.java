package com.multibossbar.bossbars.server;

import com.multibossbar.bossbars.*;
import com.multibossbar.networking.bar_packet.*;
import com.multibossbar.networking.container_packet.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class ServerMultibarContainer extends MultibarContainer {
	final List<ServerPlayerEntity> PLAYERS = new ArrayList<>();

	public ServerMultibarContainer() {
	}

	public ServerMultibarContainer(UUID uuid) {
		super(uuid);
	}

	public ServerMultibarContainer(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
		super(uuid, darkenSky, dragonMusic, thickenFog);
	}

	public boolean addPlayer(ServerPlayerEntity player) {
		if (PLAYERS.stream().anyMatch(p -> p.getUuid().equals(player.getUuid()))) return false;
		BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.CREATE, this, List.of(player));
		for (BarSection section : getBars()) {
			BossBarS2CPacket.sendPacket(
				BarPacketType.CREATE, this, section, List.of(player)
			);
		}
		return this.PLAYERS.add(player);
	}

	public boolean removePlayer(ServerPlayerEntity player) {
		BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.REMOVE, this, List.of(player));
		return this.PLAYERS.removeIf(p->p.getUuid().equals(player.getUuid()) || p.equals(player));
	}

	public boolean clearPlayers() {
		boolean hasPlayers = !this.PLAYERS.isEmpty();
		BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.REMOVE, this, this.PLAYERS);
		this.PLAYERS.clear();
		return hasPlayers;
	}

	@Override
	public void setThickenFog(boolean thickenFog) {
		boolean prev = this.isThickenFog();
		super.setThickenFog(thickenFog);
		if (prev != thickenFog) BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.UPDATE_FOG, this, this.PLAYERS);
	}

	@Override
	public void setDragonMusic(boolean dragonMusic) {
		boolean prev = this.isDragonMusic();
		super.setDragonMusic(dragonMusic);
		if (prev != dragonMusic) BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.UPDATE_MUSIC, this, this.PLAYERS);
	}

	@Override
	public void setDarkenSky(boolean darkenSky) {
		boolean prev = this.isDarkenSky();
		super.setDarkenSky(darkenSky);
		if (prev != darkenSky) BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.UPDATE_DARK_SKY, this, this.PLAYERS);
	}

	@Override
	public void setVisible(boolean visible) {
		boolean prev = this.isVisible();
		super.setVisible(visible);
		if (prev != visible) BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.UPDATE_VISIBILITY, this, this.PLAYERS);
	}

	@Override
	public void setDisplayName(Text displayName) {
		Text prev = this.getDisplayName();
		super.setDisplayName(displayName);
		if (prev != displayName) BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.UPDATE_NAME, this, this.PLAYERS);
	}

	@Override
	public boolean addSection(BarSection section) {
		BossBarS2CPacket.sendPacket(BarPacketType.CREATE, this, section, this.PLAYERS);
		return super.addSection(section);
	}

	@Override
	public boolean removeSection(BarSection section) {
		BossBarS2CPacket.sendPacket(BarPacketType.REMOVE_BAR, this, section, this.PLAYERS);
		return super.removeSection(section);
	}

	@Override
	public boolean clearSections() {
		BossBarContainerS2CPacket.sendPacket(ServerBarPacketType.CLEAR_BARS, this, this.PLAYERS);
		return super.clearSections();
	}

	public List<ServerPlayerEntity> getPlayers() {
		return List.copyOf( PLAYERS );
	}

	@Override
	public void discard() {
		super.discard();
	}
}