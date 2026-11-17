-- KEYS[1]: 구매 가능 재고 키
-- KEYS[2]: 결제별 예약 키
--
-- ARGV[1]: 상품 ID
-- ARGV[2]: 예약 수량
--
-- 반환값:
-- 1: 신규 예약 성공
-- 0: 동일 예약이 이미 존재
-- -1: 재고 부족
-- -2: 재고 키 없음
-- -3: 같은 결제 ID의 예약 내용 불일치
-- -4: 이미 확정되거나 헤제된 예약
-- -5: 잘못된 수량 또는 재고 데이터

-- Redis가 Lua 스크립트를 원자적으로 실행하므로, 같은 결제 요청이 동시에 들어와도 하나가 예약을 기록한 뒤 다른 요청이 이를 확인
-- local stockKey = KEYS[1] --구매가능 재고 키
-- local reservationKey = KEYS[2] -- 결제별 예약 키
-- local productId = ARGV[1] -- 상품Id
-- local quantity = tonumber(ARGV[2]) --예약 상품 수량

if #KEYS < 2 or #KEYS ~= #ARGV then
    return -5
end

local reservationKey = KEYS[1]
local items = ARGV[1]

-- 예약 키의 자료형 확인
local reservationType = redis.call('TYPE', reservationKey).ok
if reservationType ~= 'none' and reservationType ~= 'hash'
    then return -5
end

-- 기존 예약 확인
if reservationType == 'hash' then
    local oldItems = redis.call('HGET', reservationKey, 'items')

    local status = redis.call('HGET', reservationKey, 'status')

    if oldItems ~= items then
        return -3
    end

    if status ~= 'ACTIVE' then
        return -4
    end
    return 0
end

-- 모든 상품 검증
for i = 2, #KEYS do
    local quantityText = ARGV[i] -- 각 상품 개수 String(quantityText)
    local quantity = tonumber(quantityText) -- 각 상품 개수 quantity = number(quantityText)

    -- 중요!!: quantityText가 1이상의 올바른 양의 정수 형태가 아닌경우
    if not string.match(quantityText, '^[1-9]%d*$')
        or not quantity -- 수량이 없는 경우
        or quantity > 2147483647 then --overflow
        return -5
    end

    -- 중요!!: Redis의 TYPE 명령이나 상태 응답을 반환하는 명령어는 테이블 (예:{ ok = "hash"}) 반환
    -- stockType은 재고의 type을 받아온다.
    local stockType = redis.call('TYPE', KEYS[i]).ok

    -- 재고 값이 없는 경우
    if stockType == 'none' then
        return -2
    end
    -- 문자열이 아닌 경우
    if stockType ~= 'string' then
        return -5
    end

-- 검증 완료 시 KEYS[i]의 재고값을 받아온다.
local stockText = redis.call('GET', KEYS[i])
local stock = tonumber(stockText)


 -- DECRBY가 처리할 수 있는 정수 문자열인지 확인
    if (stockText ~= '0' --재고가 0개
        and not string.match(stockText, '^[1-9]%d*$')) -- 1이상의 양의 정수가 아니거나
        or not stock    -- 재고가 존재하지않거나
        or stock > 2147483647 then -- 오버플로우
        return -5
    end

    -- 실제 재고가 요청 주문량보다 많은 경우
    if stock < quantity then
        return -1
    end
end

-- 검증 후 모든 상품이 충분할 때만 전체 차감이 원자적으로 실행된다.
for i = 2, #KEYS do
    redis.call('DECRBY', KEYS[i], ARGV[i]) -- 해당 Key의 값에 ARGV[i] 만큼 감소
end

-- 5. 상품 목록 전체를 예약에 저장
redis.call('HSET', reservationKey,
    'items', items,
    'status', 'ACTIVE'
)

-- 신규 예약 성공
return 1