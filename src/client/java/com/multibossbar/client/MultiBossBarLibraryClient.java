package com.multibossbar.client;

import com.multibossbar.client.networking.ModNetworking;
import net.fabricmc.api.ClientModInitializer;

public class MultiBossBarLibraryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetworking.Register();
    }
}
