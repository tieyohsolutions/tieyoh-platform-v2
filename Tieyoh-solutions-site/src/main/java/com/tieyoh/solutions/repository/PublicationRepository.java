package com.tieyoh.solutions.repository;

import com.tieyoh.solutions.domain.Publication;
import com.tieyoh.solutions.domain.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicationRepository extends JpaRepository<Publication, Long> {
    List<Publication> findByVisibilityOrderByPublishedAtDesc(Visibility visibility);
    List<Publication> findAllByOrderByPublishedAtDesc();
}
