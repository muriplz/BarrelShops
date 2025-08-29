package dev.muriplz.barrelshops.economy.shops;

import dev.muriplz.barrelshops.storage.Database;
import net.minecraft.core.BlockPos;

import java.time.Instant;
import java.util.Optional;

public record AdminShop(
        long id,
        String selling,
        int amount,
        int price,
        int x, int y, int z,
        String world,
        String type,
        Instant creation
) {

    public static Optional<AdminShop> getAdminShop(BlockPos pos, String world) {
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                SELECT id,
                       selling,
                       amount,
                       price,
                       x, y, z,
                       world,
                       type,
                       creation
                  FROM admin_shops
                 WHERE x = :x
                   AND y = :y
                   AND z = :z
                   AND world = :world
                """)
                        .bind("x", pos.getX())
                        .bind("y", pos.getY())
                        .bind("z", pos.getZ())
                        .bind("world", world)
                        .map((rs, ctx) -> new AdminShop(
                                rs.getLong("id"),
                                rs.getString("selling"),
                                rs.getInt("amount"),
                                rs.getInt("price"),
                                rs.getInt("x"),
                                rs.getInt("y"),
                                rs.getInt("z"),
                                rs.getString("world"),
                                rs.getString("type"),
                                rs.getTimestamp("creation").toInstant()
                        ))
                        .findOne()
        );
    }
}
