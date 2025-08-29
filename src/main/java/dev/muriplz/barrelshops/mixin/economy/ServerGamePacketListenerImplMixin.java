package dev.muriplz.barrelshops.mixin.economy;

import dev.muriplz.barrelshops.economy.EconomyUtils;
import dev.muriplz.barrelshops.economy.shops.AdminShop;
import dev.muriplz.barrelshops.economy.shops.AdminShopApi;
import dev.muriplz.barrelshops.economy.shops.Shop;
import dev.muriplz.barrelshops.economy.shops.ShopApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleSignUpdate",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSignEdit(ServerboundSignUpdatePacket p_9921_, CallbackInfo ci) {
        BlockPos pos = p_9921_.getPos();
        String[] lines = p_9921_.getLines();
        BlockState state = player.level().getBlockState(pos);
        String dim = player.level().dimension().location().toString();

        if (!(state.getBlock() instanceof WallSignBlock)) return;

        Direction attachedDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        BlockPos attachedBlockPos = pos.relative(attachedDirection);

        // Check if there's already a shop here
        Optional<Shop> shop = Shop.get(attachedBlockPos, dim);
        Optional<AdminShop> adminShop = AdminShop.getAdminShop(attachedBlockPos, dim);
        if (shop.isPresent() || adminShop.isPresent()) {
            ci.cancel();
            return;
        }

        String shopLine = lines[0].toLowerCase();
        boolean isAdminShop = shopLine.equals("[adminbuy]") || shopLine.equals("[adminsell]");
        boolean isRegularShop = shopLine.equals("[buy]") || shopLine.equals("[sell]");

        if (!isAdminShop && !isRegularShop) return;

        // Check permissions for admin shops
        if (isAdminShop && !player.hasPermissions(3)) {
            player.sendSystemMessage(
                    Component.literal("You don't have permission to create admin shops!")
                            .setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED))
            );
            return;
        }

        player.server.execute(() -> {
            String priceLine = lines[3];


            if (priceLine.isEmpty() || !EconomyUtils.isNumeric(priceLine)) {
                player.sendSystemMessage(
                        Component.literal("Shop format:")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED))
                );
                player.sendSystemMessage(
                        Component.literal("Line 1: [buy] or [sell]")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                );
                player.sendSystemMessage(
                        Component.literal("Line 2: Whatever you want!")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                );
                player.sendSystemMessage(
                        Component.literal("Line 3: Whatever you want!")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                );
                player.sendSystemMessage(
                        Component.literal("Line 4: price (in $)")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                );
                return;
            }

            if (Integer.parseInt(priceLine) <= 0) {
                player.sendSystemMessage(
                        Component.literal("The price must be a positive number!")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED))
                );
                return;
            }

            if (!(player.level().getBlockEntity(attachedBlockPos) instanceof BarrelBlockEntity barrel)) {
                return;
            }

            ItemStack item = barrel.getItem(0);
            if (item.isEmpty()) {
                player.sendSystemMessage(
                        Component.literal("You need to put an item in the first slot of the barrel to define what will be traded!")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.RED))
                );
                return;
            }

            int count = item.getCount();

            if (isAdminShop) {
                String type = shopLine.equals("[adminbuy]") ? "buy" : "sell";

                AdminShopApi.create(
                        BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),
                        count,
                        Integer.parseInt(priceLine),
                        attachedBlockPos.getX(),
                        attachedBlockPos.getY(),
                        attachedBlockPos.getZ(),
                        dim,
                        type
                );

                player.playSound(SoundEvents.PLAYER_LEVELUP);
                player.sendSystemMessage(
                        Component.literal("Admin shop created! This shop has unlimited stock/money.")
                                .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD))
                );
            } else {
                // Regular shop creation logic (existing code)
                String type = shopLine.equals("[buy]") ? "buy" : "sell";

                ShopApi.create(
                        player.getUUID().toString(),
                        BuiltInRegistries.ITEM.getKey(item.getItem()).toString(),
                        count,
                        Integer.parseInt(priceLine),
                        attachedBlockPos.getX(),
                        attachedBlockPos.getY(),
                        attachedBlockPos.getZ(),
                        dim,
                        type
                );

                player.playSound(SoundEvents.PLAYER_LEVELUP);

                if (type.equals("buy")) {
                    player.sendSystemMessage(
                            Component.literal("Shop created! Players will buy " + count + " items for " + priceLine + "$ each.")
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN))
                    );
                    player.sendSystemMessage(
                            Component.literal("Fill the barrel with items in stacks of 64.")
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                    );
                } else {
                    player.sendSystemMessage(
                            Component.literal("Shop created! Players will sell " + count + " items for " + priceLine + "$ each.")
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN))
                    );
                    player.sendSystemMessage(
                            Component.literal("Make sure you have enough money in your balance.")
                                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW))
                    );
                }
            }
        });
    }

    @Inject(
            method = "handleSignUpdate",
            at = @At("RETURN")
    )
    private void afterSignUpdate(ServerboundSignUpdatePacket p_9921_, CallbackInfo ci) {
        player.server.execute(() -> {
            BlockPos pos = p_9921_.getPos();
            String[] lines = p_9921_.getLines();
            BlockState state = player.level().getBlockState(pos);

            String lines0 = lines[0].toLowerCase();

            boolean isAdminShop = lines0.equals("[adminbuy]") || lines0.equals("[adminsell]");
            boolean isRegularShop = lines0.equals("[buy]") || lines0.equals("[sell]");

            if (!isAdminShop && !isRegularShop) return;
            if (!(state.getBlock() instanceof WallSignBlock)) return;

            Direction attachedDirection = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
            BlockPos attachedBlockPos = pos.relative(attachedDirection);

            if (!(player.level().getBlockEntity(attachedBlockPos) instanceof BarrelBlockEntity barrel)) return;
            ItemStack item = barrel.getItem(0);
            if (item.isEmpty()) return;

            if (isAdminShop && !player.hasPermissions(3)) return;

            if (player.level().getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
                ChatFormatting color;
                if (isAdminShop) {
                    color = lines0.equals("[adminbuy]") ? ChatFormatting.DARK_BLUE : ChatFormatting.DARK_RED;
                } else {
                    color = lines0.equals("[buy]") ? ChatFormatting.BLUE : ChatFormatting.RED;
                }

                SignText newText = new SignText()
                        .setMessage(0, Component.literal(lines[0].toUpperCase()).setStyle(Style.EMPTY.applyFormat(color)))
                        .setMessage(1, Component.literal(lines[1]).setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW)))
                        .setMessage(2, Component.literal(lines[2]).setStyle(Style.EMPTY.applyFormat(ChatFormatting.YELLOW)))
                        .setMessage(3, Component.literal("$" + lines[3]).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN)));

                signEntity.updateText(side -> newText, true);
                signEntity.setChanged();
                player.level().sendBlockUpdated(pos, state, state, 3);
            }
        });
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void beforeClick(ServerboundContainerClickPacket pkt, CallbackInfo ci) {
        if (!(player.containerMenu instanceof ChestMenu ch
                && ch.getContainer() instanceof BarrelBlockEntity barrel)) return;

        String dim = barrel.getLevel().dimension().location().toString();

        // Check for regular shop
        Shop shop = Shop.get(barrel.getBlockPos(), dim).orElse(null);
        if (shop != null && !shop.owner().equals(player.getUUID().toString())) {
            ci.cancel();
            return;
        }

        // Check for admin shop
        AdminShop adminShop = AdminShop.getAdminShop(barrel.getBlockPos(), dim).orElse(null);
        if (adminShop != null && !player.hasPermissions(3)) {
            ci.cancel();
        }
    }
}