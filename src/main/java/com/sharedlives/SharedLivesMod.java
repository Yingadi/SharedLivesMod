package com.sharedlives;

import com.sharedlives.network.SetupPayload;
import com.sharedlives.network.SetupResponsePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(SharedLivesMod.MOD_ID)
public class SharedLivesMod {
    public static final String MOD_ID = "sharedlives";

    public SharedLivesMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SharedLivesConfig.SPEC);
        modEventBus.addListener(SharedLivesMod::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Server → Client: tell the client to open the setup screen
        registrar.playToClient(
                SetupPayload.TYPE,
                SetupPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.minecraft.client.Minecraft.getInstance()
                                .setScreen(new com.sharedlives.client.SharedLivesSetupScreen()))
        );

        // Client → Server: receive the player's choices
        registrar.playToServer(
                SetupResponsePayload.TYPE,
                SetupResponsePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        SharedLivesEvents.handleSetupResponse(payload, (ServerPlayer) ctx.player()))
        );
    }
}
