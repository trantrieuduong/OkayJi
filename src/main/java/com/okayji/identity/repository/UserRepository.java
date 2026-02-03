package com.okayji.identity.repository;

import com.okayji.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
    User findByUsernameIgnoreCase(String username);
    Optional<User> findUserByIdOrUsername(String id, String username);
}
