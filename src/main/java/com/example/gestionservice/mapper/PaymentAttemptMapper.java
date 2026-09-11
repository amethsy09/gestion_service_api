package com.example.gestionservice.mapper;

import com.example.gestionservice.dto.response.PaymentAttemptResponse;
import com.example.gestionservice.entity.PaymentAttempt;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentAttemptMapper {

    @Mapping(target = "serviceRequestId", source = "serviceRequest.id")
    PaymentAttemptResponse toResponse(PaymentAttempt entity);
}
