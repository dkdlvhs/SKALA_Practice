package com.skala.stock.service;

import com.skala.stock.entity.Portfolio;
import com.skala.stock.entity.Stock;
import com.skala.stock.entity.Transaction;
import com.skala.stock.entity.User;
import com.skala.stock.repository.PortfolioRepository;
import com.skala.stock.repository.StockRepository;
import com.skala.stock.repository.TransactionRepository;
import com.skala.stock.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StockRepository stockRepository;

    @Test
    void deleteUserById_shouldRemoveUserAndRelatedRecords() {
        String uniqueUsername = "delete-test-" + UUID.randomUUID();

        User user = userRepository.save(User.builder()
                .username(uniqueUsername)
                .password("pw")
                .email(uniqueUsername + "@example.com")
                .balance(1000L)
                .build());

        Stock stock = stockRepository.save(Stock.builder()
                .code("999999")
                .name("테스트주식")
                .currentPrice(1000L)
                .previousPrice(900L)
                .build());

        portfolioRepository.save(Portfolio.builder()
                .user(user)
                .stock(stock)
                .quantity(10L)
                .averagePrice(1000L)
                .build());

        transactionRepository.save(Transaction.builder()
                .user(user)
                .stock(stock)
                .type(Transaction.TransactionType.BUY)
                .quantity(10L)
                .price(1000L)
                .totalAmount(10000L)
                .build());

        userService.deleteUserById(user.getId());

        assertFalse(userRepository.findById(user.getId()).isPresent());
        assertTrue(portfolioRepository.findByUserId(user.getId()).isEmpty());
        assertTrue(transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId()).isEmpty());
    }
}
