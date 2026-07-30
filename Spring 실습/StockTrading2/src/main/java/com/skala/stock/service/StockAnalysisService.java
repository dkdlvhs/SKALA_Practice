package com.skala.stock.service;

import com.skala.stock.dto.PortfolioDto;
import com.skala.stock.dto.TotalAssetsDto;
import com.skala.stock.dto.TotalReturnRateDto;
import com.skala.stock.dto.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAnalysisService {

    private final PortfolioService portfolioService;
    private final TransactionService transactionService;
    private final UserService userService;

    public List<PortfolioDto> getUserPortfolioProfitLoss(Long userId) {
        return portfolioService.getUserPortfolio(userId);
    }

    public TransactionDto getTransactionDetail(Long userId, Long transactionId) {
        return transactionService.getTransactionDetail(userId, transactionId);
    }

    public List<TransactionDto> getUserTransactionsByStock(Long userId, Long stockId) {
        return transactionService.getUserTransactionsByStock(userId, stockId);
    }

    public TotalAssetsDto getUserTotalAssets(Long userId) {
        var userDto = userService.getUserById(userId);
        List<PortfolioDto> portfolios = portfolioService.getUserPortfolio(userId);

        long totalPortfolioValue = portfolios.stream()
                .mapToLong(PortfolioDto::getTotalValue)
                .sum();

        long totalAssets = userDto.getBalance() + totalPortfolioValue;

        return TotalAssetsDto.builder()
                .userId(userId)
                .totalAssets(totalAssets)
                .build();
    }

    public TotalReturnRateDto getUserTotalReturnRate(Long userId) {
        List<PortfolioDto> portfolios = portfolioService.getUserPortfolio(userId);

        long totalPortfolioValue = portfolios.stream()
                .mapToLong(PortfolioDto::getTotalValue)
                .sum();

        long totalCost = portfolios.stream()
                .mapToLong(portfolio -> portfolio.getAveragePrice() * portfolio.getQuantity())
                .sum();

        double totalReturnRate = 0.0;
        if (totalCost > 0) {
            long profitLoss = totalPortfolioValue - totalCost;
            totalReturnRate = BigDecimal.valueOf((double) profitLoss)
                    .divide(BigDecimal.valueOf(totalCost), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100.0))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return TotalReturnRateDto.builder()
                .userId(userId)
                .totalReturnRate(totalReturnRate)
                .build();
    }
}
