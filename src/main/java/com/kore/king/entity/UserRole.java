package com.kore.king.entity;

public enum UserRole {
    USER,           // Regular users
    EMPLOYEE_ADMIN, // Limited admin access (support, game IDs, transactions)
    MAIN_ADMIN      // Full system access (owner)
}
