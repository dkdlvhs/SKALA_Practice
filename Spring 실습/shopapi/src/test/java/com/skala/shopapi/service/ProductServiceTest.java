package com.skala.shopapi.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skala.shopapi.data.dto.ProductCreateRequest;
import com.skala.shopapi.data.table.Product;
import com.skala.shopapi.exception.Error;
import com.skala.shopapi.exception.ParameterException;
import com.skala.shopapi.exception.ResponseException;
import com.skala.shopapi.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setProductName("Apple");
        product.setProductPrice(1000.0);
        createRequest = new ProductCreateRequest("Apple", 1000.0);
    }

    @Test
    void createProduct_shouldSaveWhenValid() {
        when(productRepository.findByProductName("Apple")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.createProduct(createRequest);

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowParameterExceptionWhenNameEmpty() {
        createRequest.setProductName(" ");

        ParameterException exception = assertThrows(
                ParameterException.class, () -> productService.createProduct(createRequest));

        assertEquals("productName", exception.getFields().get(0));
    }

    @Test
    void createProduct_shouldThrowResponseExceptionWhenDuplicateName() {
        when(productRepository.findByProductName("Apple")).thenReturn(Optional.of(product));

        ResponseException exception = assertThrows(
                ResponseException.class, () -> productService.createProduct(createRequest));

        assertEquals(Error.DATADUPLICATED, exception.getError());
    }

    @Test
    void getProductById_shouldThrowResponseExceptionWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseException exception = assertThrows(ResponseException.class, () -> productService.getProductById(99L));

        assertEquals(Error.DATA_NOT_FOUND, exception.getError());
    }
}
