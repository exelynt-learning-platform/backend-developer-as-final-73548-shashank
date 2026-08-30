package com.booking.resourcebooking.repository;

import com.booking.resourcebooking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    // Finds any non-cancelled reservation for the same resource whose time window overlaps
    // [startTime, endTime). Used to block double-bookings. excludeId lets an update ignore itself.
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> com.booking.resourcebooking.entity.ReservationStatus.CANCELLED
              AND (:excludeId IS NULL OR r.id <> :excludeId)
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("excludeId") Long excludeId);
}
