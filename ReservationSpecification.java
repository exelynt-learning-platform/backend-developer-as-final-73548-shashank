package com.booking.resourcebooking.specification;

import com.booking.resourcebooking.entity.Reservation;
import com.booking.resourcebooking.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> ownedByUser(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> priceGreaterOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> priceLessOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> resourceIdEquals(Long resourceId) {
        return (root, query, cb) -> resourceId == null ? null : cb.equal(root.get("resource").get("id"), resourceId);
    }

    public static Specification<Reservation> build(Long ownerUserId, ReservationStatus status,
                                                     BigDecimal minPrice, BigDecimal maxPrice, Long resourceId) {
        return Specification.where(ownedByUser(ownerUserId))
                .and(hasStatus(status))
                .and(priceGreaterOrEqual(minPrice))
                .and(priceLessOrEqual(maxPrice))
                .and(resourceIdEquals(resourceId));
    }
}
