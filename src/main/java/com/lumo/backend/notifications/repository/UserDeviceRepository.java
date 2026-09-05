package com.lumo.backend.notifications.repository;

import com.lumo.backend.notifications.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findByUserIdAndIsActiveTrue(String userId);

    List<UserDevice> findByUserIdInAndIsActiveTrue(Collection<String> userIds);

    List<UserDevice> findByUserTypeAndIsActiveTrue(String userType);

    List<UserDevice> findByIsActiveTrue();

    Optional<UserDevice> findByUserIdAndDeviceId(String userId, String deviceId);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false, d.updatedAt = :now WHERE d.fcmToken = :token")
    void deactivateToken(@Param("token") String token, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserDevice d SET d.isActive = false, d.updatedAt = :now WHERE d.fcmToken IN :tokens")
    void deactivateTokens(@Param("tokens") Collection<String> tokens, @Param("now") Instant now);
}
