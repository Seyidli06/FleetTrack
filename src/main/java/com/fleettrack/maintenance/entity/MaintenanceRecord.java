package com.fleettrack.maintenance.entity;

import com.fleettrack.vehicle.entity.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "vehicle_id",
            nullable = false
    )
    private Vehicle vehicle;

    @Column(
            name = "maintenance_type",
            nullable = false,
            length = 50
    )
    private String maintenanceType;

    @Column(
            name = "description",
            length = 1000
    )
    private String description;

    @Column(
            name = "service_date",
            nullable = false
    )
    private LocalDate serviceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(name = "odometer")
    private Long odometer;

    @Column(
            name = "cost",
            precision = 12,
            scale = 2
    )
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private MaintenanceStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = MaintenanceStatus.SCHEDULED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }

    public Long getOdometer() {
        return odometer;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void setMaintenanceType(
            String maintenanceType
    ) {
        this.maintenanceType = maintenanceType;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

    public void setServiceDate(
            LocalDate serviceDate
    ) {
        this.serviceDate = serviceDate;
    }

    public void setNextServiceDate(
            LocalDate nextServiceDate
    ) {
        this.nextServiceDate = nextServiceDate;
    }

    public void setOdometer(
            Long odometer
    ) {
        this.odometer = odometer;
    }

    public void setCost(
            BigDecimal cost
    ) {
        this.cost = cost;
    }

    public void setStatus(
            MaintenanceStatus status
    ) {
        this.status = status;
    }
}