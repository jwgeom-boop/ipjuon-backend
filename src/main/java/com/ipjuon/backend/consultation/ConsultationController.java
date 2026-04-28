package com.ipjuon.backend.consultation;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/consultation")
@CrossOrigin(origins = "*")
public class ConsultationController {

    private final ConsultationRepository repository;

    public ConsultationController(ConsultationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ConsultationRequest> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ConsultationRequest getOne(@PathVariable java.util.UUID id) {
        return repository.findById(id).orElseThrow();
    }

    @PostMapping
    public ConsultationRequest create(@RequestBody ConsultationRequest request) {
        return repository.save(request);
    }

    @PatchMapping("/{id}")
    public ConsultationRequest update(@PathVariable java.util.UUID id, @RequestBody ConsultationRequest request) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getMemo() != null) existing.setMemo(request.getMemo());
        return repository.save(existing);
    }
}
