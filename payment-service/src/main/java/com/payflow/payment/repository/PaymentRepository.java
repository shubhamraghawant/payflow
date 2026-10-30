package com.payflow.payment.repository;

import com.payflow.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Payment entity persistence operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
