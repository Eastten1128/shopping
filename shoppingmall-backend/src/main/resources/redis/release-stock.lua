-- KEYS[1]: 예약 키
-- KEYS[2...]: 반환할 상품의 재고 키
--
-- ARGV[1]: Java에서 읽은 예약 items
-- ARGV[2...]: 각 상품에 반환할 수량
--
--  1: 해제 성공
--  0: 이미 해제됨
-- -1: 예약 없음
-- -2: 재고 키 없음
-- -3: 예약 내용 불일치
-- -4: 해제할 수 없는 상태
-- -5: 잘못된 데이터

--[[
   flow:
   예약 내용 재확인: Java가 읽은 값과 현재 저장된 값을 비교
   전체 검증 후 반환: 뒤쪽 상품에 문제가 있어도 앞쪽 상품을 먼저 증가시키지 않는다.
   재고 반환과 상태 변경을 함께 실행: 같은 결제의 해제 요청이 겹쳐도 한 번만 반환한다.
]]

if #KEYS < 2 or #KEYS ~= #ARGV then
    return -5
end

local reservationKey = KEYS[1]
local reservationType = redis.call('TYPE', reservationKey).ok

-- reservationType 검사
if reservationType == 'none' then
    return -1
end

if reservationType ~= 'hash' then
    return -5
end

-- Java 조회 후 예약 내용이 바뀌었는지 재검증
local storedItems = redis.call('HGET', reservationKey, 'items')

if storedItems ~= ARGV[1] then
    return -3
end


local status = redis.call('HGET', reservationKey, 'status')
-- 중복 해제는 추가 변환 없이 종료
if status == 'RELEASED' then
    return 0
end

-- ACTIVE 예약만 해제
if status ~= 'ACTIVE' then
    return -4
end

local seenKeys = {}
local itemParts = {}

-- 모든 상품 검증
for i = 2, #KEYS do
    local stockKey = KEYS[i]
    local quantityText = ARGV[i]
    local quantity = tonumber(quantityText)

    -- 같은 상품 키가 두번 전달되는 것을 방지
    if seenKeys[stockKey] then
        return -5
    end
seenKeys[stockKey] = true

local productId =
    string.match(stockKey, '^inventory:available:([1-9]%d*)$')

if not productId
        or not string.match(quantityText, '^[1-9]%d*$')
        or not quantity
        or quantity > 2147483647 then
        return -5
    end
    itemParts[#itemParts + 1] = productId .. ':' .. quantityText

    local stockType = redis.call('TYPE', stockKey).ok

    if stockType == 'none' then
        return -2
    end

    if stockType ~= 'string' then
        return -5
    end

    local stockText = redis.call('GET', stockKey)
    local stock = tonumber(stockText)

    if (stockText ~= '0'
        and not string.match(stockText, '^[1-9]%d*$'))
        or not stock
        or stock > 2147483647
        or stock + quantity > 2147483647 then
        return -5
    end
end

-- 전달된 키·수량이 실제 예약 상품 목록과 같은지 확인
if table.concat(itemParts, ',') ~= storedItems then
    return -3
end

-- 전체 검증 후 재고 반환
for i = 2, #KEYS do
    redis.call('INCRBY', KEYS[i], ARGV[i])
end

redis.call('HSET', reservationKey, 'status', 'RELEASED')

return 1