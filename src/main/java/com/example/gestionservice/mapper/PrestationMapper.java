package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.response.PrestationResponse;
import com.example.gestionservice.entity.Prestation;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrestationMapper {

    @Mapping(target = "serviceRequestId", source = "serviceRequest.id")
    @Mapping(target = "responsibleId",    source = "responsible.id")
    @Mapping(target = "responsibleName",  expression = "java(entity.getResponsible().getFirstName() + ' ' + entity.getResponsible().getLastName())")
    PrestationResponse toResponse(Prestation entity);
}
