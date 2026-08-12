package com.tieyoh.solutions.service;

import com.tieyoh.solutions.domain.Publication;
import com.tieyoh.solutions.domain.Visibility;
import com.tieyoh.solutions.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;

    public List<Publication> publicPublications() {
        return publicationRepository.findByVisibilityOrderByPublishedAtDesc(Visibility.PUBLIC);
    }

    public List<Publication> allPublications() {
        return publicationRepository.findAllByOrderByPublishedAtDesc();
    }

    @Transactional
    public Publication save(String title, String summary, String content, String externalUrl,
                             Visibility visibility, Long createdBy) {
        Publication publication = Publication.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .externalUrl(externalUrl)
                .visibility(visibility)
                .createdBy(createdBy)
                .build();
        return publicationRepository.save(publication);
    }

    @Transactional
    public void delete(Long id) {
        publicationRepository.deleteById(id);
    }
}
