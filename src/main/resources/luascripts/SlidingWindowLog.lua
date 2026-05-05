local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])

local t = redis.call('TIME')
now = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)

--Cleaned up version

local windowStart = now - window
local highestTimestamp = redis.call('ZRANGE', key, 0, 0, 'REV', 'WITHSCORES')
local isStale = (#highestTimestamp == 0 or now > tonumber(highestTimestamp[2]) + window)
local allowed = false

if isStale then
    redis.call('DEL', key)
    allowed = true
else
    redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
    local currentCount = tonumber(redis.call('ZCARD', key))

    if currentCount < limit then
        allowed = true
    end
end

if allowed then
    redis.call('ZADD', key, now, now)
    redis.call('PEXPIRE', key, window + 1000)
    return 1
end

return 0



-- This is correct iteration that handles all edge cases.
--local highestTimeStamp = redis.call('ZRANGE', KEYS[1], 0, 0, 'REV', 'WITHSCORES')
--
--if #highestTimeStamp == 0 or now > tonumber(highestTimeStamp[2]) + window then
--    redis.call('DEL', key)
--    redis.call('ZADD', key, now, now)
--    redis.call('PEXPIRE', key, window + 1000 ) --1000 is some delta for accuaracy
--    return 1
--end
--
--redis.call('ZREMRANGEBYSCORE', key, 0 , now - window)
--local requestCount = tonumber(redis.call('ZCARD', key))
--
--if requestCount + 1 <= limit then
--    redis.call('ZADD',key,now,now)
--    redis.call('PEXPIRE', key, window + 1000 ) --1000 is some delta for accuaracy
--    return 1
--end
--
--return 0

