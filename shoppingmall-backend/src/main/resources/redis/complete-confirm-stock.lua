-- KEYS[1]: reservation:{paymentId}
-- ARGV[1]: DB 주문에서 만든 canonicalItems
--
--  1: CONFIRMED로 전환
--  0: 이미 CONFIRMED
-- -1: 예약 없음
-- -2: 잘못된 자료형
-- -3: 예약 내용 불일치
-- -4: 확정할 수 없는 상태

local reservationKey = KEYS[1]
local reservationType = redis.call('TYPE', reservationKey).ok

if reservationType == 'none' then
    return -1
end

if reservationType ~= 'hash' then
    return -2
end

local items = redis.call('HGET', reservationKey, 'items')

if items ~= ARGV[1] then
    return -3
end

local status = redis.call('HGET', reservationKey, 'status')

if status == 'CONFIRMED' then
    return 0
end

if status ~= 'CONFIRMING' then
    return -4
end

-- 예약 상태를 CONFIRMED로 변경
redis.call('HSET', reservationKey, 'status', 'CONFIRMED')

return 1