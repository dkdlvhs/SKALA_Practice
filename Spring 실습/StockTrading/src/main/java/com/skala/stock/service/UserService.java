package com.skala.stock.service;

import com.skala.stock.dto.UserAssetSummaryDto;
import com.skala.stock.dto.UserDto;
import com.skala.stock.entity.Portfolio;
import com.skala.stock.entity.User;
import com.skala.stock.repository.PortfolioRepository;
import com.skala.stock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다: " + userDto.getUsername());
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다: " + userDto.getEmail());
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .password(userDto.getPassword())
                .email(userDto.getEmail())
                .balance(userDto.getBalance())
                .build();

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
        return convertToDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UserAssetSummaryDto getAssetSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        List<UserAssetSummaryDto.TopHoldingDto> topHoldings = portfolios.stream()
                .map(this::convertToTopHolding)
                .sorted(Comparator.comparing(UserAssetSummaryDto.TopHoldingDto::getTotalValue).reversed())
                .limit(3)
                .collect(Collectors.toList());

        long totalHoldingValue = topHoldings.stream()
                .mapToLong(UserAssetSummaryDto.TopHoldingDto::getTotalValue)
                .sum();

        long totalAssets = user.getBalance() + totalHoldingValue;

        topHoldings.forEach(holding -> {
            double ratio = totalHoldingValue == 0 ? 0 : (holding.getTotalValue() * 100.0) / totalHoldingValue;
            holding.setRatio(Math.round(ratio*100.0)/100.0);
        });

        return UserAssetSummaryDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .balance(user.getBalance())
                .totalAssets(totalAssets)
                .holdingCount(portfolios.size())
                .topHoldings(topHoldings)
                .build();
    }

    private UserAssetSummaryDto.TopHoldingDto convertToTopHolding(Portfolio portfolio) {
        var stock = portfolio.getStock();
        long totalValue = portfolio.getQuantity() * stock.getCurrentPrice();
        long profitLoss = totalValue - (portfolio.getQuantity() * portfolio.getAveragePrice());
        double returnRate = portfolio.getAveragePrice() == 0 ? 0 : ((stock.getCurrentPrice() - portfolio.getAveragePrice()) * 100.0) / portfolio.getAveragePrice();

        return UserAssetSummaryDto.TopHoldingDto.builder()
                .stockId(stock.getId())
                .stockCode(stock.getCode())
                .stockName(stock.getName())
                .quantity(portfolio.getQuantity())
                .averagePrice(portfolio.getAveragePrice())
                .currentPrice(stock.getCurrentPrice())
                .totalValue(totalValue)
                .profitLoss(profitLoss)
                .returnRate(returnRate)
                .build();
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build();
    }
}
