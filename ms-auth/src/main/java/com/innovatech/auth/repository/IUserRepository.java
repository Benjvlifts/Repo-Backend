package com.innovatech.auth.repository;

import com.innovatech.auth.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz de dominio para usuarios. 
 * Solo contiene métodos personalizados.
 */
public interface IUserRepository {
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
    List<User> findByActive(boolean active);
    boolean existsByEmail(String email);
}