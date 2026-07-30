package com.skala.stock.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAssetSummaryDto {

    private Long userId;
    private String username;
    private Long balance;
    private Long totalAssets;
    private int holdingCount;
    private List<TopHoldingDto> topHoldings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopHoldingDto {
        private Long stockId;
        private String stockCode;
        private String stockName;
        private Long quantity;
        private Long averagePrice;
        private Long currentPrice;
        private Long totalValue;
        private Long profitLoss;
        private double ratio;
        private double returnRate;
    }
}
