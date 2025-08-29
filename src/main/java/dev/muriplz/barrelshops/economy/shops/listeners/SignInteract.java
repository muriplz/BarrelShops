package dev.muriplz.barrelshops.economy.shops.listeners;

import com.mojang.authlib.GameProfile;
import dev.muriplz.barrelshops.BarrelShops;
import dev.muriplz.barrelshops.MinecraftServerSupplier;
import dev.muriplz.barrelshops.economy.BalanceApi;
import dev.muriplz.barrelshops.economy.shops.AdminShop;
import dev.muriplz.barrelshops.economy.shops.Shop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = BarrelShops.MODID)
public class SignInteract {

    private static final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 200;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof WallSignBlock)) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        Long lastClick = clickCooldowns.get(playerId);
        if (lastClick != null && currentTime - lastClick < COOLDOWN_MS) {
            return;
        }
        clickCooldowns.put(playerId, currentTime);

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Level level = event.getLevel();

        Direction facing = state.getValue(WallSignBlock.FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());

        if (!(level.getBlockEntity(attachedPos) instanceof BarrelBlockEntity barrel)) return;

        // Check for regular shop first
        Optional<Shop> shop = Shop.get(attachedPos, level.dimension().location().toString());
        if (shop.isPresent()) {
            event.setCanceled(true);

            if (shop.get().owner().equals(player.getUUID().toString())) {
                player.sendSystemMessage(Component.literal("You own this shop"));
                return;
            }

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(shop.get().selling()));

            if (shop.get().type().equals("buy")) {
                handleBuyShop(player, shop.get(), item, barrel);
            } else {
                handleSellShop(player, shop.get(), item, barrel);
            }
            return;
        }

        // Check for admin shop
        Optional<AdminShop> adminShop = AdminShop.getAdminShop(attachedPos, level.dimension().location().toString());
        if (adminShop.isPresent()) {
            event.setCanceled(true);

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(adminShop.get().selling()));

            if (adminShop.get().type().equals("buy")) {
                handleAdminBuyShop(player, adminShop.get(), item);
            } else {
                handleAdminSellShop(player, adminShop.get(), item);
            }
        }

        if (clickCooldowns.size() > 50) {
            clickCooldowns.entrySet().removeIf(entry ->
                    currentTime - entry.getValue() > 1000);
        }
    }

    private static void handleAdminBuyShop(ServerPlayer player, AdminShop shop, Item item) {
        int balance = BalanceApi.getBalance(player.getUUID());

        if (balance < shop.price()) {
            player.sendSystemMessage(Component.literal("You do not have enough balance to buy this item"));
            return;
        }

        // Check inventory space
        if (!player.getInventory().hasAnyMatching(itemStack -> itemStack.isEmpty() ||
                (itemStack.getItem() == item && itemStack.getCount() + shop.amount() <= itemStack.getMaxStackSize()))) {
            player.sendSystemMessage(Component.literal("Your inventory is full"));
            return;
        }

        // Admin shop has unlimited stock, just deduct money and give items
        BalanceApi.giveBalance(player.getUUID(), -shop.price());
        player.addItem(new ItemStack(item, shop.amount()));

        String itemName = item.getDefaultInstance().getDisplayName().getString().replace("[", "").replace("]", "");

        player.sendSystemMessage(Component.literal("You bought " + shop.amount() + " " + itemName +
                " for $" + shop.price() + " from the admin shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleAdminSellShop(ServerPlayer player, AdminShop shop, Item item) {
        int playerItems = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                playerItems += stack.getCount();
            }
        }

        String itemName = item.getDefaultInstance().getDisplayName().getString().replace("[", "").replace("]", "");

        if (playerItems < shop.amount()) {
            player.sendSystemMessage(Component.literal("You don't have enough " + itemName + " to sell"));
            return;
        }

        // Admin shop has unlimited money, just take items and give money
        BalanceApi.giveBalance(player.getUUID(), shop.price());

        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && removed < shop.amount(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), shop.amount() - removed);
                stack.shrink(toRemove);
                removed += toRemove;
            }
        }

        player.sendSystemMessage(Component.literal("You sold " + removed + " " + itemName +
                " for $" + shop.price() + " to the admin shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleBuyShop(ServerPlayer player, Shop shop, Item item, BarrelBlockEntity barrel) {
        int balance = BalanceApi.getBalance(player.getUUID());

        if (balance < shop.price()) {
            player.sendSystemMessage(Component.literal("You do not have enough balance to buy this item"));
            return;
        }

        int stock = 0;
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack itemStack = barrel.getItem(i);
            if (itemStack.getItem() == item) {
                stock += itemStack.getCount();
            }
        }

        if (stock < shop.amount()) {
            player.sendSystemMessage(Component.literal("This shop does not have enough stock to fulfill your order"));
            return;
        }

        if (!player.getInventory().hasAnyMatching(itemStack -> itemStack.isEmpty() ||
                (itemStack.getItem() == item && itemStack.getCount() + shop.amount() <= itemStack.getMaxStackSize()))) {
            player.sendSystemMessage(Component.literal("Your inventory is full"));
            return;
        }

        BalanceApi.giveBalance(player.getUUID(), -shop.price());
        BalanceApi.giveBalance(UUID.fromString(shop.owner()), shop.price());

        int given = 0;
        for (int i = 0; i < barrel.getContainerSize() && given < shop.amount(); i++) {
            ItemStack itemStack = barrel.getItem(i);
            if (itemStack.getItem() == item) {
                int toGive = Math.min(itemStack.getCount(), shop.amount() - given);
                itemStack.shrink(toGive);
                given += toGive;
                player.addItem(new ItemStack(item, toGive));
            }
        }

        String owner = MinecraftServerSupplier.getServer().getProfileCache()
                .get(UUID.fromString(shop.owner()))
                .map(GameProfile::getName)
                .orElse("Unknown Player");

        String itemName = item.getDefaultInstance().getDisplayName().getString().replace("[", "").replace("]", "");

        player.sendSystemMessage(Component.literal("You bought " + given + " " + itemName +
                " for $" + shop.price() + " from " + owner + "'s shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleSellShop(ServerPlayer player, Shop shop, Item item, BarrelBlockEntity barrel) {
        int playerItems = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                playerItems += stack.getCount();
            }
        }

        String itemName = item.getDefaultInstance().getDisplayName().getString().replace("[", "").replace("]", "");

        if (playerItems < shop.amount()) {
            player.sendSystemMessage(Component.literal("You don't have enough " + itemName + " to sell"));
            return;
        }

        int ownerBalance = BalanceApi.getBalance(UUID.fromString(shop.owner()));
        if (ownerBalance < shop.price()) {
            player.sendSystemMessage(Component.literal("The shop owner doesn't have enough money to buy your items"));
            return;
        }

        int space = 0;
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack stack = barrel.getItem(i);
            if (stack.isEmpty()) {
                space += 64;
            } else if (stack.getItem() == item) {
                space += (64 - stack.getCount());
            }
        }

        if (space < shop.amount()) {
            player.sendSystemMessage(Component.literal("The shop barrel doesn't have enough space"));
            return;
        }

        BalanceApi.giveBalance(player.getUUID(), shop.price());
        BalanceApi.giveBalance(UUID.fromString(shop.owner()), -shop.price());

        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && removed < shop.amount(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), shop.amount() - removed);
                stack.shrink(toRemove);
                removed += toRemove;
            }
        }

        int toAdd = removed;
        for (int i = 0; i < barrel.getContainerSize() && toAdd > 0; i++) {
            ItemStack stack = barrel.getItem(i);
            if (stack.isEmpty()) {
                int amount = Math.min(toAdd, 64);
                barrel.setItem(i, new ItemStack(item, amount));
                toAdd -= amount;
            } else if (stack.getItem() == item && stack.getCount() < 64) {
                int amount = Math.min(toAdd, 64 - stack.getCount());
                stack.grow(amount);
                toAdd -= amount;
            }
        }

        String owner = MinecraftServerSupplier.getServer().getProfileCache()
                .get(UUID.fromString(shop.owner()))
                .map(GameProfile::getName)
                .orElse("Unknown Player");

        player.sendSystemMessage(Component.literal("You sold " + removed + " " + itemName +
                " for $" + shop.price() + " to " + owner + "'s shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }
}