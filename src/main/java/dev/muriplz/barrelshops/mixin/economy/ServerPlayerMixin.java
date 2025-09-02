package dev.muriplz.barrelshops.mixin.economy;

import com.mojang.authlib.GameProfile;
import dev.muriplz.barrelshops.MinecraftServerSupplier;
import dev.muriplz.barrelshops.economy.BalanceApi;
import dev.muriplz.barrelshops.economy.shops.ItemStackUtils;
import dev.muriplz.barrelshops.economy.shops.Shop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    private void signEdit(SignBlockEntity be, boolean front, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getLevel().getBlockState(pos);

        if (!(state.getBlock() instanceof WallSignBlock)) return;

        Direction facing = state.getValue(WallSignBlock.FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());

        if (!(be.getLevel().getBlockEntity(attachedPos) instanceof BarrelBlockEntity barrel)) return;

        Optional<Shop> shop = Shop.get(attachedPos, be.getLevel().dimension().location().toString());
        if (shop.isEmpty()) return;

        ci.cancel();

        if (shop.get().owner().equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("You own this shop"));
            return;
        }

        ItemStack templateItem = ItemStackUtils.deserialize(shop.get().selling(), be.getLevel().registryAccess());
        if (templateItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("Shop has invalid item data"));
            return;
        }

        if (shop.get().type().equals("buy")) {
            cwmod$handleBuyInteraction(player, shop.get(), templateItem, barrel);
        } else {
            cwmod$handleSellInteraction(player, shop.get(), templateItem, barrel);
        }
    }

    @Unique
    private void cwmod$handleBuyInteraction(ServerPlayer player, Shop shop, ItemStack templateItem, BarrelBlockEntity barrel) {
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

        player.sendSystemMessage(Component.literal("You bought " + given + " " + templateItem.getDisplayName().getString() +
                " for $" + shop.price() + " from " + owner + "'s shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }

    @Unique
    private void cwmod$handleSellInteraction(ServerPlayer player, Shop shop, ItemStack templateItem, BarrelBlockEntity barrel) {
        int playerItems = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.matches(stack, templateItem)) {
                playerItems += stack.getCount();
            }
        }

        if (playerItems < shop.amount()) {
            player.sendSystemMessage(Component.literal("You don't have enough " + templateItem.getDisplayName().getString() + " to sell"));
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

        player.sendSystemMessage(Component.literal("You sold " + removed + " " + templateItem.getDisplayName().getString() +
                " for $" + shop.price() + " to " + owner + "'s shop. Your new balance is: $" + BalanceApi.getBalance(player.getUUID())));
    }
}