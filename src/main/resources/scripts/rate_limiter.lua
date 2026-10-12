-- KEYS[1]: Key of rate limiter
-- ARGV[1]: Capacity
-- ARGV[2]: Refill rate (tokens per second)
-- ARGV[3]: Current timestamp (in seconds)
-- ARGV[4]: Number of tokens to consume

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local data = redis.call("HMGET", key, 'tokens', 'last_refreshed')
local tokens = tonumber(data[1])
local last_refreshed = tonumber(data[2])

if tokens == nil then
    tokens = capacity
    last_refreshed = now
else
    local elapsed = math.max(0, now - last_refreshed)
    tokens = math.min(capacity, tokens + (elapsed * refill_rate))
    last_refreshed = now
end

if tokens >= requested then
    tokens = tokens - requested
    redis.call("HMSET", key, 'tokens', tokens, 'last_refreshed', last_refreshed)
    redis.call("EXPIRE", key, 60)
    return 1
else 
    redis.call("HMSET", key, 'tokens', tokens, 'last_refreshed', last_refreshed)
    redis.call("EXPIRE", key, 60)
    return 0
end