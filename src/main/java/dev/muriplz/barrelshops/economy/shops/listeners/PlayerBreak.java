package dev.muriplz.barrelshops.economy.shops.listeners;

import dev.muriplz.barrelshops.BarrelShops;
import dev.muriplz.barrelshops.economy.shops.AdminShop;
import dev.muriplz.barrelshops.economy.shops.AdminShopApi;
import dev.muriplz.barrelshops.economy.shops.Shop;
import dev.muriplz.barrelshops.economy.shops.ShopApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

@EventBusSubscriber(modid = BarrelShops.MODID)
public class PlayerBreak {

    @SubscribeEvent
    public static void onPlayerBreak(BlockEvent.BreakEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        Level level = player.level();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        String dim = level.dimension().location().toString();
        String uuid = player.getUUID().toString();

        BlockPos shopPos;
        if (state.getBlock() == Blocks.BARREL) {
            shopPos = pos;
        } else if (state.is(BlockTags.SIGNS) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            shopPos = pos.relative(dir.getOpposite());
        } else {
            return;
        }

        // Check for regular shop
        Optional<Shop> shop = Shop.get(shopPos, dim);
        if (shop.isPresent()) {
            if (!shop.get().owner().equals(uuid)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("You do not own this shop"));
            } else {
                ShopApi.delete(shop.get().id());
            }
            return;
        }

        // Check for admin shop
        Optional<AdminShop> adminShop = AdminShop.getAdminShop(shopPos, dim);
        if (adminShop.isPresent()) {
            if (!player.hasPermissions(3)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("You don't have permission to break admin shops"));
            } else {
                AdminShopApi.delete(adminShop.get().id());
                player.sendSystemMessage(Component.literal("Admin shop deleted"));
            }
        }
    }
}
