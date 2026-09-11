package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.request.ServiceCatalogRequest;
import com.example.gestionservice.dto.response.ServiceCatalogResponse;
import com.example.gestionservice.entity.ServiceCatalog;
import org.mapstruct.*;

/**
 * Mapper MapStruct pour ServiceCatalog.
 * componentModel = "spring" injecté via la propriété Maven -Amapstruct.defaultComponentModel=spring
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceCatalogMapper {

    ServiceCatalogResponse toResponse(ServiceCatalog entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "serviceRequests", ignore = true)
    ServiceCatalog toEntity(ServiceCatalogRequest request);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "serviceRequests", ignore = true)
    void updateEntity(ServiceCatalogRequest request, @MappingTarget ServiceCatalog entity);
}
