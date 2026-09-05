package com.fleettrack.vehicle.service;

import com.fleettrack.common.cache.CacheNames;
import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.common.exception.DuplicateResourceException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.common.exception.StaleVersionException;
import com.fleettrack.vehicle.dto.CreateVehicleRequest;
import com.fleettrack.vehicle.dto.UpdateVehicleRequest;
import com.fleettrack.vehicle.dto.VehicleFilter;
import com.fleettrack.vehicle.dto.VehicleResponse;
import com.fleettrack.vehicle.dto.VehicleSummaryResponse;
import com.fleettrack.vehicle.entity.Vehicle;
import com.fleettrack.vehicle.mapper.VehicleMapper;
import com.fleettrack.vehicle.repository.VehicleRepository;
import com.fleettrack.vehicle.specification.VehicleSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Locale;
import java.util.Set;

@Service
public class VehicleService {

    private static final int MIN_MANUFACTURE_YEAR = 1886;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "id",
                    "make",
                    "model",
                    "manufactureYear",
                    "status",
                    "createdAt",
                    "updatedAt"
            );

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehicleMapper vehicleMapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Transactional
    public VehicleResponse create(
            CreateVehicleRequest request
    ) {
        CreateVehicleRequest normalized =
                normalize(request);

        validateManufactureYear(
                normalized.manufactureYear()
        );

        ensureVinAvailable(
                normalized.vin()
        );

        ensureLicensePlateAvailable(
                normalized.licensePlate()
        );

        Vehicle vehicle =
                vehicleMapper.toEntity(
                        normalized
                );

        Vehicle saved =
                vehicleRepository.saveAndFlush(
                        vehicle
                );

        return vehicleMapper.toResponse(
                saved
        );
    }

    @Cacheable(
            cacheNames = CacheNames.VEHICLE_BY_ID,
            key = "#id"
    )
    @Transactional(readOnly = true)
    public VehicleResponse getById(
            Long id
    ) {
        return vehicleMapper.toResponse(
                getVehicle(id)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleSummaryResponse> getAll(
            VehicleFilter filter,
            int page,
            int size,
            String sort
    ) {
        validatePagination(
                page,
                size
        );

        validateFilter(filter);

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        parseSort(sort)
                );

        Page<VehicleSummaryResponse> result =
                vehicleRepository
                        .findAll(
                                VehicleSpecification
                                        .withFilter(
                                                filter
                                        ),
                                pageable
                        )
                        .map(
                                vehicleMapper
                                        ::toSummaryResponse
                        );

        return PageResponse.from(
                result
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.VEHICLE_BY_ID,
            key = "#id"
    )
    @Transactional
    public VehicleResponse update(
            Long id,
            UpdateVehicleRequest request
    ) {
        Vehicle vehicle =
                getVehicle(id);

        ensureVersionMatches(
                vehicle,
                request.version()
        );

        UpdateVehicleRequest normalized =
                normalize(request);

        validateManufactureYear(
                normalized.manufactureYear()
        );

        ensureVinAvailableForUpdate(
                normalized.vin(),
                id
        );

        ensureLicensePlateAvailableForUpdate(
                normalized.licensePlate(),
                id
        );

        vehicleMapper.update(
                normalized,
                vehicle
        );

        vehicleRepository.flush();

        return vehicleMapper.toResponse(
                vehicle
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.VEHICLE_BY_ID,
            key = "#id"
    )
    @Transactional
    public void delete(
            Long id
    ) {
        Vehicle vehicle =
                getVehicle(id);

        vehicleRepository.delete(
                vehicle
        );

        vehicleRepository.flush();
    }

    private Vehicle getVehicle(
            Long id
    ) {
        return vehicleRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle with id "
                                        + id
                                        + " was not found"
                        )
                );
    }

    private void ensureVinAvailable(
            String vin
    ) {
        if (vehicleRepository
                .existsByVinIgnoreCase(vin)) {

            throw new DuplicateResourceException(
                    "Vehicle with VIN "
                            + vin
                            + " already exists"
            );
        }
    }

    private void ensureLicensePlateAvailable(
            String licensePlate
    ) {
        if (vehicleRepository
                .existsByLicensePlateIgnoreCase(
                        licensePlate
                )) {

            throw new DuplicateResourceException(
                    "Vehicle with license plate "
                            + licensePlate
                            + " already exists"
            );
        }
    }

    private void ensureVinAvailableForUpdate(
            String vin,
            Long id
    ) {
        if (vehicleRepository
                .existsByVinIgnoreCaseAndIdNot(
                        vin,
                        id
                )) {

            throw new DuplicateResourceException(
                    "Vehicle with VIN "
                            + vin
                            + " already exists"
            );
        }
    }

    private void ensureLicensePlateAvailableForUpdate(
            String licensePlate,
            Long id
    ) {
        if (vehicleRepository
                .existsByLicensePlateIgnoreCaseAndIdNot(
                        licensePlate,
                        id
                )) {

            throw new DuplicateResourceException(
                    "Vehicle with license plate "
                            + licensePlate
                            + " already exists"
            );
        }
    }

    private void ensureVersionMatches(
            Vehicle vehicle,
            Long requestedVersion
    ) {
        if (!vehicle.getVersion()
                .equals(requestedVersion)) {

            throw new StaleVersionException(
                    "Vehicle has been modified. "
                            + "Expected version "
                            + vehicle.getVersion()
                            + " but received "
                            + requestedVersion
            );
        }
    }

    private void validateManufactureYear(
            Integer year
    ) {
        int currentYear =
                Year.now().getValue();

        if (year < MIN_MANUFACTURE_YEAR) {
            throw new IllegalArgumentException(
                    "Manufacture year cannot be earlier than "
                            + MIN_MANUFACTURE_YEAR
            );
        }

        if (year > currentYear) {
            throw new IllegalArgumentException(
                    "Manufacture year cannot be in the future"
            );
        }
    }

    private void validateFilter(
            VehicleFilter filter
    ) {
        if (filter == null) {
            return;
        }

        Integer yearFrom =
                filter.yearFrom();

        Integer yearTo =
                filter.yearTo();

        if (yearFrom != null
                && yearFrom < MIN_MANUFACTURE_YEAR) {

            throw new IllegalArgumentException(
                    "yearFrom cannot be earlier than "
                            + MIN_MANUFACTURE_YEAR
            );
        }

        if (yearTo != null
                && yearTo < MIN_MANUFACTURE_YEAR) {

            throw new IllegalArgumentException(
                    "yearTo cannot be earlier than "
                            + MIN_MANUFACTURE_YEAR
            );
        }

        int currentYear =
                Year.now().getValue();

        if (yearFrom != null
                && yearFrom > currentYear) {

            throw new IllegalArgumentException(
                    "yearFrom cannot be in the future"
            );
        }

        if (yearTo != null
                && yearTo > currentYear) {

            throw new IllegalArgumentException(
                    "yearTo cannot be in the future"
            );
        }

        if (yearFrom != null
                && yearTo != null
                && yearFrom > yearTo) {

            throw new IllegalArgumentException(
                    "yearFrom cannot be greater than yearTo"
            );
        }

        if (filter.driverId() != null
                && filter.driverId() <= 0) {

            throw new IllegalArgumentException(
                    "driverId must be greater than zero"
            );
        }
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be zero or greater"
            );
        }

        if (size < 1
                || size > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }

    private Sort parseSort(
            String sort
    ) {
        if (sort == null
                || sort.isBlank()) {

            return Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );
        }

        String[] parts =
                sort.split(",");

        if (parts.length > 2) {
            throw new IllegalArgumentException(
                    "Sort format must be field,direction"
            );
        }

        String property =
                parts[0].trim();

        if (property.isBlank()) {
            throw new IllegalArgumentException(
                    "Sort field cannot be blank"
            );
        }

        if (!ALLOWED_SORT_FIELDS
                .contains(property)) {

            throw new IllegalArgumentException(
                    "Unsupported sort field: "
                            + property
            );
        }

        Sort.Direction direction =
                Sort.Direction.ASC;

        if (parts.length == 2) {
            try {
                direction =
                        Sort.Direction.fromString(
                                parts[1].trim()
                        );
            } catch (
                    IllegalArgumentException exception
            ) {
                throw new IllegalArgumentException(
                        "Sort direction must be ASC or DESC"
                );
            }
        }

        return Sort.by(
                direction,
                property
        );
    }

    private CreateVehicleRequest normalize(
            CreateVehicleRequest request
    ) {
        return new CreateVehicleRequest(
                normalizeUpper(
                        request.vin()
                ),
                normalizeUpper(
                        request.licensePlate()
                ),
                normalizeText(
                        request.make()
                ),
                normalizeText(
                        request.model()
                ),
                request.manufactureYear(),
                request.status()
        );
    }

    private UpdateVehicleRequest normalize(
            UpdateVehicleRequest request
    ) {
        return new UpdateVehicleRequest(
                normalizeUpper(
                        request.vin()
                ),
                normalizeUpper(
                        request.licensePlate()
                ),
                normalizeText(
                        request.make()
                ),
                normalizeText(
                        request.model()
                ),
                request.manufactureYear(),
                request.status(),
                request.version()
        );
    }

    private String normalizeUpper(
            String value
    ) {
        return value
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String normalizeText(
            String value
    ) {
        return value.trim();
    }
}