# 1.1
- Added support for NBT on items inside shops
- Added passive income, along configuration on `config.json`

## UPGRADING FROM 1.0

Make sure `"passive-income-interval-minutes"` and `"passive-income-amount"` are present in your config.json, if not add them manually, with the default values:
```json5
{
  "db-url": "jdbc:mysql://HOST:PORT/DB_NAME",
  "db-user": "USER",
  "db-password": "PASSWORD",
  "c1": "The balance a new player starts with",
  "onboarding-balance": 10,
  "c2": "Passive income over time, every X minutes a player gets $Y",
  "passive-income-interval-minutes": 30,
  "c3": "Set to -1 to disable passive income",
  "passive-income-amount": 5
}
```

# 1.0
- Initial release