package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.request.TaskRequest;
import com.example.gestionservice.dto.response.TaskResponse;
import com.example.gestionservice.entity.Task;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    @Mapping(target = "prestationId",  source = "prestation.id")
    @Mapping(target = "resourceId",    source = "resource.id")
    @Mapping(target = "resourceName",  expression = "java(entity.getResource() != null ? entity.getResource().getFirstName() + ' ' + entity.getResource().getLastName() : null)")
    TaskResponse toResponse(Task entity);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "prestation",  ignore = true)
    @Mapping(target = "resource",    ignore = true)
    @Mapping(target = "status",      ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Task toEntity(TaskRequest request);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "prestation",  ignore = true)
    @Mapping(target = "resource",    ignore = true)
    @Mapping(target = "status",      ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    void updateEntity(TaskRequest request, @MappingTarget Task entity);
}
