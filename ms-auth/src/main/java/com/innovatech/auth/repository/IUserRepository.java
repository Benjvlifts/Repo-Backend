package com.innovatech.auth.repository;

import com.innovatech.auth.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz de dominio para usuarios.
 * FIX Bug 2: se agregan save/findAll/findById para que AuthService
 * dependa SOLO de esta abstracción y nunca de JpaUserRepository.
 */
public interface IUserRepository {
    User save(User user);
    List<User> findAll();
    Optional<User> findById(Long id);
    boolean existsById(Long id);
    void deleteById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
    List<User> findByActive(boolean active);
    boolean existsByEmail(String email);
}