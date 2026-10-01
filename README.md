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
| Matching queue / bans | In-memory (single-instance) |
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
    RateLimiterService          Per-IP sliding-window rate limiting (see "Abuse protection")
    ReportService                Report counters + IP bans
    CapsuleService               Time Capsule leave/claim logic (double opt-in)
    store/                       MatchQueueStore & ModerationStore (in-memory)
  model/                        ChatUser, MatchPair, Capsule (JPA entity), dto/*
  repository/                  CapsuleRepository (Spring Data JPA)
  scheduler/                   CapsuleScheduler (daily unlock sweep)
  validation/                   InputValidator — strict allow-list checks used at every
                                  entry point (see "Input validation" below)
                                  ClientIpResolver — trusted-proxy-aware IP resolution
                                  (see "Security headers & hardening" below)

src/main/resources/
  application.properties           Default config (see below)
  application-dev.properties        Local-dev-only overrides (H2 console)
  application-mysql.properties      MySQL profile overrides
  static/                          index.html, chat.html, capsule.html, css/, js/
```

`config/` also holds `ApiRateLimitFilter` and `SecurityHeadersFilter` — both plain
`OncePerRequestFilter`s, auto-registered as servlet filters via `@Component`.

## Running locally (zero setup)

Requires JDK 21. No MySQL, no `.env` file, no local Maven install needed — the
bundled wrapper downloads the right Maven version, and the default profile uses an
in-memory H2 database and an in-memory matching queue.

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

To test the 7-day unlock without waiting a week, either:
- temporarily set `quikko.capsule.unlock-after-days=0` in `application.properties` and restart, or
- restart with `SPRING_PROFILES_ACTIVE=dev` to enable the H2 console at `/h2-console`
  (JDBC URL `jdbc:h2:mem:quikko`, user `sa`, blank password — **off by default**, even
  for the local H2 profile, see "Security headers & hardening" below) and manually
  back-date the `UNLOCK_AT` column of the two `CAPSULES` rows.

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
| `quikko.security.trusted-proxies` | empty | Comma-separated CIDRs/IPs of reverse proxies allowed to set `X-Forwarded-For`; empty = never trust it, always use the raw socket address. **Set this when deployed behind a reverse proxy/PaaS load balancer** — see "Security headers & hardening" below |
| `quikko.security.allowed-origins` | empty | Comma-separated browser origins allowed to open the `/ws` WebSocket; empty = same-origin only |
| `quikko.webrtc.stun-urls` | Google's public STUN | Comma-separated STUN server URLs |
| `quikko.webrtc.turn-url` / `turn-secret` | empty | TURN server + shared secret for ephemeral HMAC credentials; also settable via `TURN_URL`, `TURN_SECRET` env vars |
| `quikko.webrtc.turn-credential-ttl-seconds` | `600` | How long an issued TURN credential stays valid |
| `quikko.interests.suggested` | Music, Movies, Gaming, … | Comma-separated suggested interest tags shown as chips |
| `quikko.capsule.unlock-after-days` | `7` | Time Capsule unlock delay |
| `quikko.capsule.max-length` | `280` | Max capsule message length |

### Environment variables

| Variable | Used for |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `TURN_URL`, `TURN_SECRET` | TURN server + shared secret for WebRTC relay in production (see "Security headers & hardening" below) |
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
  (nginx, Caddy, a load balancer) before deploying to `quikko.xyz`.
- **TURN server is required for real-world use.** STUN alone lets two peers discover
  their public IP/port, but it does **not** help when both sides are behind symmetric
  NATs or restrictive corporate firewalls — a sizeable fraction of real users. Run a
  TURN server that supports the use-auth-secret / time-limited REST API scheme (e.g.
  [coturn](https://github.com/coturn/coturn) with `static-auth-secret` configured) and
  set `TURN_URL`, `TURN_SECRET` — see "Security headers & hardening" below.
- **Put Quikko behind a reverse proxy correctly, or rate limits/bans will misfire.**
  If you run nginx/Caddy/a PaaS load balancer in front of Quikko, set
  `quikko.security.trusted-proxies` to that proxy's IP(s)/CIDR so the app can recover
  the real client IP from `X-Forwarded-For` — otherwise every visitor is rate-limited
  and banned as if they were the proxy itself. Also set
  `server.forward-headers-strategy=framework` (a standard Spring Boot property) so the
  security-headers filter's HTTPS detection (for `Strict-Transport-Security`) honors
  the proxy's `X-Forwarded-Proto` instead of only recognizing a direct TLS connection.
- **Single instance only.** The matching queue, IP bans, and rate limits are all
  in-memory and process-local — running more than one app instance behind a load
  balancer will split traffic across independent, unsynchronized queues. Keep Quikko
  to a single instance (vertically scale it) unless you reintroduce a shared store.
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

## Input validation

Every place user input enters the app is validated at the boundary via
`com.quikko.validation.InputValidator` (allow-list regex/length checks — anonymous
session ids, pair ids, interest tags, chat/capsule text, WebRTC signal types, capsule
claim tokens) before any of it reaches a service or repository. Invalid input is
rejected outright (dropped, or answered with an `ERROR` event / HTTP 400) rather than
silently truncated or coerced. A few notes on what was and wasn't applicable here:

- **XSS / script injection** — the frontend renders all user-supplied text via
  `textContent` (never `innerHTML`) everywhere except one spot (the Time Capsule claim
  page), which already HTML-escapes before interpolating. No changes were needed there;
  `InputValidator` additionally rejects control characters in free-text fields as
  general hygiene.
- **SQL injection** — the app only ever talks to the database through Spring Data JPA
  derived query methods (no raw/native SQL, no string-concatenated queries anywhere),
  which are inherently parameterized. The one real SQL-adjacent exposure was the H2
  web console (a browser SQL client bound to whatever datasource is configured) being
  reachable in a hypothetical production deployment — it's now force-disabled under the
  `mysql` profile and only ever on for local dev.
- **Command injection** — not applicable; the app never invokes an OS process
  (no `Runtime.exec`/`ProcessBuilder` anywhere).
- **Unsafe file uploads** — not applicable; there is no file upload feature or
  multipart endpoint anywhere in the app.
- WebSocket frames are additionally capped at 64KB transport-wide
  (`WebSocketConfig#configureWebSocketTransport`) so no single field-level check is the
  only thing standing between the relay and an oversized payload.

## Security headers & hardening

A few structural hardening items beyond input validation, applied directly (there's
no Spring Security filter chain here, by design — no accounts):

- **Trusted-proxy-aware `X-Forwarded-For`.** `com.quikko.validation.ClientIpResolver`
  only honors `X-Forwarded-For` when the request's real socket address matches
  `quikko.security.trusted-proxies`; otherwise every rate-limit/ban decision uses the
  raw socket address, so a client can no longer spoof the header to bypass either one.
  Used both for `/api/*` requests and at the `/ws` handshake.
- **Session-bound anonId on every WebSocket message.** `queue.join` is the only place
  a STOMP session's anonId is *established* (`SessionRegistry.register`). Every other
  handler (`chat.send`, `signal`, `match.skip`, `report`, `capsule.leave`,
  `queue.leave`) now checks the payload's claimed anonId against
  `SessionRegistry.anonIdFor(sessionId)` and silently drops the message on a mismatch
  — a connection can no longer act as an arbitrary anonId it merely claims.
- **Restricted WebSocket origins.** `quikko.security.allowed-origins` replaces the
  previous wildcard; empty (default) means same-origin only.
- **Fail-closed anonymous id generation.** The frontend refuses to run (shows a clear
  message) rather than falling back to `Math.random()` if `crypto.randomUUID` isn't
  available (old browser, or not a secure context) — the id is used for rate
  limiting/moderation/session binding, so it needs to actually be unguessable.
- **H2 console off by default**, including for the local H2 profile — opt in locally
  with `SPRING_PROFILES_ACTIVE=dev` (`application-dev.properties`).
- **No default DB credentials.** `DB_USERNAME`/`DB_PASSWORD` have no fallback under
  the `mysql` profile — a deployment that forgets to set them fails to start instead
  of silently connecting with a guessable credential.
- **Baseline security headers** (`com.quikko.config.SecurityHeadersFilter`) on every
  response: `Content-Security-Policy` (self-only script/style, `wss:`/`https:` for
  the STOMP connection and SockJS fallbacks), `X-Frame-Options: DENY`,
  `Referrer-Policy: no-referrer`, and `Strict-Transport-Security` (only when the
  request is already HTTPS, so local HTTP dev is unaffected).
- **Ephemeral, time-limited TURN credentials** instead of one static, reusable pair —
  see `quikko.webrtc.turn-secret` above. `/api/webrtc/ice-servers` computes a fresh
  HMAC-SHA1 username/credential per request (coturn's use-auth-secret scheme) that
  stops working after `turn-credential-ttl-seconds`, so scraping the endpoint only
  yields a short-lived credential rather than a permanent one.

## Abuse protection

Every abuse-prone action is sliding-window rate limited per caller IP via
`com.quikko.service.RateLimiterService`, backed by `ModerationStore`'s
generic `incrementAttempts(key, window)` counter (in-memory, single
instance — see "Single instance only" above). Limits are IP-based rather
than keyed by the client-supplied anonymous id, since that id is trivially
rotatable by a script (new tab = new id) while the IP is not. Over the
limit, WebSocket actions get a `RATE_LIMITED` event back (or are silently
dropped for `signal`, matching its existing drop-and-log behavior for
malformed input) and REST calls under `/api/*` get an HTTP 429 with a
`Retry-After` header, via `com.quikko.config.ApiRateLimitFilter`. Every
inbound `X-Forwarded-For` value is validated as a real IP literal before
being trusted for this (`com.quikko.validation.ClientIpResolver`) — the
same guard used at the WebSocket handshake — so it can't be used to spoof
another IP's identity or dodge the limiter with junk values. A few notes
on what was and wasn't applicable here:

- **Login attempts** — not applicable; Quikko has no accounts or login,
  by design (see "No accounts" above).
- **Account creation** — not applicable, for the same reason.
- **AI generation requests** — not applicable; the app has no AI/LLM
  integration anywhere (Common Ground icebreakers are picked from a local
  static list, see `IcebreakerService`).
- **API endpoints** — applicable and implemented: every `/api/*` route
  (`/api/interests`, `/api/webrtc/ice-servers`, `/api/capsules/claim`) is
  now rate limited, closing off both casual scraping and brute-forcing
  capsule claim tokens via repeated guesses.
- **Bots/automated scripts repeatedly calling endpoints** — applicable and
  implemented across the board: queue joins were already rate limited
  before this change; chat messages, skips, reports, WebRTC signal relay
  messages, and Time Capsule leave attempts are now rate limited too, so a
  script can't flood matches, spam chat, or grief via mass reports/skips
  any faster than a real user reasonably could.
- Re-introducing a CAPTCHA was considered out of scope — it was
  deliberately removed earlier in this app's history in favor of a more
  visible video/text mode picker, and rate limiting alone covers the
  "automated scripts calling endpoints" threat without bringing it back.

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

## Continuous integration

`.github/workflows/ci.yml` runs `./mvnw -B clean verify` (build + the test suite below)
on every push and pull request to `master`. No deploy step.

## Automated tests

```bash
./mvnw test
```

Unit tests (JUnit 5 + Mockito + AssertJ, all from `spring-boot-starter-test`, no Spring
context needed) cover the matching/pairing algorithm, rate limiting, the Time Capsule
double opt-in unlock logic, input validation, trusted-proxy-aware IP resolution, and
the WebSocket session-binding fix described above. Beyond that, the end-to-end flow
(queue → interest-based match → icebreaker → chat relay → profanity filtering → WebRTC
signaling relay → skip → Time Capsule token issuance → claim page) has also been
exercised manually against a running instance using a raw STOMP/WebSocket script.
