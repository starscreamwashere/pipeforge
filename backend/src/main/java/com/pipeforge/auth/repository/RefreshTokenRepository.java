package com.pipeforge.auth.repository;

import com.pipeforge.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /** Revokes every still-active refresh token for a user (logout / logout-all). */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true "
            + "where r.user.id = :userId and r.revoked = false")
    int revokeAllActiveForUser(@Param("userId") UUID userId);
}
