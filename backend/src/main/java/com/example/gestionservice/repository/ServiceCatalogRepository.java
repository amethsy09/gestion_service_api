package com.example.gestionservice.repository;

import com.example.gestionservice.entity.ServiceCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {

    Optional<ServiceCatalog> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    Page<ServiceCatalog> findAll(Pageable pageable);

    List<ServiceCatalog> findByActiveTrue();

    Page<ServiceCatalog> findByActiveTrue(Pageable pageable);
}
