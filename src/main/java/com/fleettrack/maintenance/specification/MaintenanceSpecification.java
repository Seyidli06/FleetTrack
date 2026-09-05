package com.fleettrack.maintenance.specification;

import com.fleettrack.maintenance.dto.MaintenanceFilter;
import com.fleettrack.maintenance.entity.MaintenanceRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class MaintenanceSpecification {

    private MaintenanceSpecification() {
    }

    public static Specification<MaintenanceRecord> withFilter(
            Long vehicleId,
            MaintenanceFilter filter
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    criteriaBuilder.equal(
                            root.get("vehicle").get("id"),
                            vehicleId
                    )
            );

            if (filter.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                filter.status()
                        )
                );
            }

            if (filter.serviceDateFrom() != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("serviceDate"),
                                filter.serviceDateFrom()
                        )
                );
            }

            if (filter.serviceDateTo() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("serviceDate"),
                                filter.serviceDateTo()
                        )
                );
            }

            if (filter.nextServiceBefore() != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("nextServiceDate"),
                                filter.nextServiceBefore()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }
}