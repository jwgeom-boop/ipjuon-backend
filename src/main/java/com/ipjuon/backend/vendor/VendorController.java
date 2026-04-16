package com.ipjuon.backend.vendor;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@CrossOrigin(origins = "*")
public class VendorController {

    private final VendorRepository repository;

    public VendorController(VendorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Vendor> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public Vendor create(@RequestBody Vendor vendor) {
        return repository.save(vendor);
    }

    @PutMapping("/{id}")
    public Vendor update(@PathVariable UUID id, @RequestBody Vendor vendor) {
        Vendor existing = repository.findById(id).orElseThrow();
        existing.setVendorName(vendor.getVendorName());
        existing.setVendorType(vendor.getVendorType());
        existing.setPhone(vendor.getPhone());
        if (vendor.getPassword() != null && !vendor.getPassword().isEmpty()) {
            existing.setPassword(vendor.getPassword());
        }
        return repository.save(existing);
    }

    @PatchMapping("/{id}/status")
    public Vendor toggleStatus(@PathVariable UUID id, @RequestBody Vendor vendor) {
        Vendor existing = repository.findById(id).orElseThrow();
        existing.setStatus(vendor.getStatus());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}
