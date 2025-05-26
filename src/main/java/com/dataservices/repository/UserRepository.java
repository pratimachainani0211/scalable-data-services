package com.dataservices.repository;

import com.dataservices.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    List<User> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.id = :id")
    Optional<User> findByTenantIdAndId(@Param("tenantId") String tenantId, @Param("id") Long id);
}