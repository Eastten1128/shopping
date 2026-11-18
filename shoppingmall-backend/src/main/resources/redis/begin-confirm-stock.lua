-- KEYS[1]: reservation:{paymentId}
-- ARGV[1]: DB 주문에서 만든 canonicalItems
--
--  1: CONFIRMING으로 전환
--  0: 이미 CONFIRMING
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

-- ACTIVE -> CONFIRMING
if status == 'ACTIVE' then
    redis.call('HSET', reservationKey, 'status', 'CONFIRMING')
    return 1
end

-- 이전 확정 시도가 중단된 경우의 재시도
if status == 'CONFIRMING' then
    return 0
end

return -4
