package dev.muriplz.barrelshops.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.muriplz.barrelshops.economy.BalanceApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PayCommand {

    public static int execute(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();

        if (player == null) {
            source.sendSystemMessage(Component.literal("Player not found"));
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (BalanceApi.Payment.pay(source.getPlayer().getUUID(), player.getUUID(), amount)) {
            source.sendSystemMessage(Component.literal("Successfully paid " + player.getName().getString() + " $" + amount));
            player.sendSystemMessage(Component.literal("You received $" + amount + " from " + source.getPlayer().getName().getString()));
        } else {
            source.sendSystemMessage(Component.literal("Payment failed. Check your balance or try again later."));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest online players
                            return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList()
                                    .getPlayers().stream()
                                    .map(player -> player.getName().getString()), builder);
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                                    return execute(ctx, player);
                                })))
        );
    }
}
