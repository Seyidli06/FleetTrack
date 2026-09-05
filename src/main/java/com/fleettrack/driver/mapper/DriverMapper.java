package com.fleettrack.driver.mapper;

import com.fleettrack.driver.dto.CreateDriverRequest;
import com.fleettrack.driver.dto.DriverResponse;
import com.fleettrack.driver.dto.DriverSummaryResponse;
import com.fleettrack.driver.dto.UpdateDriverRequest;
import com.fleettrack.driver.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DriverMapper {

    Driver toEntity(CreateDriverRequest request);

    DriverResponse toResponse(Driver driver);

    DriverSummaryResponse toSummaryResponse(Driver driver);

    void update(
            UpdateDriverRequest request,
            @MappingTarget Driver driver
    );
}