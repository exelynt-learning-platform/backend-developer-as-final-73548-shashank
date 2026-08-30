package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.request.ReservationRequest;
import com.booking.resourcebooking.dto.request.ReservationStatusUpdateRequest;
import com.booking.resourcebooking.dto.request.ReservationUpdateRequest;
import com.booking.resourcebooking.dto.response.ReservationResponse;
import com.booking.resourcebooking.entity.ReservationStatus;
import com.booking.resourcebooking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservations", description = "Bookings. USER sees/creates only their own; ADMIN has full access.")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation. Identity is always taken from the JWT for USER callers.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @GetMapping
    @Operation(summary = "List reservations, paginated/filterable/sortable. USER is scoped to their own; ADMIN sees all.")
    public ResponseEntity<Page<ReservationResponse>> list(
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price (inclusive)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by resource id")
            @RequestParam(required = false) Long resourceId,
            @Parameter(description = "page (0-based), size, sort e.g. sort=price,desc")
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(reservationService.list(status, minPrice, maxPrice, resourceId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by id. USER can only fetch their own.")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fully update a reservation, including status/resource/time/price (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change only the status of a reservation (ADMIN only)")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation you own (USER can cancel their own; ADMIN can cancel any)")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelOwn(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
