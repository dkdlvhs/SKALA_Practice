package com.skala.shopapi.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.shopapi.common.Response;
import com.skala.shopapi.data.dto.ProductCreateRequest;
import com.skala.shopapi.data.table.Product;
import com.skala.shopapi.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품 관리", description = "상품 조회, 등록, 수정 및 삭제 관련 API")
public class ProductController {
    private final ProductService productService;

    
    // 전체 상품 목록 조회 API
    @Operation(summary = "상품 목록 조회", description = "전체 상품 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/list")
    public Response getAllProducts(@RequestParam(defaultValue = "0") Integer offset,
                                @RequestParam(defaultValue = "10") Integer count) {

        return productService.getAllProducts(offset, count);
    }

    // 개별 상품 상세 조회 API
    @Operation(summary = "상품 상세 조회", description = "상품 ID로 특정 상품의 정보를 조회합니다.")
    @GetMapping("/{id}")
    public Response getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // 상품 등록 API
    @Operation(summary = "상품 등록", description = "새로운 상품 정보를 등록합니다.")
    @PostMapping
    public Response createProduct(@RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }
    
    // 상품 정보 수정 API
    @Operation(summary = "상품 정보 수정", description = "기존 상품 정보를 수정합니다.")
    @PutMapping
    public Response updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }
    
    // 상품 삭제 API
    @Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.")
    @DeleteMapping
    public Response deleteProduct(@RequestParam Long id) {
        return productService.deleteProduct(id);
    }
}
