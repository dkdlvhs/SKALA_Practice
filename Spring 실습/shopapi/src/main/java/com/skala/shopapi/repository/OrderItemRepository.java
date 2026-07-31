package com.skala.shopapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shopapi.data.table.Customer;
import com.skala.shopapi.data.table.OrderItem;
import com.skala.shopapi.data.table.Product;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByCustomer_CustomerId(String customerId);
    OrderItem findByCustomerAndProduct(Customer customer, Product product);

}
