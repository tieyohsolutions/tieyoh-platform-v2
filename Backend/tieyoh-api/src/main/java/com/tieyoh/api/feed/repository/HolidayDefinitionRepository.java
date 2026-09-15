package com.tieyoh.api.feed.repository;

import com.tieyoh.api.feed.entity.HolidayDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HolidayDefinitionRepository extends JpaRepository<HolidayDefinition, Long> {

    List<HolidayDefinition> findByEnabledTrue();
}
