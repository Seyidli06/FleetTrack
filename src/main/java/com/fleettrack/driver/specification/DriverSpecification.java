package com.fleettrack.driver.specification;

import com.fleettrack.driver.dto.DriverFilter;
import com.fleettrack.driver.entity.Driver;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class DriverSpecification {

    private DriverSpecification() {
    }

    public static Specification<Driver> withFilter(
            DriverFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                filter.status()
                        )
                );
            }

            if (hasText(filter.firstName())) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("firstName")
                                ),
                                "%"
                                        + filter.firstName()
                                        .trim()
                                        .toLowerCase()
                                        + "%"
                        )
                );
            }

            if (hasText(filter.lastName())) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("lastName")
                                ),
                                "%"
                                        + filter.lastName()
                                        .trim()
                                        .toLowerCase()
                                        + "%"
                        )
                );
            }

            if (hasText(filter.licenseCategory())) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.upper(
                                        root.get("licenseCategory")
                                ),
                                filter.licenseCategory()
                                        .trim()
                                        .toUpperCase()
                        )
                );
            }

            if (filter.licenseExpiringAfter() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("licenseExpiryDate"),
                                filter.licenseExpiringAfter()
                        )
                );
            }

            if (filter.licenseExpiringBefore() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("licenseExpiryDate"),
                                filter.licenseExpiringBefore()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }
}