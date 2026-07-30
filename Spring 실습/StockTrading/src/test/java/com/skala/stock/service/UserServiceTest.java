package com.skala.stock.service;

import com.skala.stock.dto.UserAssetSummaryDto;
import com.skala.stock.entity.Portfolio;
import com.skala.stock.entity.Stock;
import com.skala.stock.entity.User;
import com.skala.stock.repository.PortfolioRepository;
import com.skala.stock.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getAssetSummary_shouldSummarizeAssetsAndTopHoldings() {
        User user = User.builder()
                .id(1L)
                .username("tester")
                .password("1234")
                .email("tester@example.com")
                .balance(10000L)
                .build();

        Stock apple = Stock.builder()
                .id(10L)
                .code("AAPL")
                .name("Apple")
                .currentPrice(200L)
                .previousPrice(180L)
                .build();

        Stock microsoft = Stock.builder()
                .id(11L)
                .code("MSFT")
                .name("Microsoft")
                .currentPrice(300L)
                .previousPrice(280L)
                .build();

        Stock google = Stock.builder()
                .id(12L)
                .code("GOOG")
                .name("Google")
                .currentPrice(100L)
                .previousPrice(90L)
                .build();

        Portfolio applePortfolio = Portfolio.builder()
                .user(user)
                .stock(apple)
                .quantity(5L)
                .averagePrice(150L)
                .build();

        Portfolio microsoftPortfolio = Portfolio.builder()
                .user(user)
                .stock(microsoft)
                .quantity(4L)
                .averagePrice(250L)
                .build();

        Portfolio googlePortfolio = Portfolio.builder()
                .user(user)
                .stock(google)
                .quantity(10L)
                .averagePrice(80L)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByUserId(1L)).thenReturn(List.of(applePortfolio, microsoftPortfolio, googlePortfolio));

        UserAssetSummaryDto summary = userService.getAssetSummary(1L);

        assertThat(summary.getBalance()).isEqualTo(10000L);
        assertThat(summary.getTotalAssets()).isEqualTo(13200L);
        assertThat(summary.getHoldingCount()).isEqualTo(3);
        assertThat(summary.getTopHoldings()).hasSize(3);
        assertThat(summary.getTopHoldings().get(0).getStockName()).isEqualTo("Microsoft");
        assertThat(summary.getTopHoldings().get(0).getRatio()).isCloseTo(37.5, org.assertj.core.data.Offset.offset(0.01));
        assertThat(summary.getTopHoldings().get(0).getReturnRate()).isCloseTo(20.0, org.assertj.core.data.Offset.offset(0.01));
    }
}
