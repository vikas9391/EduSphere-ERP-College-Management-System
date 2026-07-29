package com.collegeerp.Backend.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminPasswordResetTokenRepository extends JpaRepository<SuperAdminPasswordResetToken, Long> {
    Optional<SuperAdminPasswordResetToken> findByToken(String token);
}
