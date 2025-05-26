package com.dataservices.service;

import com.dataservices.model.User;
import com.dataservices.repository.UserRepository;
import com.dataservices.tenant.TenantContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "users", key = "#root.methodName + ':' + T(com.dataservices.tenant.TenantContext).getTenantId()")
    public List<User> getAllUsers() {
        return userRepository.findByTenantId(TenantContext.getTenantId());
    }

    @Cacheable(value = "users", key = "'user:' + T(com.dataservices.tenant.TenantContext).getTenantId() + ':' + #id")
    public Optional<User> getUserById(Long id) {
        return userRepository.findByTenantIdAndId(TenantContext.getTenantId(), id);
    }

    public User upsertUser(Long id, User userDetails) {
        return userRepository.save(User.builder()
                .id(id)
                .tenantId(TenantContext.getTenantId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .build());
    }

    public boolean deleteUser(Long id) {
        return userRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
                .map(user -> {
                    userRepository.delete(user);
                    return true;
                }).orElse(false);
    }
}