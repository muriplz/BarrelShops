package dev.muriplz.barrelshops.economy;

import dev.muriplz.barrelshops.config.ConfigReader;
import dev.muriplz.barrelshops.storage.Database;

import java.util.*;

public class BalanceApi {

    public static void createBalance(UUID uuid) {
        Database.getJdbi().useHandle(handle -> {
            handle.createUpdate("""
            INSERT IGNORE INTO balances (uuid, balance) VALUES (:uuid, :balance)
            """)
                    .bind("uuid", uuid.toString())
                    .bind("balance", ConfigReader.ONBOARDING_BALANCE)
                    .execute();
        });
    }

    public static int getBalance(UUID uuid) {
        return Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT balance FROM balances WHERE uuid = :uuid
                """)
                .bind("uuid", uuid.toString())
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public static LinkedHashMap<UUID, Integer> getTopBalances(int page) {
        int entries = 10;
        return Database.getJdbi().withHandle(handle -> {
            List<Map<String, Object>> results = handle.createQuery("""
            SELECT uuid, balance FROM balances
            ORDER BY balance DESC
            LIMIT :limit OFFSET :offset
            """)
                    .bind("limit", entries)
                    .bind("offset", (page - 1) * entries)
                    .mapToMap()
                    .list();

            LinkedHashMap<UUID, Integer> balances = new LinkedHashMap<>();
            for (Map<String, Object> row : results) {
                UUID uuid = UUID.fromString((String) row.get("uuid"));
                Integer balance = ((Number) row.get("balance")).intValue();
                balances.put(uuid, balance);
            }
            return balances;
        });
    }

    public static void giveBalance(UUID uuid, int amount) {
        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("""
            UPDATE balances SET balance = balance + :amount WHERE uuid = :uuid
            """)
                        .bind("amount", amount)
                        .bind("uuid", uuid.toString())
                        .execute()
        );
    }

    public static class Payment {

        public static boolean pay(UUID from, UUID to, int amount) {
            if (getBalance(from) < amount) {
                return false; // Not enough balance
            }

            Database.getJdbi().useHandle(handle -> {
                handle.createUpdate("""
                    UPDATE balances SET balance = balance - :amount WHERE uuid = :from
                    """)
                    .bind("amount", amount)
                    .bind("from", from.toString())
                    .execute();

                handle.createUpdate("""
                    UPDATE balances SET balance = balance + :amount WHERE uuid = :to
                    """)
                    .bind("amount", amount)
                    .bind("to", to.toString())
                    .execute();
            });

            return true;
        }

    }
}
