# Redis setup

The backend uses Redis through Spring's cache abstraction.

## Local development

Install Redis locally and run it on the default port `6379`, or run:

```bash
docker run --name college-erp-redis -p 6379:6379 -d redis:7-alpine
```

The backend defaults to:

- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `REDIS_PASSWORD=`
- `REDIS_CACHE_TTL_SECONDS=600`
- `REDIS_KEY_PREFIX=college-erp:`

## Production

Set these environment variables on the server:

```text
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<strong-password-if-configured>
REDIS_CACHE_TTL_SECONDS=600
REDIS_KEY_PREFIX=college-erp:
```

Do not commit Redis passwords or other secrets to Git.

The cache configuration is intentionally tenant-neutral at the infrastructure layer. Application cache keys must include tenant identity whenever cached data is tenant-specific.
