-- 4 scenarios in consideration :
-- prevBucket is nil and currBucket is nil
-- prevBucket is nil and currBucket is not nil
    --in this scenario we have to check is currBucket is valid
-- Both buckets exists
    --in this scenario as well we have to check if currBucket is valid
    --currBucket is always updated with real values and probablistic values is just used to check the limit


local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])



local prevKey = key .. ":prev"
local currKey = key .. ":curr"

-- 1. Get Redis Time for millisecond accuracy
local t = redis.call('TIME')
now = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)

-- 2. Fetch raw data
local prevBucketRaw = redis.call('GET', prevKey)
local currBucketRaw = redis.call('GET', currKey)

local allowed = 0
local requestCount = 1

-- Safety helper for JSON
local function decode(val)
    return val and cjson.decode(val) or nil
end

local curr = decode(currBucketRaw)
local prev = decode(prevBucketRaw)

-- 3. CORE LOGIC
if not curr then
    -- CASE: First request ever or complete expiration
    if requestCount <= limit then
        local state = {count = 1, timeStampCreated = now, timeStampEnd = now + window}
        -- Initial TTL is 2x window to allow for "Previous Window" reference later
        redis.call('SET', currKey, cjson.encode(state), 'PX', window * 2)
        allowed = 1
    end
else
    -- CASE: Current exists. Check if it's stale (needs rolling)
    if now > curr.timeStampEnd then
        -- The "current" bucket has finished. Move it to "previous".
        -- It stays alive for exactly 'window' ms more as a reference.
        redis.call('SET', prevKey, currBucketRaw, 'PX', window)

        -- Calculate probabilistic weight of this just-moved bucket
        local weight = math.max(0, 1 - (now - curr.timeStampEnd) / window)
        local total = (curr.count * weight) + requestCount

        if total <= limit then
            local state = {count = 1, timeStampCreated = now, timeStampEnd = now + window}
            redis.call('SET', currKey, cjson.encode(state), 'PX', window * 2)
            allowed = 1
        end
    else
        -- CASE: Current is valid. Standard weighted average.
        local weight = 0
        if prev then
            weight = math.max(0, 1 - (now - prev.timeStampEnd) / window)
        end

        local total = curr.count + ( (prev and prev.count or 0) * weight ) + requestCount

        if total <= limit then
            curr.count = curr.count + 1

            -- FIX: Calculate remaining TTL so we don't "reset" the countdown timer
            -- The bucket must die at: original timeStampEnd + 1 window
            local finalExpiry = curr.timeStampEnd + window
            local ttlRemaining = math.max(1, finalExpiry - now)

            redis.call('SET', currKey, cjson.encode(curr), 'PX', ttlRemaining)
            allowed = 1
        end
    end
end

return allowed


--First iteration of the script
--local key = KEYS[1]
--
--local window = tonumber(ARGV[1])
--local limit= tonumber(ARGV[2])
----local ttl
--
--local prevKey = key .. ":prev"
--local currKey = key .. ":curr"
--
--local requestCount = 1 --Current request is initialised instead of adding 1 later
--
--local prevKeyBucket = redis.call('GET', prevKey)
--local currKeyBucket = redis.call('GET', currKey)
--local t = redis.call('TIME')
--local now = (tonumber(t[1]) * 1000) + math.floor(tonumber(t[2]) / 1000)
--local newCurrBucketState
--local newPrevBucketState
--if not prevKeyBucket and not currKeyBucket then
--    if requestCount <= limit then --ideally application validates that limit is >= 1. But this is just a sanity check
--        newCurrBucketState = {
--            count = requestCount,
--            timeStampCreated = now,
--            timeStampEnd = now + window
--        }
--        redis.call('SET', currKey, cjson.encode(newCurrBucketState), 'PX', window * 2)
--        allowed = 1
--
--    else
--        allowed = 0
--    end
--elseif not prevKeyBucket and currKeyBucket then --currKeyBucket is not nil
--    local currKeyBucketValue = cjson.decode(currKeyBucket)
--    if now > currKeyBucketValue.timeStampEnd then
--        redis.call('SET', prevKey, currKeyBucket, 'PX', window) -- I will explain in dry run why PX is set to window and not 2*window here
--        local probablisticCount = ( currKeyBucketValue.count * (1 -(now - currKeyBucketValue.timeStampEnd)/window) )
--        requestCount = requestCount + probablisticCount
--        if requestCount <= limit then
--            newCurrBucketState = { count = requestCount,
--                                   timeStampCreated = now,
--                                   timeStampEnd = now + window
--
--            }
--            redis.call('SET', currKey, cjson.encode(newCurrBucketState), 'PX', 2*window)
--            allowed = 1
--        else
--            allowed = 0
--        end
--    else
--        requestCount = requestCount + currKeyBucketValue.count
--        if requestCount <= limit then
--            newCurrBucketState = {
--                count = requestCount,
--                timeStampCreated = currKeyBucketValue.timeStampCreated,
--                timeStampEnd = currKeyBucketValue.timeStampEnd
--            }
--            redis.call('SET', currKey, cjson.encode(newCurrBucketState))
--            allowed = 1
--        else
--            allowed = 0
--        end
--
--    end
--else
--    local currKeyBucketValue = cjson.decode(currKeyBucket)
--    local prevKeyBucketValue = cjson.decode(prevKeyBucket)
--    if now > currKeyBucketValue.timeStampEnd then
--        redis.call('SET', prevKey, currKeyBucket, 'PX', window) -- I will explain in dry run why PX is set to window and not 2*window here
--        local probablisticCount = ( currKeyBucketValue.count * (1 -(now - currKeyBucketValue.timeStampEnd)/window) )
--        requestCount = requestCount + probablisticCount
--        if requestCount <= limit then
--            newCurrBucketState = { count = requestCount,
--                                   timeStampCreated = now,
--                                   timeStampEnd = now + window
--
--            }
--            redis.call('SET', currKey, cjson.encode(newCurrBucketState), 'PX', 2*window)
--            allowed = 1
--        else
--            allowed = 0
--        end
--    else
--        local probablisticCount = currKeyBucketValue.count +( prevKeyBucketValue.count * (1 -(now - prevKeyBucketValue.timeStampEnd)/window) )
--        requestCount = requestCount + probablisticCount
--        if(requestCount <= limit) then
--            newCurrBucketState = {
--                count = requestCount,
--                timeStampCreated = currKeyBucketValue.timeStampCreated,
--                timeStampEnd = currKeyBucketValue.timeStampEnd
--            }
--            redis.call('SET', currKey, cjson.encode(newCurrBucketState))
--            allowed = 1
--        else
--            allowed = 0
--        end
--    end
--
--
--end
--end
--
--return allowed