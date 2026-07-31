package com.skala.shopapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.shopapi.data.table.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{

    Optional<Product> findByProductName(String name);

}
