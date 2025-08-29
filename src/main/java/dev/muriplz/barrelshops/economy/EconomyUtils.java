package dev.muriplz.barrelshops.economy;

import net.minecraft.server.level.ServerPlayer;

public class EconomyUtils {

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean playerHas(ServerPlayer player, int cost) {
        int balance = BalanceApi.getBalance(player.getUUID());
        
        return balance >= cost;
    }

    public static void charge(ServerPlayer player, int cost) {
        BalanceApi.giveBalance(player.getUUID(), -cost);
    }
}
