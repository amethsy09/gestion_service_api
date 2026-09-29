package com.example.gestionservice.repository;

import com.example.gestionservice.entity.Responsible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponsibleRepository extends JpaRepository<Responsible, UUID> {

    Optional<Responsible> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    List<Responsible> findByActiveTrue();

    Page<Responsible> findAll(Pageable pageable);
}
