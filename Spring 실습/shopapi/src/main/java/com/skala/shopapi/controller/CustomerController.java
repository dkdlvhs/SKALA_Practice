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
import com.skala.shopapi.data.dto.CustomerCreateRequest;
import com.skala.shopapi.data.dto.CustomerSession;
import com.skala.shopapi.data.dto.OrderRequest;
import com.skala.shopapi.data.table.Customer;
import com.skala.shopapi.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "고객 관리", description = "고객 등록, 조회, 로그인, 수정, 삭제 및 주문 관련 API")
public class CustomerController {
    private final CustomerService customerService;

    // 전체 고객 목록 조회 API
    @Operation(summary = "고객 목록 조회", description = "전체 고객 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/list")
    public Response getAllCustomers(@RequestParam(defaultValue = "0") int offset,
                                @RequestParam(defaultValue = "10") int count) {
        
        return customerService.getAllCustomers(offset, count);
        
    }

    // 단일 고객 상세 조회 API
    @Operation(summary = "고객 상세 조회", description = "고객 ID로 특정 고객의 정보를 조회합니다.")
    @GetMapping("/{customerId}")
    public Response getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId);
    }

    // 고객 등록
    @Operation(summary = "고객 등록", description = "새로운 고객 정보를 등록합니다.")
    @PostMapping
    public Response createCustomer(@RequestBody CustomerCreateRequest request) {
        return customerService.createCustomer(request);
    }

    // 고객 로그인
    @Operation(summary = "고객 로그인", description = "고객 ID와 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public Response loginCustomer(@RequestBody CustomerSession customerSession) {
        return customerService.loginCustomer(customerSession);
    }

    // 내 정보 조회
    @Operation(summary = "내 정보 조회", description = "JWT로 인증된 고객의 정보를 조회합니다.")
    @GetMapping("/me")
    public Response getMyInfo() {
        return customerService.getMyInfo();
    }
    
    // 고객 정보 수정
    @Operation(summary = "고객 정보 수정", description = "기존 고객 정보를 수정합니다.")
    @PutMapping
    public Response updateCustomer(@RequestBody Customer customer) {
        return customerService.updateCustomer(customer);
    }

    // 고객 삭제
    @Operation(summary = "고객 삭제", description = "고객 정보를 삭제합니다.")
    @DeleteMapping
    public Response deleteCustomer(@RequestParam String customerId) {
        return customerService.deleteCustomer(customerId);
    }

    // 고객 상품 주문
    @Operation(summary = "상품 주문", description = "고객이 원하는 상품을 주문합니다.")
    @PostMapping("/order")
    public Response placeOrder(@RequestBody OrderRequest order) {
        return customerService.placeOrder(order);
    }

    // 고객 주문 취소
    @Operation(summary = "주문 취소", description = "고객의 주문을 취소합니다.")
    @PostMapping("/cancel")
    public Response cancelOrder(@RequestBody OrderRequest order) {
        return customerService.cancelOrder(order);
    }
}
