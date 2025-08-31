<p align="center">
  <img width="200" src="https://github.com/muriplz/BarrelShops/blob/main/src/main/resources/icon.png">
</p>

<h1 align="center">BarrelShops<br>
	<a href="https://www.curseforge.com/minecraft/mc-mods/barrelshops/files"><img src="https://cf.way2muchnoise.eu/versions/barrelshops.svg" alt="Supported Versions"></a>
	<a href="https://www.curseforge.com/minecraft/mc-mods/barrelshops"><img src="http://cf.way2muchnoise.eu/barrelshops.svg" alt="CF"></a>
    <a href="https://modrinth.com/mod/barrelshops"><img src="https://img.shields.io/modrinth/dt/barrelshops?logo=modrinth&label=&suffix=%20&style=flat&color=242629&labelColor=5ca424&logoColor=1c1c1c" alt="Modrinth"></a>
    <br><br>
</h1>

## Dependencies:

Requires you to install [https://www.curseforge.com/minecraft/mc-mods/mysql-jdbc](https://www.curseforge.com/minecraft/mc-mods/mysql-jdbc)

### Commands:

*   /balance \[player\]: shows your balance if no player given, if not shows their balance
*   /balancetop \[page\]: shows the leaderboard of balances
*   /balancegive <player> <amount>: admin command, requires OP
*   /pay <player> <amount>: transfers money

### Config.json

`/config/barrelshops/config.json`

```
{
  "db-url": "jdbc:mysql://HOST:PORT/DB_NAME",
  "db-user": "USER",
  "db-password": "PASSWORD",
  "c1": "The balance a new player starts with",
  "onboarding-balance": 10
}
```

You need to run the mod once, and set up the MySQL credentials, then start again the server.

### Shop creation

1.  Place a barrel
2.  Put inside the item you want to buy/sell, and the amount too, for example 15 apples.
3.  put a sign on any side of the barrel
4.  put in the first line \[BUY\], \[SELL\], \[ADMINBUY\] or \[ADMINSELL\] (admin shops require OP to set up or destroy)
5.  put in the last line (4th) the price, for example 124
6.  Done! the sign should be given some color and you can now put stock onto it or take it out (if non admin) and players will be able to use it!