-- Atomically increment today count and ensure key has TTL.
-- KEYS[1]: redis key
-- ARGV[1]: ttl seconds

local key = KEYS[1]
local ttl = tonumber(ARGV[1])

local value = redis.call('INCR', key)

if redis.call('TTL', key) < 0 then
  redis.call('EXPIRE', key, ttl)
end

return value
