package com.skala.shopapi.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.shopapi.common.JwtTokenProvider;
import com.skala.shopapi.common.PagedList;
import com.skala.shopapi.common.Response;
import com.skala.shopapi.common.SessionHandler;
import com.skala.shopapi.data.dto.CustomerCreateRequest;
import com.skala.shopapi.data.dto.CustomerSession;
import com.skala.shopapi.data.dto.LoginResponse;
import com.skala.shopapi.data.dto.OrderItemDto;
import com.skala.shopapi.data.dto.OrderRequest;
import com.skala.shopapi.data.table.Customer;
import com.skala.shopapi.data.table.OrderItem;
import com.skala.shopapi.data.table.Product;
import com.skala.shopapi.exception.Error;
import com.skala.shopapi.exception.ParameterException;
import com.skala.shopapi.exception.ResponseException;
import com.skala.shopapi.repository.CustomerRepository;
import com.skala.shopapi.repository.OrderItemRepository;
import com.skala.shopapi.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final SessionHandler sessionHandler;
    private final JwtTokenProvider jwtTokenProvider;

    // 전체 고객 목록 조회
    public Response getAllCustomers(int offset, int count) {
        // Pagable 객체 생성
        Pageable pageable = PageRequest.of(offset, count, Sort.by(Sort.Direction.ASC, "customerId"));

        // 페이지 단위 조회
        Page<Customer> page = customerRepository.findAll(pageable);

        // PagedList로 결과 변환
        PagedList<Customer> pagedList = PagedList.<Customer>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return Response.builder()
                .success(true)
                .message("고객 목록 조회 성공")
                .data(pagedList)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 단일 고객 및 상품 목록 조회
    @Transactional(readOnly = true)
    public Response getCustomerById(String customerId) {
        validateOwnerOrAdmin(customerId);

        // Customer 엔티티 존재 여부 검증
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        // 고객이 보유한 OrderList 조회
        List<OrderItem> orderItems = orderItemRepository.findByCustomer_CustomerId(customerId);

        // Stream API로 DTO 리스트 변환
        List<OrderItemDto> dtoList = orderItems.stream()
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .productPrice(item.getProduct().getProductPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return Response.builder()
                .success(true)
                .message("고객 및 상품 목록 조회 성공")
                .data(dtoList)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 고객 생성
    public Response createCustomer(CustomerCreateRequest request) {
        // 입력값 검증
        validateCustomerSession(request.getCustomerId(), request.getCustomerPassword());

        // 중복 아이디 체크
        if (customerRepository.existsById(request.getCustomerId())) {
            throw new ResponseException(Error.DATADUPLICATED, "이미 존재하는 고객 ID입니다.");
        }

        // Customer 객체 생성, 초기 적립 포인트와 일반 사용자 권한 설정
        Customer customer = new Customer();
        customer.setCustomerId(request.getCustomerId());
        customer.setCustomerPassword(request.getCustomerPassword());
        customer.setCustomerPoint(10000.0);
        customer.setRole("USER");

        Customer saved = customerRepository.save(customer);

        return Response.builder()
                .success(true)
                .message("고객 생성 성공")
                .data(saved)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 고객 로그인
    public Response loginCustomer(CustomerSession customerSession) {
        // 입력값 검증
        validateCustomerSession(customerSession.getCustomerId(), customerSession.getCustomerPassword());

        // ID로 조회
        Customer customer = customerRepository.findById(customerSession.getCustomerId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));
        // 비밀번호 검증
        if (!customer.getCustomerPassword().equals(customerSession.getCustomerPassword())) {
            throw new ResponseException(Error.NOT_AUTHENTICATED, "비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(customer.getCustomerId(), customer.getRole());
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .customerId(customer.getCustomerId())
                .role(customer.getRole())
                .build();
        return Response.builder()
                .success(true)
                .message("로그인 성공")
                .data(loginResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public Response getMyInfo() {
        // JWT로 인증된 현재 customerId 가져오기
        String currentCustomerId = sessionHandler.getCurrentCustomerId();

        // 고객 정보 조회
        Customer customer = customerRepository.findById(currentCustomerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        return Response.builder()
                .success(true)
                .message("내 정보 조회 성공")
                .data(customer)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 고객 정보 업데이트
    public Response updateCustomer(Customer customer) {

        // customerId, customerPoint 유효성 체크
        if (customer.getCustomerId() == null || customer.getCustomerPoint() < 0) {
            throw new ResponseException(Error.DATA_NOT_FOUND, "Customer not found");
        }
        validateOwnerOrAdmin(customer.getCustomerId());

        // customerId로 존재 확인
        Customer existing = customerRepository.findById(customer.getCustomerId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        // 포인트 업데이트
        existing.setCustomerPoint(customer.getCustomerPoint());

        // 저장
        Customer updated = customerRepository.save(existing);

        return Response.builder()
                .success(true)
                .message("고객 정보 수정 성공")
                .data(updated)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 고객 삭제
    public Response deleteCustomer(String customerId) {
        // customerId로 존재 확인
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        // 삭제
        customerRepository.deleteById(customerId);

        return Response.builder()
                .success(true)
                .message("고객 삭제 성공")
                .data(existing)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 상품 주문
    @Transactional
    public Response placeOrder(OrderRequest order) {

        // 현재 로그인된 customerId 가져오기
        String currentCustomerId = sessionHandler.getCurrentCustomerId();

        // customer 엔티티 조회 및 검증
        Customer customer = customerRepository.findById(currentCustomerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        // product 엔티티 조회 및 검증
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Product not found"));

        // 포인트 충분성 체크
        if (customer.getCustomerPoint() < product.getProductPrice() * order.getQuantity()) {
            throw new ResponseException(Error.INSUFFICIENTFUNDS, "포인트가 부족합니다.");
        }

        // 포인트 차감
        customer.setCustomerPoint(customer.getCustomerPoint() - product.getProductPrice() * order.getQuantity());
        customerRepository.save(customer);

        // OrderItem에 이미 주무한 상품이면 수량 추가, 없으면 신규 생성
        OrderItem existing = orderItemRepository.findByCustomerAndProduct(customer, product);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + order.getQuantity());
            orderItemRepository.save(existing);
        } else {
            OrderItem newItem = new OrderItem();
            newItem.setCustomer(customer);
            newItem.setProduct(product);
            newItem.setQuantity(order.getQuantity());
            orderItemRepository.save(newItem);
        }

        return Response.builder()
                .success(true)
                .message("주문 성공")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 주문 취소
    @Transactional
    public Response cancelOrder(OrderRequest order) {
        // 현재 로그인된 customerId 가져오기
        String currentCustomerId = sessionHandler.getCurrentCustomerId();

        // customer 엔티티 조회 및 검증
        Customer customer = customerRepository.findById(currentCustomerId)
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Customer not found"));

        // product 엔티티 조회 및 검증
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND, "Product not found"));

        // 취소할 orderItem 존재 확인
        OrderItem existing = orderItemRepository.findByCustomerAndProduct(customer, product);
        if (existing == null) {
            throw new ResponseException(Error.DATA_NOT_FOUND, "Order item not found");
        }

        // orderItem 보유수량 검증
        if (existing.getQuantity() < order.getQuantity()) {
            throw new ResponseException(Error.INSUFFICIENTQUANTITY, "취소 수량이 보유 수량보다 많습니다.");
        }

        // 수량 감소 또는 삭제
        existing.setQuantity(existing.getQuantity() - order.getQuantity());
        if (existing.getQuantity() == 0) {
            orderItemRepository.delete(existing);
        } else {
            orderItemRepository.save(existing);
        }

        // 취소 금액만큼 고객 포인트 증가
        double refund = product.getProductPrice() * order.getQuantity();
        customer.setCustomerPoint(customer.getCustomerPoint() + refund);
        customerRepository.save(customer);

        return Response.builder()
                .success(true)
                .message("주문 취소 성공")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 검증 로직
    private void validateCustomerSession(String customerId, String password) {
        List<String> invalidFields = new ArrayList<>();
        if (customerId == null || customerId.trim().isEmpty()) {
            invalidFields.add("customerId");
        }
        if (password == null || password.trim().isEmpty()) {
            invalidFields.add("customerPassword");
        }
        if (!invalidFields.isEmpty()) {
            throw new ParameterException(invalidFields);
        }
    }

    private void validateOwnerOrAdmin(String customerId) {
        if (!sessionHandler.isAdmin()
                && !sessionHandler.getCurrentCustomerId().equals(customerId)) {
            throw new ResponseException(Error.NOT_AUTHORIZED, "본인의 고객 정보만 접근할 수 있습니다.");
        }
    }
}
