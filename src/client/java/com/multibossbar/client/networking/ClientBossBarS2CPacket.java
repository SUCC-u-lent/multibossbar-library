package com.multibossbar.client.networking;

import com.multibossbar.MultiBossBarLibrary;
import com.multibossbar.bossbars.*;
import com.multibossbar.networking.bar_packet.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.TextCodecs;

import java.util.*;

public class ClientBossBarS2CPacket {
    public static void receive(BossBarS2CPacket payload, ClientPlayNetworking.Context context) {
        PacketByteBuf buf = payload.data();
        BarPacketType packetType = buf.readEnumConstant(BarPacketType.class);
        UUID containerUUID = buf.readUuid();
        UUID sectionUUID = buf.readUuid();

        MultibarContainer container = MultibarContainer.getOptionalClientBar(containerUUID).orElse(null);
        if (container == null) {
            MultiBossBarLibrary.LOGGER.warn("Failed to acquire container with uuid: {}",containerUUID);
            return;
        }
        BarSection section = container.getOptionalSection(sectionUUID).orElse(null);

        switch (packetType) {
            case UPDATE_NAME -> {
                Objects.requireNonNull(section);
                section.setName(BossBarS2CPacket.TEXT_UNLIMITED_CODEC.decode(buf));
            }
            case UPDATE_PERCENTAGE -> {
                Objects.requireNonNull(section);
                section.setPercentage(buf.readDouble());
            }
            case UPDATE_COLOR -> {
                Objects.requireNonNull(section);
                section.setColor(buf.readEnumConstant(BossBar.Color.class));
            }
            case UPDATE_STYLE -> {
                Objects.requireNonNull(section);
                section.setStyle(buf.readEnumConstant(BossBar.Style.class));
            }
            case UPDATE_SCALE -> {
                Objects.requireNonNull(section);
                section.setVerticalScale(buf.readFloat());
            }
            case REMOVE_BAR -> {
                if (section == null) return;
                container.removeSection(section);
            }
            case UPDATE_VISIBILITY -> {
                Objects.requireNonNull(section);
                section.setVisible(buf.readBoolean());
            }
            case UPDATE_TYPE -> {
                Objects.requireNonNull(section);
                section.setType(buf.readEnumConstant(RowType.class));
            }
            case CREATE -> section = MultibarContainer.createSection(
                    container,
                    sectionUUID,
                    BossBarS2CPacket.TEXT_UNLIMITED_CODEC.decode(buf),
                    buf.readDouble(),
                    buf.readEnumConstant(BossBar.Color.class),
                    buf.readEnumConstant(BossBar.Style.class),
                    buf.readEnumConstant(RowType.class),
                    buf.readFloat(),
                    buf.readBoolean()
            );
        }
    }
}
