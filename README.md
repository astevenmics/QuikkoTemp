# Quikko ⚡

A no-login, random stranger video/text chat app (Omegle-style), built with Spring Boot.
One button connects you with a stranger; optional interest tags improve the match and
power an icebreaker banner ("Common Ground"); and either side can leave a mutual,
delayed-reveal note for the person they just talked to ("Time Capsule").

## Feature summary

- **No accounts.** Every browser tab gets a random anonymous id (`crypto.randomUUID()`,
  kept in `sessionStorage`). Nothing personal is collected.
- **Matching queue.** Click Start → join a queue → matched with someone who shares an
  interest tag if possible, else with anyone waiting once a short fallback timer elapses.
- **Video + text chat.** WebRTC peer-to-peer video/audio (signaling relayed over a
  Spring WebSocket/STOMP connection), plus a text channel with a basic profanity filter.
- **Common Ground.** A top banner shows a shared-interest icebreaker prompt, or a
  generic conversation starter if you have nothing in common.
- **Skip / Stop / Report.** Skip moves you to a new stranger, Stop leaves entirely,
  Report disconnects and flags the other session (with automatic temporary IP bans
  after repeated reports).
- **Time Capsule.** At the end of a chat, either side may leave a short note (≤280
  chars) for the specific stranger they were just paired with. It only ever unlocks if
  **both** sides left one for each other, and only after 7 days — otherwise it silently
  expires. Retrieval is via a one-time claim token pasted on `/capsule`, no accounts
  involved.

## Tech stack

| Concern              | Technology |
|-----------------------|------------|
| Backend               | Java 21, Spring Boot 3.3, Maven |
| Realtime signaling    | Spring WebSocket + STOMP (SockJS fallback) |
| Video                 | WebRTC (browser-native), STUN + optional TURN |
| Matching queue / bans | Pluggable: in-memory (default) or Redis |
| Time Capsule storage  | Spring Data JPA — H2 (default) or MySQL |
| Scheduling            | Spring `@Scheduled` |
| Frontend              | Plain HTML/CSS/JS, no framework |

## Project layout

```
src/main/java/com/quikko/
  QuikkoApplication.java       Entry point (@SpringBootApplication, @EnableScheduling)
  config/                      AppProperties, WebSocketConfig, MvcConfig
  controller/                  REST: ConfigController (interests/ICE), CapsuleController (claim)
  websocket/                   STOMP @MessageMapping handlers + session bookkeeping
    QueueController              /app/queue.join, /app/queue.leave
    MatchActionController        /app/match.skip, /app/report
    ChatController                /app/chat.send
    SignalingController           /app/signal (WebRTC offer/answer/ICE relay)
    CapsuleWsController           /app/capsule.leave
    SessionRegistry, WebSocketEventListener, ClientIpHandshakeInterceptor
  service/
    MatchingService             Queue + pairing algorithm (interest-first, fallback timer)
    IcebreakerService           Icebreaker text generation
    PairRegistry                Short-lived "who was I just paired with" memory (for capsules)
    ModerationService (+NoOp)   Pluggable hook for a future external moderation API
    ProfanityFilterService      Local word-list filter
    RateLimiterService          Join-rate limiting
    ReportService                Report counters + IP bans
    CapsuleService               Time Capsule leave/claim logic (double opt-in)
    store/                       MatchQueueStore & ModerationStore, each with
                                  in-memory (default) and Redis implementations
  model/                        ChatUser, MatchPair, Capsule (JPA entity), dto/*
  repository/                  CapsuleRepository (Spring Data JPA)
  scheduler/                   CapsuleScheduler (daily unlock sweep)

src/main/resources/
  application.properties           Default config (see below)
  application-redis.properties     Redis profile overrides
  application-mysql.properties      MySQL profile overrides
  static/                          index.html, chat.html, capsule.html, css/, js/
```

## Running locally (zero setup)

Requires JDK 21 and Maven. No Redis, no MySQL, no `.env` file needed — the default
profile uses an in-memory H2 database and an in-memory matching queue.

```bash
mvn spring-boot:run
```

Then open **two** browser tabs/windows (or one normal + one incognito, so they get
separate `sessionStorage`) at <http://localhost:8080>, click Start in both, and you
should match with yourself within a couple of seconds. Grant camera/mic permission in
both tabs to see local↔local video (works over `localhost` without HTTPS, since
browsers treat `localhost` as a secure context for WebRTC).

### Trying the Time Capsule feature quickly

1. Match two tabs, chat a bit, then click **Skip** or **Stop** in tab A and leave a
   capsule — copy the token shown.
2. Do the same in tab B (leave a capsule too — it must be mutual).
3. Go to `/capsule`, paste either token. It will say **Pending** until 7 days pass
   (this is by design — see below for how to shortcut it while testing).

To test the 7-day unlock without waiting a week, either:
- temporarily set `quikko.capsule.unlock-after-days=0` in `application.properties` and restart, or
- open the H2 console at `/h2-console` (JDBC URL `jdbc:h2:mem:quikko`, user `sa`, blank
  password) and manually back-date the `UNLOCK_AT` column of the two `CAPSULES` rows.

## Configuration reference (`application.properties`)

All settings live under the `quikko.*` tree and can be overridden with environment
variables (Spring relaxed binding, e.g. `QUIKKO_MATCHING_FALLBACK_AFTER_SECONDS`) or
`-D` system properties.

| Property | Default | Purpose |
|---|---|---|
| `quikko.matching.queue-store` | `memory` | `memory` or `redis` — selects which `MatchQueueStore`/`ModerationStore` bean is active |
| `quikko.matching.fallback-after-seconds` | `7` | How long to hold out for an interest-overlap match before pairing with anyone |
| `quikko.matching.poll-interval-ms` | `1000` | How often the matchmaking scheduler runs |
| `quikko.moderation.report-ban-threshold` | `5` | Reports against an IP before it's temporarily banned |
| `quikko.moderation.ban-duration-minutes` | `60` | Length of the temporary ban |
| `quikko.moderation.profanity-words` | (short built-in list) | Comma-separated, case-insensitive |
| `quikko.rate-limit.max-joins-per-window` / `window-seconds` | `10` / `60` | Join-attempt rate limiting per IP |
| `quikko.webrtc.stun-urls` | Google's public STUN | Comma-separated STUN server URLs |
| `quikko.webrtc.turn-url` / `turn-username` / `turn-credential` | empty | TURN server; also settable via `TURN_URL`, `TURN_USERNAME`, `TURN_CREDENTIAL` env vars |
| `quikko.interests.suggested` | Music, Movies, Gaming, … | Comma-separated suggested interest tags shown as chips |
| `quikko.capsule.unlock-after-days` | `7` | Time Capsule unlock delay |
| `quikko.capsule.max-length` | `280` | Max capsule message length |

### Environment variables

| Variable | Used for |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `TURN_URL`, `TURN_USERNAME`, `TURN_CREDENTIAL` | TURN server for WebRTC relay in production |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection (only read when the `redis` profile is active) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection (only read when the `mysql` profile is active; `DB_PORT` defaults to `3306`) |

### Switching to Redis and/or MySQL

Both are independent Spring profiles and can be combined:

```bash
# Redis-backed matching queue/bans, still using H2 for capsules
SPRING_PROFILES_ACTIVE=redis mvn spring-boot:run

# MySQL for Time Capsules, still using the in-memory queue
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run

# Both, production-style
SPRING_PROFILES_ACTIVE=mysql,redis \
REDIS_HOST=redis.internal DB_HOST=db.internal DB_USERNAME=quikko DB_PASSWORD=secret \
mvn spring-boot:run
```

Run a local Redis/MySQL for testing this with Docker:

```bash
docker run -p 6379:6379 redis:7
docker run -p 3306:3306 -e MYSQL_DATABASE=quikko -e MYSQL_USER=quikko -e MYSQL_PASSWORD=quikko -e MYSQL_ROOT_PASSWORD=root mysql:8
```

## Production deployment notes

- **HTTPS is mandatory.** Browsers only grant camera/mic access to WebRTC pages served
  over HTTPS (or `localhost`). Put Quikko behind a TLS-terminating reverse proxy
  (nginx, Caddy, a load balancer) before deploying to `quikko.xyz`.
- **TURN server is required for real-world use.** STUN alone lets two peers discover
  their public IP/port, but it does **not** help when both sides are behind symmetric
  NATs or restrictive corporate firewalls — a sizeable fraction of real users. Run a
  TURN server (e.g. [coturn](https://github.com/coturn/coturn)) and set `TURN_URL`,
  `TURN_USERNAME`, `TURN_CREDENTIAL` (short-lived/HMAC credentials recommended over
  static ones).
- **Use Redis in any multi-instance deployment.** The in-memory queue store only works
  correctly with a single app instance — matching, IP bans, and rate limits are all
  process-local otherwise. Activate the `redis` profile once you run more than one
  instance or need state to survive a restart.
- **Use MySQL, not H2, in production** — H2 here runs in `mem` mode and loses all
  Time Capsules on restart. Activate the `mysql` profile.
- **WebSocket-aware load balancing.** If you run multiple instances behind a load
  balancer, either enable sticky sessions for the `/ws` endpoint or move to a
  broker-relay STOMP setup (e.g. `enableStompBrokerRelay` with a real message broker
  like RabbitMQ) since the current `SimpleBrokerMessageHandler` only fans out messages
  within a single JVM.
- **Moderation.** `ModerationService` is a stub (`NoOpModerationService`, always
  allows). Swap in a real implementation (e.g. calling a third-party text/image
  moderation API) by providing your own `@Service` implementing the interface —
  no other code needs to change since callers only depend on the interface.

## Building a runnable jar

```bash
mvn clean package
java -jar target/quikko.jar
```

## Manual test notes

The end-to-end flow (queue → interest-based match → icebreaker → chat relay →
profanity filtering → WebRTC signaling relay → skip → Time Capsule token issuance →
claim page) has been exercised against a running instance using a raw STOMP/WebSocket
script; no automated test suite is included yet.
