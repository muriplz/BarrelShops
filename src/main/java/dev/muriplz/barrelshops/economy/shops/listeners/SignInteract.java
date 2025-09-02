package dev.muriplz.barrelshops.economy.shops.listeners;

import com.mojang.authlib.GameProfile;
import dev.muriplz.barrelshops.BarrelShops;
import dev.muriplz.barrelshops.MinecraftServerSupplier;
import dev.muriplz.barrelshops.economy.BalanceApi;
import dev.muriplz.barrelshops.economy.shops.AdminShop;
import dev.muriplz.barrelshops.economy.shops.ItemStackUtils;
import dev.muriplz.barrelshops.economy.shops.Shop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

        Optional<Shop> shop = Shop.get(attachedPos, level.dimension().location().toString());
        if (shop.isPresent()) {
            event.setCanceled(true);

            if (shop.get().owner().equals(player.getUUID().toString())) {
                player.sendSystemMessage(Component.literal("You own this shop"));
                return;
            }

            ItemStack templateItem = ItemStackUtils.deserialize(shop.get().selling(), level.registryAccess());
            if (templateItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("Shop has invalid item data"));
                return;
            }

            if (shop.get().type().equals("buy")) {
                handleBuyShop(player, shop.get(), templateItem, barrel);
            } else {
                handleSellShop(player, shop.get(), templateItem, barrel);
            }
            return;
        }

        Optional<AdminShop> adminShop = AdminShop.getAdminShop(attachedPos, level.dimension().location().toString());
        if (adminShop.isPresent()) {
            event.setCanceled(true);

            ItemStack templateItem = ItemStackUtils.deserialize(adminShop.get().selling(), level.registryAccess());
            if (templateItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("Admin shop has invalid item data"));
                return;
            }

            if (adminShop.get().type().equals("buy")) {
                handleAdminBuyShop(player, adminShop.get(), templateItem);
            } else {
                handleAdminSellShop(player, adminShop.get(), templateItem);
            }
        }

        if (clickCooldowns.size() > 50) {
            clickCooldowns.entrySet().removeIf(entry ->
                    currentTime - entry.getValue() > 1000);
        }
    }

    private static void handleAdminBuyShop(ServerPlayer player, AdminShop shop, ItemStack templateItem) {
        int balance = BalanceApi.getBalance(player.getUUID());

        if (balance < shop.price()) {
            player.sendSystemMessage(Component.literal("You do not have enough balance to buy this item"));
            return;
        }

        if (!player.getInventory().hasAnyMatching(itemStack -> itemStack.isEmpty() ||
                (ItemStack.matches(itemStack, templateItem) && itemStack.getCount() + shop.amount() <= itemStack.getMaxStackSize()))) {
            player.sendSystemMessage(Component.literal("Your inventory is full"));
            return;
        }

        BalanceApi.giveBalance(player.getUUID(), -shop.price());

        ItemStack itemToGive = templateItem.copy();
        itemToGive.setCount(shop.amount());
        player.addItem(itemToGive);

        String itemName = templateItem.getDisplayName().getString().replace("[", "").replace("]", "");

        player.sendSystemMessage(Component.literal("You bought " + shop.amount() + " " + itemName +
                " for $" + shop.price() + " from the admin shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleAdminSellShop(ServerPlayer player, AdminShop shop, ItemStack templateItem) {
        int playerItems = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.matches(stack, templateItem)) {
                playerItems += stack.getCount();
            }
        }

        String itemName = templateItem.getDisplayName().getString().replace("[", "").replace("]", "");

        if (playerItems < shop.amount()) {
            player.sendSystemMessage(Component.literal("You don't have enough " + itemName + " to sell"));
            return;
        }

        BalanceApi.giveBalance(player.getUUID(), shop.price());

        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && removed < shop.amount(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.matches(stack, templateItem)) {
                int toRemove = Math.min(stack.getCount(), shop.amount() - removed);
                stack.shrink(toRemove);
                removed += toRemove;
            }
        }

        player.sendSystemMessage(Component.literal("You sold " + removed + " " + itemName +
                " for $" + shop.price() + " to the admin shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleBuyShop(ServerPlayer player, Shop shop, ItemStack templateItem, BarrelBlockEntity barrel) {
        int balance = BalanceApi.getBalance(player.getUUID());

        if (balance < shop.price()) {
            player.sendSystemMessage(Component.literal("You do not have enough balance to buy this item"));
            return;
        }

        int stock = 0;
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack itemStack = barrel.getItem(i);
            if (ItemStack.matches(itemStack, templateItem)) {
                stock += itemStack.getCount();
            }
        }

        if (stock < shop.amount()) {
            player.sendSystemMessage(Component.literal("This shop does not have enough stock to fulfill your order"));
            return;
        }

        if (!player.getInventory().hasAnyMatching(itemStack -> itemStack.isEmpty() ||
                (ItemStack.matches(itemStack, templateItem) && itemStack.getCount() + shop.amount() <= itemStack.getMaxStackSize()))) {
            player.sendSystemMessage(Component.literal("Your inventory is full"));
            return;
        }

        BalanceApi.giveBalance(player.getUUID(), -shop.price());
        BalanceApi.giveBalance(UUID.fromString(shop.owner()), shop.price());

        int given = 0;
        for (int i = 0; i < barrel.getContainerSize() && given < shop.amount(); i++) {
            ItemStack itemStack = barrel.getItem(i);
            if (ItemStack.matches(itemStack, templateItem)) {
                int toGive = Math.min(itemStack.getCount(), shop.amount() - given);
                itemStack.shrink(toGive);
                given += toGive;

                ItemStack giveStack = templateItem.copy();
                giveStack.setCount(toGive);
                player.addItem(giveStack);
            }
        }

        String owner = MinecraftServerSupplier.getServer().getProfileCache()
                .get(UUID.fromString(shop.owner()))
                .map(GameProfile::getName)
                .orElse("Unknown Player");

        String itemName = templateItem.getDisplayName().getString().replace("[", "").replace("]", "");

        player.sendSystemMessage(Component.literal("You bought " + given + " " + itemName +
                " for $" + shop.price() + " from " + owner + "'s shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    private static void handleSellShop(ServerPlayer player, Shop shop, ItemStack templateItem, BarrelBlockEntity barrel) {
        int playerItems = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.matches(stack, templateItem)) {
                playerItems += stack.getCount();
            }
        }

        String itemName = templateItem.getDisplayName().getString().replace("[", "").replace("]", "");

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
                space += templateItem.getMaxStackSize();
            } else if (ItemStack.matches(stack, templateItem)) {
                space += (templateItem.getMaxStackSize() - stack.getCount());
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
            if (ItemStack.matches(stack, templateItem)) {
                int toRemove = Math.min(stack.getCount(), shop.amount() - removed);
                stack.shrink(toRemove);
                removed += toRemove;
            }
        }

        int toAdd = removed;
        for (int i = 0; i < barrel.getContainerSize() && toAdd > 0; i++) {
            ItemStack stack = barrel.getItem(i);
            if (stack.isEmpty()) {
                int amount = Math.min(toAdd, templateItem.getMaxStackSize());
                ItemStack addStack = templateItem.copy();
                addStack.setCount(amount);
                barrel.setItem(i, addStack);
                toAdd -= amount;
            } else if (ItemStack.matches(stack, templateItem) && stack.getCount() < templateItem.getMaxStackSize()) {
                int amount = Math.min(toAdd, templateItem.getMaxStackSize() - stack.getCount());
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