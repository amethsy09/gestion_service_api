package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.response.ServiceRequestResponse;
import com.example.gestionservice.entity.ServiceRequest;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceRequestMapper {

    @Mapping(target = "serviceCatalogId", source = "serviceCatalog.id")
    @Mapping(target = "serviceName",      source = "serviceCatalog.name")
    ServiceRequestResponse toResponse(ServiceRequest entity);
}
