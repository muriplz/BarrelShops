package dev.muriplz.barrelshops.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.muriplz.barrelshops.economy.BalanceApi;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BalanceGiveCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.hasPermission(3)) {
            source.sendSystemMessage(Component.literal("You don't have permission to use this command!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String playerName = StringArgumentType.getString(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (amount <= 0) {
            source.sendSystemMessage(Component.literal("Amount must be positive!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);

        if (targetPlayer == null) {
            source.sendSystemMessage(Component.literal("Player '" + playerName + "' is not online!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        try {
            BalanceApi.giveBalance(targetPlayer.getUUID(), amount);

            source.sendSystemMessage(Component.literal("Successfully gave $" + amount + " to " + targetPlayer.getName().getString())
                    .withStyle(ChatFormatting.GREEN));

            targetPlayer.sendSystemMessage(Component.literal("You received $" + amount + " from an admin!")
                    .withStyle(ChatFormatting.GREEN));

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Error giving balance: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balancegive")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList()
                                    .getPlayers().stream()
                                    .map(player -> player.getName().getString()), builder);
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(BalanceGiveCommand::execute)
                        )
                )
        );
    }
}