package dev.muriplz.barrelshops.storage;

public class DatabaseUtils {

    public static void createTables() {
        Database.getJdbi().useHandle(handle -> {
            handle.execute("""
            CREATE TABLE IF NOT EXISTS balances (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                uuid CHAR(36) NOT NULL UNIQUE,
                balance BIGINT NOT NULL CHECK (balance >= 0)
            );
            """);
        });

        Database.getJdbi().useHandle(handle -> {
            handle.execute("""
            CREATE TABLE IF NOT EXISTS shops (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                owner CHAR(36) NOT NULL,
                selling VARCHAR(255) NOT NULL,
                amount INT NOT NULL,
                price INT NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                world VARCHAR(255) NOT NULL,
                type VARCHAR(255) NOT NULL,
                creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """);
        });

        Database.getJdbi().useHandle(handle -> {
            handle.execute("""
            CREATE TABLE IF NOT EXISTS admin_shops (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                selling VARCHAR(255) NOT NULL,
                amount INT NOT NULL,
                price INT NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                world VARCHAR(255) NOT NULL,
                type VARCHAR(255) NOT NULL,
                creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """);
        });

    }
}
