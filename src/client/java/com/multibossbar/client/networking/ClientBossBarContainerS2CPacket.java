package com.multibossbar.client.networking;

import com.multibossbar.bossbars.MultibarContainer;
import com.multibossbar.networking.bar_packet.BossBarS2CPacket;
import com.multibossbar.networking.container_packet.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class ClientBossBarContainerS2CPacket {
    public static void receive(BossBarContainerS2CPacket packet, ClientPlayNetworking.Context context) {
        PacketByteBuf buf = packet.data();
        ServerBarPacketType packetType = buf.readEnumConstant(ServerBarPacketType.class);
        UUID containerUUID = buf.readUuid();
        switch (packetType) {
            case CREATE -> {
                MultibarContainer container = new MultibarContainer(
                        containerUUID,
                        BossBarS2CPacket.TEXT_UNLIMITED_CODEC.decode(buf),
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
            case UPDATE_NAME -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(c->c.setDisplayName(BossBarS2CPacket.TEXT_UNLIMITED_CODEC.decode(buf)));
            case CLEAR_BARS -> MultibarContainer.getOptionalClientBar(containerUUID).ifPresent(MultibarContainer::clearSections);
            case REMOVE -> MultibarContainer.MULTIBAR_CONTAINERS.remove(containerUUID);
        }
    }
}
