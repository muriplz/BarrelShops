package dev.muriplz.barrelshops;

import com.mojang.brigadier.CommandDispatcher;
import dev.muriplz.barrelshops.commands.*;
import dev.muriplz.barrelshops.config.ConfigReader;
import dev.muriplz.barrelshops.economy.BalanceApi;
import dev.muriplz.barrelshops.storage.Database;
import dev.muriplz.barrelshops.storage.DatabaseUtils;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.io.IOException;
import java.nio.file.Path;

@Mod(BarrelShops.MODID)
public class BarrelShops {
    public static final String MODID = "barrelshops";

    public BarrelShops() {
        NeoForge.EVENT_BUS.register(this);

        try {
            ConfigReader.readFile(Path.of("config/" + MODID));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DatabaseUtils.createTables();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getServer().getCommands().getDispatcher();

        BalanceCommand.register(dispatcher);
        BalanceGiveCommand.register(dispatcher);
        BalanceTopCommand.register(dispatcher);
        PayCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        Database.closeDataSource();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        BalanceApi.createBalance(event.getEntity().getUUID());
    }
}
