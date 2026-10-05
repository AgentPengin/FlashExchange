-- Lấy tồn kho hiện tại từ Redis

local current_stock = tonumber(redis.call('GET', KEYS[1]))

if current_stock == nil then
    return -1
end

local buy_quantity = tonumber(ARGV[1])

if current_stock >= buy_quantity then
    -- Giảm tồn kho trong RAM của Redis
    redis.call('DECRBY', KEYS[1], buy_quantity)
    return 1
else 
    return 0 -- Thất bại do hết hàng
end

