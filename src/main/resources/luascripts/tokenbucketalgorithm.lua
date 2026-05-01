local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])

local raw_data = redis.call('GET', key)
local tokenCount, lastRefill
local t = redis.call('TIME')
now = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)
-- 1. Initialize or Load State
if not raw_data then
    tokenCount = capacity
    lastRefill = now
else
    local state = cjson.decode(raw_data)
    local refill = ((now - state.lastRefillTimeStamp) / window) * limit
    tokenCount = math.min(capacity, state.tokenCount + refill)
    lastRefill = now
end

-- 2. The Decision (The only difference is the allowed flag)
local allowed = 0
if tokenCount >= 1 then
    tokenCount = tokenCount - 1
    allowed = 1
end

-- 3. Unified Storage
local newState = {
    tokenCount = tokenCount,
    lastRefillTimeStamp = lastRefill
}
redis.call('SET', key, cjson.encode(newState))
redis.call('PEXPIRE', key, ttl)

return allowed