package dev.muriplz.barrelshops.economy.shops.listeners;

import dev.muriplz.barrelshops.BarrelShops;
import dev.muriplz.barrelshops.economy.shops.AdminShop;
import dev.muriplz.barrelshops.economy.shops.Shop;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

@EventBusSubscriber(modid = BarrelShops.MODID)
public class PlayerPlace {

    @SubscribeEvent
    public static void onPlayerPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getState().is(BlockTags.SIGNS)) return;
        if (!event.getState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;

        BlockPos signPos = event.getPos();
        BlockPos barrelPos = signPos.relative(event.getState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());

        ServerPlayer player = (ServerPlayer) event.getEntity();
        String dim = player.level().dimension().location().toString();

        // Check for regular shop
        Optional<Shop> shop = Shop.get(barrelPos, dim);
        if (shop.isPresent()) {
            if (!shop.get().owner().equals(player.getUUID().toString())) {
                event.setCanceled(true);
            }
            return;
        }

        // Check for admin shop
        Optional<AdminShop> adminShop = AdminShop.getAdminShop(barrelPos, dim);
        if (adminShop.isPresent()) {
            if (!player.hasPermissions(3)) {
                event.setCanceled(true);
            }
        }
    }
}