package com.innovatech.auth.repository;

import com.innovatech.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Esta interfaz une la potencia de Spring Data (JpaRepository) 
 * con tus métodos personalizados (IUserRepository).
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, Long>, IUserRepository {
    // Se deja vacío porque hereda todo de ambos padres.
}