package com.example.gestionservice.repository;

import com.example.gestionservice.entity.PrestationResource;
import com.example.gestionservice.enums.ResourceAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrestationResourceRepository extends JpaRepository<PrestationResource, UUID> {

    List<PrestationResource> findByPrestationId(UUID prestationId);

    List<PrestationResource> findByResourceId(UUID resourceId);

    Optional<PrestationResource> findByPrestationIdAndResourceId(UUID prestationId, UUID resourceId);

    boolean existsByPrestationIdAndResourceId(UUID prestationId, UUID resourceId);

    List<PrestationResource> findByPrestationIdAndStatus(UUID prestationId, ResourceAssignmentStatus status);

    /** Vérifie qu'une ressource est bien affectée à une prestation (statut actif). */
    @Query("""
            SELECT COUNT(pr) > 0 FROM PrestationResource pr
            WHERE pr.prestation.id = :prestationId
              AND pr.resource.id   = :resourceId
              AND pr.status IN ('ASSIGNED', 'ACTIVE')
            """)
    boolean isResourceAssignedToPrestation(
            @Param("prestationId") UUID prestationId,
            @Param("resourceId") UUID resourceId);
}
