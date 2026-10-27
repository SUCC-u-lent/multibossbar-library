package com.multibossbar.networking.container_packet;

import com.multibossbar.MultiBossBarLibrary;
import com.multibossbar.bossbars.MultibarContainer;
import com.multibossbar.networking.bar_packet.BossBarS2CPacket;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.*;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
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
				BossBarS2CPacket.TEXT_UNLIMITED_CODEC.encode(buf, container.getDisplayName());
				buf.writeBoolean(container.isDarkenSky());
				buf.writeBoolean(container.isDragonMusic());
				buf.writeBoolean(container.isThickenFog());
				buf.writeBoolean(container.isVisible());
			}
			case UPDATE_VISIBILITY -> buf.writeBoolean(container.isVisible());
			case UPDATE_NAME -> BossBarS2CPacket.TEXT_UNLIMITED_CODEC.encode(buf, container.getDisplayName());
			case REMOVE, CLEAR_BARS -> {}
			case UPDATE_FOG -> buf.writeBoolean(container.isThickenFog());
			case UPDATE_MUSIC -> buf.writeBoolean(container.isDragonMusic());
			case UPDATE_DARK_SKY -> buf.writeBoolean(container.isDarkenSky());
			default -> throw new UnsupportedOperationException("A case for "+packetType.name()+ " has not been implemented yet.");
		}

		BossBarContainerS2CPacket packet = new BossBarContainerS2CPacket(buf);
		players.forEach(packet::send);
	}


	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public static final Id<BossBarContainerS2CPacket> ID =
		new Id<>(Identifier.of(MultiBossBarLibrary.MOD_ID, "container_s2c"));

	public void send(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, this);
	}
}