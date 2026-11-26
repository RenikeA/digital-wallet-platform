# Digital Wallet Platform

A backend for a digital wallet, built with Java and Spring Boot as a set of small services. Users can register, log in, create wallets, deposit, withdraw and transfer money, and every movement is recorded in a double-entry ledger. When money moves, events are published to Kafka, and a notification service turns them into simulated email and SMS messages.

Deposits and withdrawals are simulated. No real card, bank or payment gateway is connected.

## Architecture

```
Postman / client
      |
      +--> auth-service (8081) ------> auth-postgres (5433)
      |       register, login, refresh, JWT
      |
      +--> wallet-service (8082) ----> wallet-postgres (5432)
              wallets, deposit, withdraw, transfer,
              history, statement
                    |
                    v  publishes events after each save
                 Kafka (9092), topic: wallet-events
                    |
                    v
          notification-service (8083)
              logs a simulated EMAIL and SMS
```

| Module | What it does |
|---|---|
| `auth-service` | Registers users, logs them in, and issues JWT access and refresh tokens |
| `wallet-service` | Wallets, deposits, withdrawals, transfers, history and statements |
| `notification-service` | Reads wallet events from Kafka and logs a message for each |
| `wallet-domain` | Shared entities and repositories for wallet-service |
| `wallet-common` | Shared helpers |

Each service that stores data has its own PostgreSQL database.

## Features

- Register, log in and refresh tokens (JWT, BCrypt-hashed passwords, roles)
- Create wallets, deposit, withdraw and transfer between wallets
- Double-entry ledger: a transfer writes one debit and one credit in a single database transaction
- Idempotency keys: sending the same request twice never moves money twice
- Ownership checks: a user can only use their own wallets (the owner comes from the token)
- Transaction history with pagination and filters (type, date range)
- Account statement with opening and closing balance
- Kafka events: `WALLET_CREATED`, `MONEY_DEPOSITED`, `MONEY_WITHDRAWN`, `TRANSFER_SENT`, `TRANSFER_RECEIVED`
- Database migrations with Flyway, and unit tests with JUnit and Mockito

## Technologies

Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, Apache Kafka, Docker Compose, Maven, JUnit 5, Mockito, Lombok

## How to run it

You need Java 17, Maven, Docker, and IntelliJ (or any IDE).

1. Start the databases and Kafka:
   ```
   docker compose up -d
   ```
2. Build everything:
   ```
   mvn clean install
   ```
3. Set the same `JWT_SECRET` (any text of 32 or more characters) as an environment variable for **both** `auth-service` and `wallet-service`. They won't start without it.
4. Start `auth-service`, then `wallet-service`, then `notification-service`.
5. Try it with Postman:
    - `POST http://localhost:8081/api/v1/auth/register` with an email and a password of 8 or more characters
    - `POST http://localhost:8081/api/v1/auth/login` and copy the `accessToken`
    - Send the token as a Bearer token on the wallet requests below
    - `POST http://localhost:8082/api/v1/wallets` with `{"currency": "NGN"}`
    - `POST http://localhost:8082/api/v1/wallets/deposit` with `walletId`, `amount` and a new `idempotencyKey`
6. Watch the `notification-service` console for the simulated email and SMS lines.

Access tokens last 15 minutes, so log in again when you get a `401`.

### Main endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register`, `/login`, `/refresh` | Accounts and tokens (port 8081) |
| POST | `/api/v1/wallets` | Create a wallet (port 8082) |
| POST | `/api/v1/wallets/deposit`, `/withdraw`, `/transfer` | Move money |
| GET | `/api/v1/wallets/{id}` | Wallet and balance |
| GET | `/api/v1/wallets/{id}/transactions` | History, with `type`, `from`, `to`, `page`, `size` |
| GET | `/api/v1/wallets/{id}/statement` | Statement for `from` and `to` dates |

## Running the tests

Run the tests from the Maven panel in IntelliJ, or from a terminal:

```
mvn test
```

## Future improvements

- **Outbox pattern:** save each event in the database in the same transaction, then send it, so an event is never lost if Kafka is down
- **Real payments:** connect a payment gateway and credit wallets from its webhook
- **Fully double-entry deposits and withdrawals:** debit or credit a system account on the other side
- **Idempotent consumers:** ignore a Kafka event that was already processed, using `eventId`
- **Real notifications:** look up each user's email and phone, and send real messages
- **API gateway:** check tokens once at the front, instead of in every service
- **Fraud detection, admin endpoints, limits, and more tests** (integration tests with Testcontainers)
- **Deployment:** GitHub Actions and AWS