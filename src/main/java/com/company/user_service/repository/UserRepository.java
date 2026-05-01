
package com.company.user_service.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.company.user_service.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
List<User> findBySellerStatus(String sellerStatus);
    Optional<User> findByEmail(String email);
    
}