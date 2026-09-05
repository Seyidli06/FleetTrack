package com.fleettrack.driver.service;

import com.fleettrack.common.cache.CacheNames;
import com.fleettrack.common.dto.PageResponse;
import com.fleettrack.common.exception.DuplicateResourceException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.common.exception.StaleVersionException;
import com.fleettrack.driver.dto.CreateDriverRequest;
import com.fleettrack.driver.dto.DriverFilter;
import com.fleettrack.driver.dto.DriverResponse;
import com.fleettrack.driver.dto.DriverSummaryResponse;
import com.fleettrack.driver.dto.UpdateDriverRequest;
import com.fleettrack.driver.entity.Driver;
import com.fleettrack.driver.mapper.DriverMapper;
import com.fleettrack.driver.repository.DriverRepository;
import com.fleettrack.driver.specification.DriverSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
public class DriverService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "id",
                    "firstName",
                    "lastName",
                    "licenseNumber",
                    "licenseCategory",
                    "licenseExpiryDate",
                    "status",
                    "createdAt",
                    "updatedAt"
            );

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverService(
            DriverRepository driverRepository,
            DriverMapper driverMapper
    ) {
        this.driverRepository =
                driverRepository;

        this.driverMapper =
                driverMapper;
    }

    @Transactional
    public DriverResponse create(
            CreateDriverRequest request
    ) {
        CreateDriverRequest normalized =
                normalize(request);

        ensureLicenseNumberAvailable(
                normalized.licenseNumber()
        );

        Driver driver =
                driverMapper.toEntity(
                        normalized
                );

        Driver saved =
                driverRepository.saveAndFlush(
                        driver
                );

        return driverMapper.toResponse(
                saved
        );
    }

    @Cacheable(
            cacheNames = CacheNames.DRIVER_BY_ID,
            key = "#id"
    )
    @Transactional(readOnly = true)
    public DriverResponse getById(
            Long id
    ) {
        return driverMapper.toResponse(
                getDriver(id)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverSummaryResponse> getAll(
            DriverFilter filter,
            int page,
            int size,
            String sort
    ) {
        validatePagination(
                page,
                size
        );

        validateFilter(
                filter
        );

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        parseSort(sort)
                );

        Page<DriverSummaryResponse> result =
                driverRepository
                        .findAll(
                                DriverSpecification
                                        .withFilter(
                                                filter
                                        ),
                                pageable
                        )
                        .map(
                                driverMapper
                                        ::toSummaryResponse
                        );

        return PageResponse.from(
                result
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.DRIVER_BY_ID,
            key = "#id"
    )
    @Transactional
    public DriverResponse update(
            Long id,
            UpdateDriverRequest request
    ) {
        Driver driver =
                getDriver(id);

        ensureVersionMatches(
                driver,
                request.version()
        );

        UpdateDriverRequest normalized =
                normalize(request);

        ensureLicenseNumberAvailableForUpdate(
                normalized.licenseNumber(),
                id
        );

        driverMapper.update(
                normalized,
                driver
        );

        driverRepository.flush();

        return driverMapper.toResponse(
                driver
        );
    }

    @CacheEvict(
            cacheNames = CacheNames.DRIVER_BY_ID,
            key = "#id"
    )
    @Transactional
    public void delete(
            Long id
    ) {
        Driver driver =
                getDriver(id);

        driverRepository.delete(
                driver
        );

        driverRepository.flush();
    }

    public Driver getDriver(
            Long id
    ) {
        return driverRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Driver with id "
                                        + id
                                        + " was not found"
                        )
                );
    }

    private void ensureLicenseNumberAvailable(
            String licenseNumber
    ) {
        if (driverRepository
                .existsByLicenseNumberIgnoreCase(
                        licenseNumber
                )) {

            throw new DuplicateResourceException(
                    "Driver with license number "
                            + licenseNumber
                            + " already exists"
            );
        }
    }

    private void ensureLicenseNumberAvailableForUpdate(
            String licenseNumber,
            Long id
    ) {
        if (driverRepository
                .existsByLicenseNumberIgnoreCaseAndIdNot(
                        licenseNumber,
                        id
                )) {

            throw new DuplicateResourceException(
                    "Driver with license number "
                            + licenseNumber
                            + " already exists"
            );
        }
    }

    private void ensureVersionMatches(
            Driver driver,
            Long requestedVersion
    ) {
        if (!driver.getVersion()
                .equals(requestedVersion)) {

            throw new StaleVersionException(
                    "Driver has been modified. "
                            + "Expected version "
                            + driver.getVersion()
                            + " but received "
                            + requestedVersion
            );
        }
    }

    private void validateFilter(
            DriverFilter filter
    ) {
        if (filter == null) {
            return;
        }

        LocalDate after =
                filter.licenseExpiringAfter();

        LocalDate before =
                filter.licenseExpiringBefore();

        if (after != null
                && before != null
                && after.isAfter(before)) {

            throw new IllegalArgumentException(
                    "licenseExpiringAfter cannot be after "
                            + "licenseExpiringBefore"
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

    private CreateDriverRequest normalize(
            CreateDriverRequest request
    ) {
        return new CreateDriverRequest(
                normalizeRequired(
                        request.firstName()
                ),
                normalizeRequired(
                        request.lastName()
                ),
                normalizeUpper(
                        request.licenseNumber()
                ),
                normalizeUpper(
                        request.licenseCategory()
                ),
                request.licenseExpiryDate(),
                normalizeOptional(
                        request.phone()
                ),
                normalizeEmail(
                        request.email()
                ),
                request.status()
        );
    }

    private UpdateDriverRequest normalize(
            UpdateDriverRequest request
    ) {
        return new UpdateDriverRequest(
                normalizeRequired(
                        request.firstName()
                ),
                normalizeRequired(
                        request.lastName()
                ),
                normalizeUpper(
                        request.licenseNumber()
                ),
                normalizeUpper(
                        request.licenseCategory()
                ),
                request.licenseExpiryDate(),
                normalizeOptional(
                        request.phone()
                ),
                normalizeEmail(
                        request.email()
                ),
                request.status(),
                request.version()
        );
    }

    private String normalizeRequired(
            String value
    ) {
        return value.trim();
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

    private String normalizeOptional(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeEmail(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}