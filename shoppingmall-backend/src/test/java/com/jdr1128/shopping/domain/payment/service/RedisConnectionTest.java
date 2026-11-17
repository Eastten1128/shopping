package com.jdr1128.shopping.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private StockReservationService stockReservationService;

    @Test
    @DisplayName("redis 연결테스트")
    void redisConnection() {
        Long productId = -999L;
        String key = "inventory:available:" + productId;

        try{
            redisTemplate.opsForValue().set(key, "10");

            String stock =
                    stockReservationService.getAvailableStock(productId);

            assertThat(stock).isEqualTo("10");
        }finally {
            redisTemplate.delete(key);
        }
    }

}