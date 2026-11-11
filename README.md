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
  native WebSocket/STOMP connection), plus a text channel with a basic profanity filter.
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

| Concern               | Technology |
|------------------------|------------|
| Backend                | Java 21, Spring Boot 4.1, Maven (wrapper included) |
| Realtime signaling     | Spring WebSocket + STOMP, native WebSocket client (`@stomp/stompjs`) |
| Video                  | WebRTC (browser-native), STUN + optional TURN (ephemeral HMAC credentials) |
| Matching queue / bans  | In-memory (single-instance — see below) |
| Time Capsule storage   | Spring Data JPA — H2 (default) or MySQL |
| Scheduling             | Spring `@Scheduled` |
| Frontend               | Plain HTML/CSS/JS, no framework |

## Project layout

```
src/main/java/com/quikko/
  QuikkoApplication.java       Entry point (@SpringBootApplication, @EnableScheduling)
  config/
    AppProperties                The quikko.* configuration tree (matching, moderation,
                                  rate-limit, security, webrtc, interests, capsule)
    WebSocketConfig               STOMP-over-WebSocket wiring (/ws, no SockJS)
    MvcConfig                     Friendly extension-less URLs (/chat, /capsule)
    ApiRateLimitFilter             Per-IP rate limiting for /api/* (429 + Retry-After)
    SecurityHeadersFilter          CSP, X-Frame-Options, X-Content-Type-Options,
                                   Permissions-Policy, Referrer-Policy, HSTS
    MysqlCredentialsCheck          Fails startup fast if DB_USERNAME/DB_PASSWORD are
                                   missing under the mysql profile
  controller/                  REST: ConfigController (interests/ICE), CapsuleController
                                (claim), GlobalRestExceptionHandler
  websocket/                   STOMP @MessageMapping handlers + session bookkeeping
    QueueController               /app/queue.join, /app/queue.leave
    MatchActionController         /app/match.skip, /app/report
    ChatController                 /app/chat.send
    SignalingController            /app/signal (WebRTC offer/answer/ICE relay)
    CapsuleWsController            /app/capsule.leave
    SessionRegistry, WebSocketEventListener, ClientIpHandshakeInterceptor
  service/
    MatchingService              Queue + pairing algorithm (interest-first, fallback timer)
    IcebreakerService             Icebreaker text generation
    PairRegistry                 Short-lived "who was I just paired with" memory (for capsules)
    ModerationService (+NoOp)    Pluggable hook for a future external moderation API
    ProfanityFilterService        Local word-list filter
    RateLimiterService            Per-IP sliding-window rate limiting
    ReportService                  Report counters + IP bans
    CapsuleService                 Time Capsule leave/claim logic (double opt-in)
    store/                        MatchQueueStore & ModerationStore (in-memory)
  model/                        ChatUser, MatchPair, Capsule (JPA entity), dto/*
  repository/                  CapsuleRepository (Spring Data JPA)
  scheduler/                   CapsuleScheduler (daily unlock sweep + 90-day retention purge)
  validation/
    InputValidator                Strict allow-list checks used at every entry point
    ClientIpResolver               Trusted-proxy-aware X-Forwarded-For resolution

src/main/resources/
  application.properties           Default config (H2, in-memory queue — see below)
  application-dev.properties        Local-dev-only overrides
  application-mysql.properties      MySQL profile overrides
  static/                          index.html, chat.html, capsule.html, css/, js/
                                   privacy.html, terms.html, cookies.html, refund.html

src/test/java/com/quikko/...   JUnit 5 + Mockito unit tests (matching, rate limiting,
                                Time Capsule unlock logic, input validation, IP
                                resolution, WS session-binding)
```

## Running locally (zero setup)

Requires JDK 21. No MySQL, no `.env` file, no local Maven install needed — the bundled
wrapper downloads the right Maven version, and the default profile uses an in-memory
H2 database and an in-memory matching queue.

```bash
./mvnw spring-boot:run
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

To test the 7-day unlock without waiting a week, temporarily set
`quikko.capsule.unlock-after-days=0` in `application.properties` and restart.

(There is no browser-based database console available for manually inspecting H2 in
this project — Spring Boot 4 removed `H2ConsoleAutoConfiguration` entirely, so
`spring.h2.console.enabled` currently has no effect regardless of value. In-memory H2
can only ever be reached from within the same JVM anyway, so this isn't a capability
loss in practice.)

## Configuration reference (`application.properties`)

All settings live under the `quikko.*` tree and can be overridden with environment
variables (Spring relaxed binding, e.g. `QUIKKO_MATCHING_FALLBACK_AFTER_SECONDS`) or
`-D` system properties.

| Property | Default | Purpose |
|---|---|---|
| `quikko.matching.fallback-after-seconds` | `7` | How long to hold out for an interest-overlap match before pairing with anyone |
| `quikko.matching.poll-interval-ms` | `1000` | How often the matchmaking scheduler runs |
| `quikko.moderation.report-ban-threshold` | `5` | Reports against an IP before it's temporarily banned |
| `quikko.moderation.ban-duration-minutes` | `60` | Length of the temporary ban |
| `quikko.moderation.profanity-words` | (short built-in list) | Comma-separated, case-insensitive |
| `quikko.rate-limit.window-seconds` | `60` | Rolling window length shared by every rate-limit bucket below |
| `quikko.rate-limit.max-joins-per-window` | `10` | Queue-join attempts per IP per window |
| `quikko.rate-limit.max-chat-messages-per-window` | `60` | Chat messages per IP per window |
| `quikko.rate-limit.max-skips-per-window` | `30` | Skip actions per IP per window |
| `quikko.rate-limit.max-reports-per-window` | `10` | Report actions per IP per window |
| `quikko.rate-limit.max-signals-per-window` | `120` | WebRTC signal relay messages per IP per window |
| `quikko.rate-limit.max-capsule-leaves-per-window` | `10` | Time Capsule leave attempts per IP per window |
| `quikko.rate-limit.max-api-requests-per-window` | `60` | Requests to any `/api/*` REST endpoint per IP per window |
| `quikko.security.trusted-proxies` | empty | Comma-separated CIDRs/IPs of reverse proxies allowed to set `X-Forwarded-For`; empty = never trust it, always use the raw socket address. **Set this when deployed behind a reverse proxy/PaaS load balancer** (Railway, Render, nginx, Caddy, etc.) |
| `quikko.security.allowed-origins` | empty | Comma-separated browser origins allowed to open the `/ws` WebSocket; empty = same-origin only |
| `quikko.webrtc.stun-urls` | Google's public STUN | Comma-separated STUN server URLs |
| `quikko.webrtc.turn-url` / `turn-secret` | empty | TURN server + shared secret for ephemeral HMAC credentials; also settable via `TURN_URL`, `TURN_SECRET` env vars |
| `quikko.webrtc.turn-credential-ttl-seconds` | `600` | How long an issued TURN credential stays valid |
| `quikko.interests.suggested` | Music, Movies, Gaming, … | Comma-separated suggested interest tags shown as chips |
| `quikko.capsule.unlock-after-days` | `7` | Time Capsule unlock delay |
| `quikko.capsule.max-length` | `280` | Max capsule message length |
| `quikko.capsule.retention-days` | `90` | How long a capsule's message text is kept, win or lose, before the daily scheduler purges it — a data-minimization backstop, not a feature |

### Environment variables

| Variable | Used for |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `TURN_URL`, `TURN_SECRET` | TURN server + shared secret for WebRTC relay in production — see "Security hardening" below |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | MySQL connection (only read when the `mysql` profile is active; `DB_PORT` defaults to `3306`, `DB_HOST`/`DB_NAME` default to `localhost`/`quikko`) |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL credentials — **required** when the `mysql` profile is active, no default; the app refuses to start without them rather than falling back to a guessable credential |

### Switching to MySQL

The `mysql` Spring profile is a convenience bundle that sets the relevant
`spring.datasource.*` properties together with sensible env-var defaults.

```bash
# MySQL for Time Capsules, still using the in-memory queue
SPRING_PROFILES_ACTIVE=mysql \
DB_HOST=db.internal DB_USERNAME=quikko DB_PASSWORD=secret \
./mvnw spring-boot:run
```

Run a local MySQL for testing this with Docker:

```bash
docker run -p 3306:3306 -e MYSQL_DATABASE=quikko -e MYSQL_USER=quikko -e MYSQL_PASSWORD=quikko -e MYSQL_ROOT_PASSWORD=root mysql:8
```

## Production deployment notes

- **HTTPS is mandatory.** Browsers only grant camera/mic access to WebRTC pages served
  over HTTPS (or `localhost`). Put Quikko behind a TLS-terminating reverse proxy
  (nginx, Caddy, a load balancer) before deploying to a real domain.
- **TURN server is required for real-world use.** STUN alone lets two peers discover
  their public IP/port, but it does **not** help when both sides are behind symmetric
  NATs or restrictive corporate firewalls — a sizeable fraction of real users. Run a
  TURN server that supports the use-auth-secret / time-limited REST API scheme (e.g.
  [coturn](https://github.com/coturn/coturn) with `static-auth-secret` configured) and
  set `TURN_URL`, `TURN_SECRET`.
- **Put Quikko behind a reverse proxy correctly, or rate limits/bans will misfire.**
  If you run nginx/Caddy/a PaaS load balancer in front of Quikko, set
  `quikko.security.trusted-proxies` to that proxy's IP(s)/CIDR so the app can recover
  the real client IP from `X-Forwarded-For` — otherwise every visitor is rate-limited
  and banned as if they were the proxy itself.
- **Single instance only.** `InMemoryMatchQueueStore` and `InMemoryModerationStore`
  hold all queue/pair/rate-limit/ban state in process memory. This means: (a) it
  cannot be horizontally scaled behind a load balancer as-is — two instances won't
  share a queue, so users on different instances will never match each other — and
  (b) every restart/redeploy drops all in-flight state (active queue, live pairs,
  bans). Keep Quikko to a single instance (vertically scale it) unless a shared store
  is reintroduced. Redis was deliberately removed from this project's history and
  should not be reintroduced without being explicitly asked for.
- **Use MySQL, not H2, in production** — H2 here runs in `mem` mode and loses all
  Time Capsules on restart. Activate the `mysql` profile.
- **WebSocket-aware load balancing.** If you ever do run multiple instances behind a
  load balancer, either enable sticky sessions for the `/ws` endpoint or move to a
  broker-relay STOMP setup (e.g. `enableStompBrokerRelay` with a real message broker
  like RabbitMQ) since the current `SimpleBrokerMessageHandler` only fans out messages
  within a single JVM.
- **Moderation.** `ModerationService` is a stub (`NoOpModerationService`, always
  allows). Swap in a real implementation (e.g. calling a third-party text/image
  moderation API) by providing your own `@Service` implementing the interface —
  no other code needs to change since callers only depend on the interface.

## Input validation

Every place user input enters the app is validated at the boundary via
`com.quikko.validation.InputValidator` (allow-list regex/length checks — anonymous
session ids, pair ids, interest tags, chat/capsule text, WebRTC signal types, capsule
claim tokens) before any of it reaches a service or repository. Invalid input is
rejected outright (dropped, or answered with an `ERROR` event / HTTP 400) rather than
silently truncated or coerced.

- **XSS / script injection** — the frontend renders all user-supplied text via
  `textContent` (never `innerHTML`) everywhere except one spot (the Time Capsule claim
  page), which already HTML-escapes before interpolating.
- **SQL injection** — the app only ever talks to the database through Spring Data JPA
  derived query methods (no raw/native SQL, no string-concatenated queries anywhere),
  which are inherently parameterized.
- **Command injection** — not applicable; the app never invokes an OS process.
- **Unsafe file uploads** — not applicable; there is no file upload feature anywhere.
- WebSocket frames are additionally capped at 64KB transport-wide
  (`WebSocketConfig#configureWebSocketTransport`).

## Security hardening

A few structural hardening items, applied directly (there's no Spring Security filter
chain here, by design — no accounts):

- **Trusted-proxy-aware `X-Forwarded-For`.** `ClientIpResolver` only honors
  `X-Forwarded-For` when the request's real socket address matches
  `quikko.security.trusted-proxies`; otherwise every rate-limit/ban decision uses the
  raw socket address. Used both for `/api/*` requests and at the `/ws` handshake.
- **Session-bound anonId on every WebSocket message.** `queue.join` is the only place
  a STOMP session's anonId is established (`SessionRegistry.register`). Every other
  handler checks the payload's claimed anonId against
  `SessionRegistry.anonIdFor(sessionId)` and silently drops the message on a mismatch.
- **Restricted WebSocket origins** via `quikko.security.allowed-origins` (empty =
  same-origin only), and **native WebSocket, no SockJS** — every browser in current
  use (including mobile Safari) has supported WebSocket natively for years, so the
  SockJS negotiation layer (designed for IE8-era browsers) was removed as unneeded
  complexity and an extra point of failure.
- **Fail-closed anonymous id generation.** The frontend refuses to run (shows a clear
  message) rather than generating a weak id if `crypto.randomUUID` isn't available.
- **No default DB credentials.** `DB_USERNAME`/`DB_PASSWORD` have no fallback under
  the `mysql` profile — `MysqlCredentialsCheck` (an `EnvironmentPostProcessor`) fails
  startup immediately with a clear message if either is missing, before any bean
  (including the DataSource) is created.
- **Baseline security headers** (`SecurityHeadersFilter`) on every response:
  `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Permissions-Policy` (camera/mic scoped to same-origin, everything else denied),
  `Referrer-Policy: no-referrer`, and `Strict-Transport-Security` (only when the
  request is already HTTPS).
- **Time Capsule retention.** Capsule message text is purged automatically
  `quikko.capsule.retention-days` (default 90) after it's left, whether or not it was
  ever claimed — see `CapsuleScheduler#purgeExpiredCapsules`.
- **Ephemeral, time-limited TURN credentials** instead of one static, reusable pair —
  `/api/webrtc/ice-servers` computes a fresh HMAC-SHA1 username/credential per request
  (coturn's use-auth-secret scheme) that stops working after
  `turn-credential-ttl-seconds`.

Not applicable to this app, and intentionally left out: login/account-creation rate
limiting, OAuth, Spring Security, or any account system (the anonymous, no-login
design is intentional); Redis or any other shared external store (deliberately
removed from this project's history).

## Building a runnable jar

```bash
./mvnw clean package
java -jar target/quikko.jar
```

## Running in Docker

```bash
docker build -t quikko .
docker run -p 8080:8080 quikko
```

Pass any of the environment variables above with `-e` (e.g. `-e SPRING_PROFILES_ACTIVE=mysql
-e DB_HOST=... -e DB_USERNAME=... -e DB_PASSWORD=...`).

## Legal pages

`/privacy`, `/terms`, `/cookies`, and `/refund` are static pages (see `MvcConfig`)
describing what little data the app actually collects (see "Input validation" and
"Security hardening" above for the technical side of the same facts), the 18+
eligibility requirement, and that the service is currently free with nothing to
refund. A small cookie-notice banner (`js/cookie-banner.js`) links to the Cookie
Policy on first visit; dismissing it just sets a `localStorage` flag, there's nothing
to actually consent to yet since no ad network or analytics tool is wired up. These
pages name the actual operator/contact/jurisdiction currently behind this deployment —
update them if that ever changes.

## Continuous integration

`.github/workflows/ci.yml` runs `./mvnw -B clean verify` (build + the test suite below)
on every push and pull request to `master`. No deploy step.

## Automated tests

```bash
./mvnw test
```

Unit tests (JUnit 5 + Mockito + AssertJ, all from `spring-boot-starter-test`) cover the
matching/pairing algorithm, rate limiting, the Time Capsule double opt-in unlock logic,
input validation, trusted-proxy-aware IP resolution, and the WebSocket session-binding
fix described above.
