package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.request.ReservationRequest;
import com.booking.resourcebooking.dto.request.ReservationStatusUpdateRequest;
import com.booking.resourcebooking.dto.request.ReservationUpdateRequest;
import com.booking.resourcebooking.dto.response.ReservationResponse;
import com.booking.resourcebooking.entity.Reservation;
import com.booking.resourcebooking.entity.ReservationStatus;
import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.exception.BadRequestException;
import com.booking.resourcebooking.exception.ConflictException;
import com.booking.resourcebooking.exception.ForbiddenOperationException;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.UserRepository;
import com.booking.resourcebooking.security.CurrentUserProvider;
import com.booking.resourcebooking.specification.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();
        Resource resource = resourceService.findEntity(request.resourceId());

        validateTimeRange(request.startTime(), request.endTime());

        // Identity always comes from the JWT-derived principal. The only exception is an ADMIN
        // explicitly booking on behalf of another user via the optional userId field.
        User owner = currentUser;
        if (currentUser.getRole() == Role.ADMIN && request.userId() != null) {
            owner = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));
        }

        if (!resource.isAvailable()) {
            throw new ConflictException("Resource '" + resource.getName() + "' is not currently available for booking");
        }

        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), null);

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(owner)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.PENDING)
                .price(request.price())
                .notes(request.notes())
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse getById(Long id) {
        Reservation reservation = findEntity(id);
        assertCanView(reservation);
        return toResponse(reservation);
    }

    public Page<ReservationResponse> list(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                           Long resourceId, Pageable pageable) {
        User currentUser = currentUserProvider.getCurrentUser();

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }

        // ADMIN sees every reservation; USER is hard-scoped to their own regardless of query params.
        Long ownerFilter = currentUser.getRole() == Role.ADMIN ? null : currentUser.getId();

        var spec = ReservationSpecification.build(ownerFilter, status, minPrice, maxPrice, resourceId);
        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        // Full edit (resource/time/status/price) is an ADMIN-only capability, enforced at controller
        // level via @PreAuthorize as well — kept here too as defense in depth.
        Reservation reservation = findEntity(id);
        Resource resource = resourceService.findEntity(request.resourceId());

        validateTimeRange(request.startTime(), request.endTime());
        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), reservation.getId());

        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(request.status());
        reservation.setPrice(request.price());
        reservation.setNotes(request.notes());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request) {
        Reservation reservation = findEntity(id);
        reservation.setStatus(request.status());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancelOwn(Long id) {
        User currentUser = currentUserProvider.getCurrentUser();
        Reservation reservation = findEntity(id);

        boolean owns = reservation.getUser().getId().equals(currentUser.getId());
        if (!owns && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("You can only cancel your own reservations");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ConflictException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    // ---- helpers ----

    private Reservation findEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertCanView(Reservation reservation) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean owns = reservation.getUser().getId().equals(currentUser.getId());
        if (currentUser.getRole() != Role.ADMIN && !owns) {
            // 404 instead of 403 so a USER can't probe for the existence of other users' reservations.
            throw new ResourceNotFoundException("Reservation not found with id: " + reservation.getId());
        }
    }

    private void validateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startTime must be before endTime");
        }
    }

    private void assertNoOverlap(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long excludeId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(resourceId, start, end, excludeId);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("The resource is already booked during the requested time window");
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource().getId())
                .resourceName(r.getResource().getName())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .price(r.getPrice())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
