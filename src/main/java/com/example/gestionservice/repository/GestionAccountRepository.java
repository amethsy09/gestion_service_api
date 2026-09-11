package com.example.gestionservice.repository;

import com.example.gestionservice.entity.GestionAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GestionAccountRepository extends JpaRepository<GestionAccount, UUID> {

    Optional<GestionAccount> findByTelephone(String telephone);

    boolean existsByTelephone(String telephone);
}
