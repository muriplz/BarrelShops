package dev.muriplz.barrelshops.economy;

import dev.muriplz.barrelshops.config.ConfigReader;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PassiveIncomeTask {

    private static ScheduledExecutorService scheduler;
    private static MinecraftServer server;

    public static void start(MinecraftServer minecraftServer) {
        server = minecraftServer;
        
        if (ConfigReader.PASSIVE_INCOME_AMOUNT == -1) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            PassiveIncomeTask::givePassiveIncome,
            ConfigReader.PASSIVE_INCOME_INTERVAL_MINUTES,
            ConfigReader.PASSIVE_INCOME_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private static void givePassiveIncome() {
        if (server == null) return;
        
        server.getPlayerList().getPlayers().forEach(player -> 
            BalanceApi.giveBalance(player.getUUID(), ConfigReader.PASSIVE_INCOME_AMOUNT)
        );
    }
}