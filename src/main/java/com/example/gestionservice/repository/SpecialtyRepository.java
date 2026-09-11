package com.example.gestionservice.repository;

import com.example.gestionservice.entity.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {

    Optional<Specialty> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Page<Specialty> findAll(Pageable pageable);
}
