package com.multibossbar.client.networking;

import com.multibossbar.networking.bar_packet.BossBarS2CPacket;
import com.multibossbar.networking.container_packet.BossBarContainerS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;

public class ModNetworking {
    public static void Register() {
        PayloadTypeRegistry.playS2C().register(BossBarS2CPacket.ID, BossBarS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(BossBarContainerS2CPacket.ID, BossBarContainerS2CPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(BossBarS2CPacket.ID, ClientBossBarS2CPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(BossBarContainerS2CPacket.ID, ClientBossBarContainerS2CPacket::receive);
    }
}
