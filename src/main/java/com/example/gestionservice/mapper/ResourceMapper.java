package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.request.ResourceRequest;
import com.example.gestionservice.dto.response.ResourceResponse;
import com.example.gestionservice.entity.Resource;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceMapper {

    @Mapping(target = "specialtyId",   source = "specialty.id")
    @Mapping(target = "specialtyName", source = "specialty.name")
    ResourceResponse toResponse(Resource entity);

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "specialty",          ignore = true)   // résolu manuellement dans le service
    @Mapping(target = "prestationResources",ignore = true)
    @Mapping(target = "tasks",              ignore = true)
    Resource toEntity(ResourceRequest request);

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "specialty",          ignore = true)
    @Mapping(target = "prestationResources",ignore = true)
    @Mapping(target = "tasks",              ignore = true)
    void updateEntity(ResourceRequest request, @MappingTarget Resource entity);
}
