package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.request.ResponsibleRequest;
import com.example.gestionservice.dto.response.ResponsibleResponse;
import com.example.gestionservice.entity.Responsible;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResponsibleMapper {

    ResponsibleResponse toResponse(Responsible entity);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "prestations",ignore = true)
    Responsible toEntity(ResponsibleRequest request);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "prestations",ignore = true)
    void updateEntity(ResponsibleRequest request, @MappingTarget Responsible entity);
}
