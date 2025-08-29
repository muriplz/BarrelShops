package dev.muriplz.barrelshops.economy.shops;

import dev.muriplz.barrelshops.storage.Database;

public class AdminShopApi {

    public static void create(String selling, int amount, int price, int x, int y, int z, String world, String type) {
        Database.getJdbi().useHandle(handle -> {
            handle.createUpdate("""
                    INSERT INTO admin_shops (selling, amount, price, x, y, z, world, type)
                    VALUES (:selling, :amount, :price, :x, :y, :z, :world, :type)
                    """)
                    .bind("selling", selling)
                    .bind("amount", amount)
                    .bind("price", price)
                    .bind("x", x)
                    .bind("y", y)
                    .bind("z", z)
                    .bind("world", world)
                    .bind("type", type)
                    .execute();
        });
    }

    public static void delete(long id) {
        Database.getJdbi().useHandle(handle -> {
            handle.createUpdate("""
                    DELETE FROM admin_shops WHERE id = :id
                    """)
                    .bind("id", id)
                    .execute();
        });
    }
}