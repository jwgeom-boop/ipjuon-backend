package com.ipjuon.backend.notice;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {

    private final NoticeRepository repository;

    public NoticeController(NoticeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Notice> getAll() {
        return repository.findAllOrdered();
    }

    @PostMapping
    public Notice create(@RequestBody Notice notice) {
        return repository.save(notice);
    }

    @PutMapping("/{id}")
    public Notice update(@PathVariable UUID id, @RequestBody Notice notice) {
        Notice existing = repository.findById(id).orElseThrow();
        existing.setCategory(notice.getCategory());
        existing.setTitle(notice.getTitle());
        existing.setContent(notice.getContent());
        existing.setPinned(notice.isPinned());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}
