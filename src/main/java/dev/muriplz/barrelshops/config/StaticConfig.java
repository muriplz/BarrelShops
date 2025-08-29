package dev.muriplz.barrelshops.config;

import static dev.muriplz.barrelshops.config.ConfigReader.*;

public class StaticConfig {
    public static final boolean production = true;

    public static final String dbUrl = production
            ? DB_URL
            : "jdbc:mysql://localhost:3307/mysql";

    public static final String dbUser = production
            ? DB_USER
            : "root";
    public static final String dbPassword = production
            ? DB_PASSWORD
            : "lell";
}
