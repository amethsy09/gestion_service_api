package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.request.SpecialtyRequest;
import com.example.gestionservice.dto.response.SpecialtyResponse;
import com.example.gestionservice.entity.Specialty;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SpecialtyMapper {

    SpecialtyResponse toResponse(Specialty entity);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resources", ignore = true)
    Specialty toEntity(SpecialtyRequest request);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resources", ignore = true)
    void updateEntity(SpecialtyRequest request, @MappingTarget Specialty entity);
}
