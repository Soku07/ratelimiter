local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])


local t = redis.call('TIME')
now = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)

local raw_bucket = redis.call('GET', key)
local allowed = 0
local function decode(val)
    return val and cjson.decode(val) or nil
end
local bucketValue = decode(raw_bucket)

if not bucketValue or now > bucketValue.timeStampCreated + window then
    local state = {counter = 1, timeStampCreated = now}
    redis.call('PSETEX', key, window + 1000, cjson.encode(state))
    allowed = 1
else
    if bucketValue.counter + 1 <= limit then
        local state = {counter = bucketValue.counter + 1 , timeStampCreated = bucketValue.timeStampCreated}
        redis.call('SET', key, cjson.encode(state), 'KEEPTTL')
        allowed = 1
    end
end
return allowed