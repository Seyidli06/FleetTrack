package com.fleettrack.vehicle.mapper;

import com.fleettrack.vehicle.dto.CreateVehicleRequest;
import com.fleettrack.vehicle.dto.UpdateVehicleRequest;
import com.fleettrack.vehicle.dto.VehicleResponse;
import com.fleettrack.vehicle.dto.VehicleSummaryResponse;
import com.fleettrack.vehicle.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface VehicleMapper {

    Vehicle toEntity(CreateVehicleRequest request);

    VehicleResponse toResponse(Vehicle vehicle);

    VehicleSummaryResponse toSummaryResponse(Vehicle vehicle);

    void update(
            UpdateVehicleRequest request,
            @MappingTarget Vehicle vehicle
    );
}