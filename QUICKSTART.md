## Quickstart

### 1. Clone the repository

```bash
https://github.com/Soku07/ratelimiter.git
cd rate-limiter
```

### 2. Choose your storage backend

**Option A — Caffeine (no external dependencies)**

No setup needed. Caffeine runs in-process.


---

**Option B — Redis**

Start a local Redis server:

```bash
# MacOS
brew install redis && brew services start redis

# Ubuntu / Debian
sudo apt install redis-server && sudo systemctl start redis

# Windows (via WSL or Docker)
docker run -d -p 6379:6379 redis:7
```

Set the storage type in `application-dev.properties`:

```properties
ratelimiter.storage.type=redis
```
### 3. Start the application

```bash
bash mvnw spring-boot:run
```
Or you can simply use IntelliJ IDE's start button

### 4. Verify it is running

```bash
curl -i http://localhost:8080/api/actuator/health
```

Expected response status code : 200 or 429

---

## Configuration

### Storage Backend

```properties
# application-dev.properties

# Choose storage: caffeine | redis
ratelimiter.storage.type=caffeine
```

---

### Redis Properties

Only required when `ratelimiter.storage.type=redis`.

```properties
# Redis connection
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=

# Connection pool (Lettuce)
spring.data.redis.lettuce.pool.max-total=1500
spring.data.redis.lettuce.pool.max-idle=750
spring.data.redis.lettuce.pool.min-idle=100
spring.data.redis.lettuce.pool.max-wait-millis=5000
```

**Pool sizing guidance:**

| Property | Default | What It Controls |
|---|---|---|
| `max-total` | 1500 | Maximum total connections in the pool |
| `max-idle` | 750 | Maximum idle connections kept alive |
| `min-idle` | 100 | Minimum idle connections always maintained |
| `max-wait-millis` | 5000 | How long to wait for a connection before failing |

```
For local development:
  These defaults are intentionally sized for load testing.
  For lightweight local use, reduce to:
  
  max-total=10
  max-idle=5
  min-idle=1
  max-wait-millis=2000
```

---

### Rule Configuration

Rules are configured in `rules.yml`.
Each rule defines the path, algorithm, limit, window, and identity strategy.

```yaml
# Example rule — limit /api/payments to 10 req/min by IP
clients:
  - clientId: "enterprise-api-gateway"
    rules:
      - pathPattern: "/api/v1/token-bucket-test"
        priority: 100
        policy:
          limit: 10
          window: "PT10S"
          algorithmKey: "TOKEN_BUCKET"
          identityStrategy: "IP_ADDRESS"

      - pathPattern: "/api/v1/fixed-window-test"
        priority: 95
        policy:
          limit: 25
          window: "PT10S"
          algorithmKey: "FIXED_WINDOW"
          identityStrategy: "AUTH_TOKEN"

      - pathPattern: "/api/v1/notifications/email"
        priority: 75
        policy:
          limit: 1000
          window: "PT5M"
          algorithmKey: "SLIDING_WINDOW_LOG"
          identityStrategy: "AUTH_TOKEN"

      - pathPattern: "/api/v1/analytics/clickstream"
        priority: 5
        policy:
          limit: 15000
          window: "PT1M"
          algorithmKey: "PROBABILISTIC_SLIDING_WINDOW"
          identityStrategy: "IP_ADDRESS"

```

**Rule matching behaviour:**
- More specific paths take priority over wildcard paths by default
- Explicit `priority` field overrides pattern-length based ordering
- ANT wildcards supported: `?` (one character), `*` (one segment), `**` (any depth)

---



### Fault Tolerance & Behaviour Settings

Behaviour in edge cases is controlled via `RateLimiterSettings`.

| Property | Default | Description                                                                   |
|---|---------|-------------------------------------------------------------------------------|
| `enabled` | `true`  | Master switch. `false` disables rate limiting globally                        |
| `allowRequestOnMatchingRuleNotFound` | `true`  | No rule matches the path → allow through. Set `false` to deny unmatched paths |
| `byPassOnException` | `false` | Storage failure → fail closed by default. Set `true` to fail open             |
| `burstFactor` | `0.2`   | Token Bucket burst multiplier. `0.0` = no burst. `0.5` = 50% above limit      |

**`byPassOnException` — choose deliberately:**

```
false (fail closed) → storage fails → request rejected
  Safer for security-sensitive APIs
  No unverified traffic reaches the backend

true (fail open) → storage fails → rate limiter steps aside
  Safer for availability-sensitive APIs
  Backend remains reachable under infrastructure failure
```

> **Production note:** `allowRequestOnMatchingRuleNotFound=true` means
> any path without a configured rule is unprotected.
> Either set this to `false` or add a catch-all rule (`/**`)
> to ensure full coverage.