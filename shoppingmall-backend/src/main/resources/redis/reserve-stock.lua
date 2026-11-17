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
local stockKey = KEYS[1] --구매가능 재고 키
local reservationKey = KEYS[2] -- 결제별 예약 키

local productId = ARGV[1] -- 상품Id
local quantity = tonumber(ARGV[2]) --예약 상품 수량

-- 변경 전에 입력 검증
-- 재고 수량 검증
if not quantity
    or quantity <=0
    or quantity > 2147483647
    or quantity ~= math.floor(quantity) then
        return -5
end

-- 같은 결제 ID의 예약이 있는지 먼저 확인
-- redis.call: redis 명령 실행 함수
-- redis.call('명령어', 키, 추가 인자)
-- 같은 결제가 이미 예약했다면 다시 차감하지 않기 위해 검사
if redis.call('EXISTS', reservationKey) == 1 then -- 해당 결제에 대한 예약 키가 존재(1)하면
    -- 이전 예약 상품Id, 수량, 상태 저장
    local oldProductId = redis.call('HGET', reservationKey, 'productId')
    local oldQuantity = redis.call('HGET', reservationKey, 'quantity')
    local status = redis.call('HGET', reservationKey, 'status')

    -- productId가 다르거나 재고 수가 다르면 예약 내용 불일치
    if oldProductId ~= productId
        or tonumber(oldQuantity) ~= quantity then
        return -3
    end

    -- 이미 ACTIVE 된 결제인 경우
    if status ~= 'ACTIVE' then
        return -4
    end

    -- 이미 같은 예약이 있으므로 추가 차감하지 않음
    return 0
end

-- 구매 가능 수량 조회
local stockValue = redis.call('GET', stockKey)

-- stockValue가 존재하지 않는다면 -2
if not stockValue then
    return -2
end

-- 숫자로 변환
local stock = tonumber(stockValue)

-- s
if not string.match(stockValue, '^%d+$')
    or not stock
    or stock > 2147483647 then
    return -5
end

-- 재고 수 부족
if stock < quantity then
    return -1
end

-- 모든 검증 후 변경

-- DECRBY: 구매 가능 수량 차감
redis.call('DECRBY', stockKey, ARGV[2])

-- 예약 기록 저장
-- 'HSET', 키, 필드1 - 값1, 필드2 - 값2, ...
redis.call('HSET', reservationKey,
    'productId', productId,
    'quantity', ARGV[2],
    'status', 'ACTIVE'
    )
-- 신규 예약 성공
return 1