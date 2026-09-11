package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.response.PrestationResourceResponse;
import com.example.gestionservice.entity.PrestationResource;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrestationResourceMapper {

    @Mapping(target = "prestationId",       source = "prestation.id")
    @Mapping(target = "resourceId",         source = "resource.id")
    @Mapping(target = "resourceFirstName",  source = "resource.firstName")
    @Mapping(target = "resourceLastName",   source = "resource.lastName")
    @Mapping(target = "resourceEmail",      source = "resource.email")
    @Mapping(target = "specialtyName",      source = "resource.specialty.name")
    PrestationResourceResponse toResponse(PrestationResource entity);
}
