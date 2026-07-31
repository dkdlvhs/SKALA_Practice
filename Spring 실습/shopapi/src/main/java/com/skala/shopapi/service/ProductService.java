package com.skala.shopapi.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.skala.shopapi.common.PagedList;
import com.skala.shopapi.common.Response;
import com.skala.shopapi.data.dto.ProductCreateRequest;
import com.skala.shopapi.data.table.Product;
import com.skala.shopapi.exception.Error;
import com.skala.shopapi.exception.ParameterException;
import com.skala.shopapi.exception.ResponseException;
import com.skala.shopapi.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // 전체 상품 목록 조회
    public Response getAllProducts(int offset, int count) {

        // Pagable 객체 생성
        Pageable pageable = PageRequest.of(offset, count, Sort.by(Sort.Direction.ASC, "id"));

        // 페이지 단위 데이터 조회
        Page<Product> page = productRepository.findAll(pageable);

        // 결과를 PagedList 객체로 가공
        PagedList<Product> pagedList = PagedList.<Product>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return Response.builder()
                .success(true)
                .message("상품 목록 조회 성공")
                .data(pagedList)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 개별 상품 상세 조회
    public Response getProductById(Long id) {
        //  ID로 상품 조회, Optinal로 존재 여부 확인
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다."));

        return Response.builder()
                .success(true)
                .message("상품 상세 조회 성공")
                .data(product)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 상품 등록(생성)
    public Response createProduct(ProductCreateRequest request) {
        // 입력값 검증
        validateProduct(request);

        // 이름 중복 체크
        String productName = request.getProductName().trim();
        Optional<Product> existing = productRepository.findByProductName(productName);
        if (existing.isPresent()) {
            throw new ResponseException(Error.DATADUPLICATED, "이미 존재하는 상품명입니다.");
        }

        // 신규 Product 객체 생성, ID는 JPA가 자동으로 설정
        Product product = new Product();
        product.setProductName(productName);
        product.setProductPrice(request.getProductPrice());

        Product saved = productRepository.save(product);

        return Response.builder()
                .success(true)
                .message("상품 등록 성공")
                .data(saved)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 상품 정보 수정
    public Response updateProduct(Product product) {
        // 입력값 검증
        validateProduct(product);

        // 해당 ID의 Product가 존재하는지 확인
        Product existing = productRepository.findById(product.getId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 수정(저장)
        existing.setProductName(product.getProductName().trim());
        existing.setProductPrice(product.getProductPrice());
        Product updated = productRepository.save(existing);

        return Response.builder()
                .success(true)
                .message("상품 수정 성공")
                .data(updated)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 상품 삭제
    public Response deleteProduct(Long id) {
        // 해당 ID의 Product가 존재하는지 확인
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 삭제
        productRepository.deleteById(id);

        return Response.builder()
                .success(true)
                .message("상품 삭제 성공")
                .data(existing)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 입력값 검증
    private void validateProduct(Product product) {
        List<String> invalidFields = new ArrayList<>();

        // product null 확인 -> 아래 검증을 위한 확인
        if (product == null) {
            throw new ParameterException(List.of("product"));
        }

        // productName 비어있음
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            invalidFields.add("productName");
        }

        // price <= 0
        if (product.getProductPrice() <= 0) {
            invalidFields.add("productPrice");
        }
        
        // 검증에서 발생한 문제가 하나라도 있다면 예외 발생
        if (!invalidFields.isEmpty()) {
            throw new ParameterException(invalidFields);
        }
    }

    // 상품 등록 요청값 검증
    private void validateProduct(ProductCreateRequest request) {
        if (request == null) {
            throw new ParameterException(List.of("product"));
        }

        List<String> invalidFields = new ArrayList<>();
        if (request.getProductName() == null || request.getProductName().trim().isEmpty()) {
            invalidFields.add("productName");
        }
        if (request.getProductPrice() <= 0) {
            invalidFields.add("productPrice");
        }

        if (!invalidFields.isEmpty()) {
            throw new ParameterException(invalidFields);
        }
    }
}
