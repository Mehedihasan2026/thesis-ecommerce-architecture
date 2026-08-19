package com.thesis.microservices.payment.repository;

import com.thesis.microservices.payment.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
}
