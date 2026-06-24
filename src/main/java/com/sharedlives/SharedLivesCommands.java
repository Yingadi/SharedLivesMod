package com.sharedlives;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.sharedlives.network.SetupPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class SharedLivesCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildTree("sharedlives"));
        dispatcher.register(buildTree("sl"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTree(String root) {
        return Commands.literal(root)
                .executes(SharedLivesCommands::getLives)
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(SharedLivesCommands::addLives)))
                .then(Commands.literal("setup")
                        .requires(source -> source.hasPermission(2))
                        .executes(SharedLivesCommands::openSetup));
    }

    private static int getLives(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        SharedLivesSavedData data = SharedLivesSavedData.get(server);

        if (data.isGameOver()) {
            ctx.getSource().sendSuccess(
                () -> Component.literal("GAME OVER — all shared lives were exhausted.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                false
            );
        } else {
            int lives = data.getLives();
            ctx.getSource().sendSuccess(
                () -> Component.literal("Shared lives remaining: " + lives)
                        .withStyle(ChatFormatting.GOLD),
                false
            );
        }
        return data.getLives();
    }

    private static int addLives(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        MinecraftServer server = ctx.getSource().getServer();
        SharedLivesSavedData data = SharedLivesSavedData.get(server);

        int newLives = data.addLives(amount);
        String verb = amount >= 0 ? "Added" : "Removed";
        int abs = Math.abs(amount);

        server.getPlayerList().broadcastSystemMessage(
            Component.literal(verb + " " + abs + " life" + (abs != 1 ? "s" : "") +
                              " via command. Lives remaining: " + newLives)
                    .withStyle(ChatFormatting.GOLD),
            false
        );

        SharedLivesEvents.updateTabList(server);
        return newLives;
    }

    private static int openSetup(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        SharedLivesSavedData data = SharedLivesSavedData.get(player.server);
        if (data.isConfigured()) {
            ctx.getSource().sendFailure(
                Component.literal("World is already configured. Use /sharedlives add to adjust the count.")
                        .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        PacketDistributor.sendToPlayer(player, new SetupPayload());
        return 1;
    }
}
