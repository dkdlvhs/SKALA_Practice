package com.skala.shopapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shopapi.data.table.Customer;

public interface CustomerRepository extends JpaRepository<Customer, String>{
    
}
