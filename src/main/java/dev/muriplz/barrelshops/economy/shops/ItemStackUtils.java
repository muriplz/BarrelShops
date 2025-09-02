package dev.muriplz.barrelshops.economy.shops;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

public class ItemStackUtils {

    public static String serialize(ItemStack stack, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        return stack.save(registryAccess, tag).toString();
    }

    public static ItemStack deserialize(String data, RegistryAccess registryAccess) {
        try {
            CompoundTag tag = TagParser.parseTag(data);
            return ItemStack.parseOptional(registryAccess, tag);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}