package dev.muriplz.barrelshops.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.muriplz.barrelshops.economy.BalanceApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class BalanceCommand {
    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendSystemMessage(Component.literal("Can't execute from console"));
            return 0;
        }

        int balance = BalanceApi.getBalance(player.getUUID());
        source.sendSystemMessage(Component.literal("Your balance: " + balance));

        return Command.SINGLE_SUCCESS;
    }

    public static int executeWithPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        if (player == null) {
            source.sendSystemMessage(Component.literal("Player not found"));
            return 0;
        }

        int balance = BalanceApi.getBalance(player.getUUID());
        source.sendSystemMessage(Component.literal(player.getName().getString() + "’s balance: " + balance));

        return Command.SINGLE_SUCCESS;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
                .executes(BalanceCommand::execute)
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest online players
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList()
                                    .getPlayers().stream()
                                    .map(player -> player.getName().getString()), builder);
                        })
                        .executes(ctx -> {
                            String playerName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                            return executeWithPlayer(ctx, player);
                        }))
        );

        dispatcher.register(Commands.literal("bal")
                .executes(BalanceCommand::execute)
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest online players
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList()
                                    .getPlayers().stream()
                                    .map(player -> player.getName().getString()), builder);
                        })
                        .executes(ctx -> {
                            String playerName = StringArgumentType.getString(ctx, "player");
                            ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                            return executeWithPlayer(ctx, player);
                        }))
        );
    }
}
