package com.fleettrack.location.dto;

import java.util.List;

public record VehicleLocationHistoryResponse(
        List<VehicleLocationResponse> items,
        VehicleLocationCursor nextCursor,
        boolean hasNext
) {
}