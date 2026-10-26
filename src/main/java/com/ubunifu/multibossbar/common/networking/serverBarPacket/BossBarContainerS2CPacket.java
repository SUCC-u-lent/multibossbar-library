package com.ubunifu.multibossbar.common.networking.serverBarPacket;

import com.ubunifu.multibossbar.common.MultibossbarMod;
import com.ubunifu.multibossbar.common.boss_bar.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.payload.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record BossBarContainerS2CPacket(@NotNull PacketByteBuf data) implements CustomPayload {

	public static final PacketCodec<? super PacketByteBuf, BossBarContainerS2CPacket> CODEC =
		new PacketCodec<>() {
			@Override
			public void encode(PacketByteBuf buf, BossBarContainerS2CPacket value) {
				buf.writeBytes(value.data());
			}

			@Override
			public BossBarContainerS2CPacket decode(PacketByteBuf buf) {
				return new BossBarContainerS2CPacket(
					new PacketByteBuf(buf.readBytes(buf.readableBytes()))
				);
			}
		};

	public static void sendPacket(ServerBarPacketType packetType, MultibarContainer container, List<ServerPlayerEntity> players) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(packetType);
		buf.writeUuid(container.getUuid());
		switch (packetType) {
			case CREATE -> {
				TextCodecs.UNLIMITED_TEXT_PACKET_CODEC.encode(buf, container.getDisplayName());
				buf.writeBoolean(container.isDarkenSky());
				buf.writeBoolean(container.isDragonMusic());
				buf.writeBoolean(container.isThickenFog());
				buf.writeBoolean(container.isVisible());
			}
			case UPDATE_VISIBILITY -> buf.writeBoolean(container.isVisible());
			case UPDATE_NAME -> TextCodecs.UNLIMITED_TEXT_PACKET_CODEC.encode(buf, container.getDisplayName());
			case REMOVE, CLEAR_BARS -> {}
			case UPDATE_FOG -> buf.writeBoolean(container.isThickenFog());
			case UPDATE_MUSIC -> buf.writeBoolean(container.isDragonMusic());
			case UPDATE_DARK_SKY -> buf.writeBoolean(container.isDarkenSky());
			default -> throw new UnsupportedOperationException("A case for "+packetType.name()+ " has not been implemented yet.");
		}

		BossBarContainerS2CPacket packet = new BossBarContainerS2CPacket(buf);
		players.forEach(packet::send);
	}

	public void receive(ClientPlayNetworking.Context context) {
		PacketByteBuf buf = this.data();
		ServerBarPacketType packetType = buf.readEnumConstant(ServerBarPacketType.class);
		UUID containerUUID = buf.readUuid();
		switch (packetType) {
			case CREATE -> {
				MultibarContainer container = new MultibarContainer(
					containerUUID,
					TextCodecs.UNLIMITED_TEXT_PACKET_CODEC.decode(buf),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readBoolean(),
					buf.readBoolean()
				);
				MultibarContainer.MULTIBAR_CONTAINERS.put(containerUUID, container);
			}
			case UPDATE_FOG -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setThickenFog(buf.readBoolean()));
			case UPDATE_MUSIC -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setDragonMusic(buf.readBoolean()));
			case UPDATE_DARK_SKY -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setDarkenSky(buf.readBoolean()));
			case UPDATE_VISIBILITY -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setVisible(buf.readBoolean()));
			case UPDATE_NAME -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setDisplayName(TextCodecs.UNLIMITED_TEXT_PACKET_CODEC.decode(buf)));
			case CLEAR_BARS -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(MultibarContainer::clearSections);
			case REMOVE -> MultibarContainer.MULTIBAR_CONTAINERS.remove(containerUUID);
		}
	}


	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static final Id<BossBarContainerS2CPacket> ID =
		new Id<>(new Identifier(MultibossbarMod.MOD_ID, "container_s2c"));

	public void send(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, this);
	}
}
