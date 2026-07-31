package com.skala.shopapi.common;

import org.springframework.stereotype.Component;

@Component
public class SessionHandler {
    private final ThreadLocal<AuthenticatedCustomer> currentCustomer = new ThreadLocal<>();

    public void setCurrentCustomer(String customerId, String role) {
        currentCustomer.set(new AuthenticatedCustomer(customerId, role));
    }

    public String getCurrentCustomerId() {
        AuthenticatedCustomer customer = currentCustomer.get();
        if (customer == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return customer.customerId();
    }

    public boolean isAdmin() {
        AuthenticatedCustomer customer = currentCustomer.get();
        return customer != null && "ADMIN".equals(customer.role());
    }

    public void clear() {
        currentCustomer.remove();
    }

    private record AuthenticatedCustomer(String customerId, String role) {
    }
}
