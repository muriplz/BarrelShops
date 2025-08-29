package dev.muriplz.barrelshops.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.muriplz.barrelshops.economy.BalanceApi;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class BalanceTopCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendSystemMessage(Component.literal("Can't execute from console"));
            return 0;
        }

        int page = 1;
        try {
            page = IntegerArgumentType.getInteger(context, "page");
        } catch (IllegalArgumentException ignored) {}

        LinkedHashMap<UUID, Integer> topBalances = BalanceApi.getTopBalances(page);

        player.sendSystemMessage(Component.literal("Top Balances - Page " + page).withStyle(ChatFormatting.GOLD));

        int rank = (page - 1) * 10 + 1;
        for (Map.Entry<UUID, Integer> entry : topBalances.entrySet()) {
            String playerName = player.getServer().getProfileCache().get(entry.getKey()).map(profile -> profile.getName()).orElse("Unknown");

            player.sendSystemMessage(Component.literal(rank + ". " + playerName + ": $" + entry.getValue()).withStyle(ChatFormatting.WHITE));
            rank++;
        }

        return Command.SINGLE_SUCCESS;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balancetop")
                .executes(BalanceTopCommand::execute)
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(BalanceTopCommand::execute))
        );

        dispatcher.register(Commands.literal("baltop")
                .executes(BalanceTopCommand::execute)
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(BalanceTopCommand::execute))
        );
    }
}
