package com.tieyoh.solutions.repository;

import com.tieyoh.solutions.domain.EventItem;
import com.tieyoh.solutions.domain.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventItemRepository extends JpaRepository<EventItem, Long> {
    List<EventItem> findByVisibilityOrderByEventDateAsc(Visibility visibility);
    List<EventItem> findAllByOrderByEventDateAsc();
}
