package com.verasotelomercedes.ordermanagement.customer.domain;

public class Customer {
    private final Long id;
    private final String name;
    private final String email;

    public Customer(String name, String email) {
        this(null, name, email);
    }

    public Customer(Long id, String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
