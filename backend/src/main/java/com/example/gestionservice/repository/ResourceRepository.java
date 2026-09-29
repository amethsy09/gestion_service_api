package com.example.gestionservice.repository;

import com.example.gestionservice.entity.Resource;
import com.example.gestionservice.enums.ResourceAvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Page<Resource> findAll(Pageable pageable);

    List<Resource> findByActiveTrueAndAvailabilityStatus(ResourceAvailabilityStatus status);

    @Query("SELECT r FROM Resource r WHERE r.active = true AND r.availabilityStatus = 'AVAILABLE'")
    List<Resource> findAllAvailable();

    @Query("SELECT r FROM Resource r WHERE r.specialty.id = :specialtyId AND r.active = true")
    List<Resource> findBySpecialtyId(@Param("specialtyId") UUID specialtyId);
}
