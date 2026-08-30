package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.request.ResourceRequest;
import com.booking.resourcebooking.dto.response.ResourceResponse;
import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .location(request.location())
                .available(request.available() == null || request.available())
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    public Page<ResourceResponse> list(String type, Boolean available, Pageable pageable) {
        Specification<Resource> spec = Specification.where(typeEquals(type)).and(availableEquals(available));
        return resourceRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntity(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setLocation(request.location());
        if (request.available() != null) {
            resource.setAvailable(request.available());
        }
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }

    Resource findEntity(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private Specification<Resource> typeEquals(String type) {
        return (root, query, cb) -> (type == null || type.isBlank()) ? null : cb.equal(cb.lower(root.get("type")), type.toLowerCase());
    }

    private Specification<Resource> availableEquals(Boolean available) {
        return (root, query, cb) -> available == null ? null : cb.equal(root.get("available"), available);
    }

    private ResourceResponse toResponse(Resource r) {
        return ResourceResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .description(r.getDescription())
                .location(r.getLocation())
                .available(r.isAvailable())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
