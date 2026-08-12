package com.tieyoh.solutions.service;

import com.tieyoh.solutions.domain.EventItem;
import com.tieyoh.solutions.domain.Visibility;
import com.tieyoh.solutions.repository.EventItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventItemRepository eventItemRepository;

    public List<EventItem> publicEvents() {
        return eventItemRepository.findByVisibilityOrderByEventDateAsc(Visibility.PUBLIC);
    }

    public List<EventItem> allEvents() {
        return eventItemRepository.findAllByOrderByEventDateAsc();
    }

    @Transactional
    public EventItem save(String title, String description, LocalDateTime eventDate, String location,
                           Visibility visibility, Long createdBy) {
        EventItem event = EventItem.builder()
                .title(title)
                .description(description)
                .eventDate(eventDate)
                .location(location)
                .visibility(visibility)
                .createdBy(createdBy)
                .build();
        return eventItemRepository.save(event);
    }

    @Transactional
    public void delete(Long id) {
        eventItemRepository.deleteById(id);
    }
}
