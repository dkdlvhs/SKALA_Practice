package com.skala.stock.controller;

import com.skala.stock.dto.PortfolioDto;
import com.skala.stock.dto.TotalAssetsDto;
import com.skala.stock.dto.TotalReturnRateDto;
import com.skala.stock.dto.TransactionDto;
import com.skala.stock.service.StockAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "포트폴리오 분석", description = "포트폴리오 분석 API")
public class StockAnalysisController {

    private final StockAnalysisService stockAnalysisService;

    @GetMapping("/user/{userId}/portfolio-profit-loss")
    @Operation(summary = "포트폴리오 평가 손익 조회", description = "사용자의 포트폴리오별 평가 손익을 조회합니다")
    public ResponseEntity<List<PortfolioDto>> getPortfolioProfitLoss(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getUserPortfolioProfitLoss(userId));
    }

    @GetMapping("/user/{userId}/transactions/{transactionId}")
    @Operation(summary = "거래 내역 상세 조회", description = "특정 거래 내역의 상세 정보를 조회합니다")
    public ResponseEntity<TransactionDto> getTransactionDetail(
            @PathVariable Long userId,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(stockAnalysisService.getTransactionDetail(userId, transactionId));
    }

    @GetMapping("/user/{userId}/transactions/stock/{stockId}")
    @Operation(summary = "특정 주식 거래 내역 조회", description = "사용자의 특정 주식 거래 내역을 조회합니다")
    public ResponseEntity<List<TransactionDto>> getStockTransactionHistory(
            @PathVariable Long userId,
            @PathVariable Long stockId) {
        return ResponseEntity.ok(stockAnalysisService.getUserTransactionsByStock(userId, stockId));
    }

    @GetMapping("/user/{userId}/assets")
    @Operation(summary = "총 자산 조회", description = "사용자의 현금과 포트폴리오를 합한 총 자산을 조회합니다")
    public ResponseEntity<TotalAssetsDto> getTotalAssets(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getUserTotalAssets(userId));
    }

    @GetMapping("/user/{userId}/return-rate")
    @Operation(summary = "총 수익률 조회", description = "사용자의 전체 포트폴리오 총 수익률을 조회합니다")
    public ResponseEntity<TotalReturnRateDto> getTotalReturnRate(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getUserTotalReturnRate(userId));
    }
}


