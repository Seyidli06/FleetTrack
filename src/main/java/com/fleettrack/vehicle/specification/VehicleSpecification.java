package com.fleettrack.vehicle.specification;

import com.fleettrack.assignment.entity.VehicleAssignment;
import com.fleettrack.vehicle.dto.VehicleFilter;
import com.fleettrack.vehicle.entity.Vehicle;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class VehicleSpecification {

    private VehicleSpecification() {
    }

    public static Specification<Vehicle> withFilter(
            VehicleFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                filter.status()
                        )
                );
            }

            if (hasText(filter.make())) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("make")
                                ),
                                "%"
                                        + filter.make()
                                        .trim()
                                        .toLowerCase()
                                        + "%"
                        )
                );
            }

            if (hasText(filter.model())) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("model")
                                ),
                                "%"
                                        + filter.model()
                                        .trim()
                                        .toLowerCase()
                                        + "%"
                        )
                );
            }

            if (filter.yearFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("manufactureYear"),
                                filter.yearFrom()
                        )
                );
            }

            if (filter.yearTo() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("manufactureYear"),
                                filter.yearTo()
                        )
                );
            }

            if (filter.driverId() != null) {

                Subquery<Long> subquery =
                        query.subquery(Long.class);

                var assignment =
                        subquery.from(
                                VehicleAssignment.class
                        );

                subquery.select(
                        assignment
                                .get("vehicle")
                                .get("id")
                );

                subquery.where(
                        criteriaBuilder.equal(
                                assignment
                                        .get("driver")
                                        .get("id"),
                                filter.driverId()
                        ),
                        criteriaBuilder.isNull(
                                assignment.get(
                                        "unassignedAt"
                                )
                        )
                );

                predicates.add(
                        root.get("id")
                                .in(subquery)
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }

    private static boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }
}