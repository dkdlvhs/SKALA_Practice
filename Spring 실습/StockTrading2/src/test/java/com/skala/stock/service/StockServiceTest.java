package com.skala.stock.service;

import com.skala.stock.dto.StockDto;
import com.skala.stock.entity.Stock;
import com.skala.stock.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @Test
    void updateStock_shouldModifyExistingStock() {
        Stock saved = stockRepository.save(Stock.builder()
                .code("TEST01")
                .name("테스트주식")
                .currentPrice(1000L)
                .previousPrice(900L)
                .build());

        StockDto updated = stockService.updateStock(StockDto.builder()
                .id(saved.getId())
                .code("TEST01")
                .name("수정된주식")
                .currentPrice(1100L)
                .previousPrice(1000L)
                .build());

        assertEquals("수정된주식", updated.getName());
        assertEquals(1100L, updated.getCurrentPrice());
        assertEquals(1000L, updated.getPreviousPrice());
    }
}
