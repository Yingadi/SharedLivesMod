package com.sharedlives.mixin;

import com.sharedlives.SharedLivesConfig;
import com.sharedlives.SharedLivesSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    // Vanilla forces SPECTATOR during respawnPlayer() for hardcore worlds.
    // We inject at the very end (RETURN), after all vanilla code has run,
    // and restore SURVIVAL when the shared pool still has lives left.
    @Inject(
        method = "respawnPlayer(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("RETURN")
    )
    private void sharedlives$fixHardcoreRespawn(
            ServerPlayer oldPlayer, boolean endConquered,
            CallbackInfoReturnable<ServerPlayer> cir) {

        ServerPlayer newPlayer = cir.getReturnValue();
        if (newPlayer == null) return;
        if (!SharedLivesConfig.ENABLED.get()) return;
        if (!newPlayer.server.isHardcore()) return;
        if (endConquered) return;

        SharedLivesSavedData data = SharedLivesSavedData.get(newPlayer.server);
        if (!data.isGameOver()) {
            newPlayer.setGameMode(GameType.SURVIVAL);
        }
    }
}
