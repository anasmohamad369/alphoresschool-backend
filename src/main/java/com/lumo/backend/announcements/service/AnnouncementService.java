package com.lumo.backend.announcements.service;

import com.lumo.backend.announcements.dto.AnnouncementRequest;
import com.lumo.backend.announcements.entity.Announcement;
import com.lumo.backend.announcements.repository.AnnouncementRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public Announcement createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setDescription(request.description());
        announcement.setType(request.type());
        announcement.setStartDate(request.startDate() != null ? request.startDate() : LocalDate.now());
        return announcementRepository.save(announcement);
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    public List<Announcement> getAnnouncementsByType(String type) {
        return announcementRepository.findByType(type);
    }

    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found with id: " + id));
    }

    public Announcement updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = getAnnouncementById(id);
        announcement.setTitle(request.title());
        announcement.setDescription(request.description());
        announcement.setType(request.type());
        announcement.setStartDate(request.startDate() != null ? request.startDate() : LocalDate.now());
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(Long id) {
        Announcement announcement = getAnnouncementById(id);
        announcementRepository.delete(announcement);
    }
}
