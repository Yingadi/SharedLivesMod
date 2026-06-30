package com.sharedlives;

import com.sharedlives.network.SetupPayload;
import com.sharedlives.network.SetupResponsePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = SharedLivesMod.MOD_ID)
public class SharedLivesEvents {

    // Players who must be set back to SURVIVAL on the next server tick.
    // We can't do it inside PlayerRespawnEvent because vanilla assigns SPECTATOR
    // (for hardcore) AFTER that event fires.
    private static final Set<UUID> pendingSurvivalRespawns = new HashSet<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SharedLivesCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SharedLivesConfig.ENABLED.get()) return;
        if (!player.server.isHardcore()) return;

        SharedLivesSavedData data = SharedLivesSavedData.get(player.server);
        if (!data.isConfigured() || !data.isSetupEnabled()) return;
        if (data.isGameOver()) return;

        int remaining = data.decrementAndGet();
        MinecraftServer server = player.server;
        String playerName = player.getName().getString();

        if (remaining > 0) {
            String livesLabel = remaining == 1 ? "LIFE LEFT" : "LIVES LEFT";
            broadcastTitle(server,
                    Component.literal("☠  " + remaining + "  " + livesLabel)
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(playerName + " died")
                            .withStyle(ChatFormatting.DARK_RED),
                    70
            );
            broadcastSound(server, SoundEvents.WITHER_SPAWN, 1.0f, 1.0f);
        } else {
            broadcastTitle(server,
                    Component.literal("GAME OVER")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    Component.literal("☠  " + playerName + " used the last life  ☠")
                            .withStyle(ChatFormatting.RED),
                    120
            );
            broadcastSound(server, SoundEvents.WITHER_DEATH, 1.0f, 1.0f);
            DamageSource gameOverSource = buildGameOverDamageSource(server);
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (!other.getUUID().equals(player.getUUID()) && other.isAlive()) {
                    other.hurt(gameOverSource, Float.MAX_VALUE);
                }
            }
        }

        updateTabList(server);
    }

    // Queues a SURVIVAL restore for the next tick.
    // Vanilla sets SPECTATOR after this event fires in hardcore, so we defer.
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SharedLivesConfig.ENABLED.get()) return;
        if (!player.server.isHardcore()) return;
        if (event.isEndConquered()) return;

        SharedLivesSavedData data = SharedLivesSavedData.get(player.server);
        if (!data.isConfigured() || !data.isSetupEnabled()) return;
        if (!data.isGameOver()) {
            pendingSurvivalRespawns.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (pendingSurvivalRespawns.isEmpty()) return;

        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (pendingSurvivalRespawns.remove(player.getUUID())) {
                player.setGameMode(GameType.SURVIVAL);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.server.isHardcore()) return;

        SharedLivesSavedData data = SharedLivesSavedData.get(player.server);

        if (!data.isConfigured()) {
            // Singleplayer/LAN: integrated server → always show to host (cheats-off worlds have no OP level).
            // Dedicated server: require OP so random players can't configure.
            boolean canSetup = !player.server.isDedicatedServer() || player.hasPermissions(2);
            if (canSetup) {
                PacketDistributor.sendToPlayer(player, new SetupPayload());
            } else {
                player.sendSystemMessage(
                        Component.literal("[Shared Lives] Waiting for an OP to configure the mod...")
                                .withStyle(ChatFormatting.YELLOW)
                );
            }
            updateTabList(player.server);
            return;
        }

        if (!SharedLivesConfig.ENABLED.get() || !data.isSetupEnabled()) return;

        if (data.isGameOver()) {
            player.sendSystemMessage(
                    Component.literal("[Shared Lives] Game Over — all lives were exhausted.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
            );
        } else {
            player.sendSystemMessage(
                    Component.literal("[Shared Lives] Lives remaining: " + data.getLives())
                            .withStyle(ChatFormatting.GOLD)
            );
        }

        updateTabList(player.server);
    }

    // Called from SharedLivesMod payload handler on the server thread.
    public static void handleSetupResponse(SetupResponsePayload payload, ServerPlayer player) {
        SharedLivesSavedData data = SharedLivesSavedData.get(player.server);
        if (data.isConfigured()) return;

        data.configure(payload.enabled(), payload.lives());
        MinecraftServer server = player.server;

        if (payload.enabled()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[Shared Lives] ♥ Setup complete! Starting with ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(payload.lives() + " shared lives")
                                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                            .append(Component.literal(".").withStyle(ChatFormatting.GOLD)),
                    false
            );
        } else {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[Shared Lives] Mod is disabled for this world.")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
        }

        updateTabList(server);

        if (SharedLivesConfig.SHOW_PLAYER_HEARTS.get()) {
            var src = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(src, "scoreboard objectives add h health");
            server.getCommands().performPrefixedCommand(src, "scoreboard objectives setdisplay list h");
        }
    }

    private static void broadcastSound(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSoundPacket(
                    Holder.direct(sound), SoundSource.MASTER,
                    p.getX(), p.getY(), p.getZ(),
                    volume, pitch, 0L
            ));
        }
    }

    private static void broadcastTitle(MinecraftServer server, Component title, Component subtitle, int stay) {
        ClientboundSetTitlesAnimationPacket timing = new ClientboundSetTitlesAnimationPacket(10, stay, 20);
        ClientboundSetTitleTextPacket titlePkt = new ClientboundSetTitleTextPacket(title);
        ClientboundSetSubtitleTextPacket subtitlePkt = new ClientboundSetSubtitleTextPacket(subtitle);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(timing);
            player.connection.send(titlePkt);
            player.connection.send(subtitlePkt);
        }
    }

    private static DamageSource buildGameOverDamageSource(MinecraftServer server) {
        ResourceKey<DamageType> key = ResourceKey.create(
                Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(SharedLivesMod.MOD_ID, "game_over")
        );
        Holder<DamageType> holder = server.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder);
    }

    public static void updateTabList(MinecraftServer server) {
        SharedLivesSavedData data = SharedLivesSavedData.get(server);

        Component footer;
        if (!data.isConfigured()) {
            footer = Component.literal("♥  Shared Lives — Setup pending...")
                    .withStyle(ChatFormatting.YELLOW);
        } else if (!data.isSetupEnabled() || !SharedLivesConfig.ENABLED.get()) {
            return;
        } else if (data.isGameOver()) {
            footer = Component.literal("☠  GAME OVER  ☠")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        } else {
            footer = Component.literal("♥  Shared Lives: ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(String.valueOf(data.getLives()))
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        Component header = Component.empty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundTabListPacket(header, footer));
        }
    }
}
