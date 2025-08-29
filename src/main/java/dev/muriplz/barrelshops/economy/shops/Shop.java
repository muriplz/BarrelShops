package dev.muriplz.barrelshops.economy.shops;

import dev.muriplz.barrelshops.storage.Database;
import net.minecraft.core.BlockPos;

import java.time.Instant;
import java.util.Optional;

public record Shop(
        long id,
        String owner,
        String selling,
        int amount,
        int price,
        int x, int y, int z,
        String world,
        String type,
        Instant creation
) {
    public static Optional<Shop> get(BlockPos pos, String world) {
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                SELECT id,
                       owner,
                       selling,
                       amount,
                       price,
                       x, y, z,
                       world,
                       type,
                       creation
                  FROM shops
                 WHERE x = :x
                   AND y = :y
                   AND z = :z
                   AND world = :world
                """)
                        .bind("x", pos.getX())
                        .bind("y", pos.getY())
                        .bind("z", pos.getZ())
                        .bind("world", world)
                        .map((rs, ctx) -> new Shop(
                                rs.getLong("id"),
                                rs.getString("owner"),
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