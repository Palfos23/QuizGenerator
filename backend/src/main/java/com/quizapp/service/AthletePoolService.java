package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.AthletePool;
import com.quizapp.repository.AthletePoolRepository;
import com.quizapp.repository.AthleteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AthletePoolService {

    private final AthletePoolRepository poolRepository;
    private final AthleteRepository athleteRepository;

    public AthletePoolService(AthletePoolRepository poolRepository, AthleteRepository athleteRepository) {
        this.poolRepository = poolRepository;
        this.athleteRepository = athleteRepository;
    }

    @Transactional(readOnly = true)
    public List<AthletePoolSummaryDto> findAll() {
        return poolRepository.findAll().stream()
                .map(p -> new AthletePoolSummaryDto(p.getId(), p.getName(), p.getSport(), p.getMembers().size()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AthletePoolDto getOne(Long id) {
        return toDto(poolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No pool found with id " + id)));
    }

    @Transactional
    public AthletePoolDto create(AthletePoolRequest request) {
        AthletePool pool = new AthletePool();
        applyRequest(pool, request);
        return toDto(poolRepository.save(pool));
    }

    @Transactional
    public AthletePoolDto update(Long id, AthletePoolRequest request) {
        AthletePool pool = poolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No pool found with id " + id));
        applyRequest(pool, request);
        return toDto(poolRepository.save(pool));
    }

    @Transactional
    public void delete(Long id) {
        if (!poolRepository.existsById(id)) {
            throw new ResourceNotFoundException("No pool found with id " + id);
        }
        poolRepository.deleteById(id);
    }

    private void applyRequest(AthletePool pool, AthletePoolRequest request) {
        pool.setName(request.getName());
        pool.setSport(request.getSport());

        List<Athlete> found = athleteRepository.findAllById(request.getAthleteIds());
        if (found.size() != new HashSet<>(request.getAthleteIds()).size()) {
            List<Long> foundIds = found.stream().map(Athlete::getId).collect(Collectors.toList());
            List<Long> missing = request.getAthleteIds().stream()
                    .filter(aid -> !foundIds.contains(aid))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("No athlete found with id(s) " + missing);
        }
        // Plain @ManyToMany join table, no owned child entities - a straight
        // replacement here is safe and simple, none of the orphan-removal/
        // collection-diffing complexity that applies to Grid's own candidates.
        pool.setMembers(new HashSet<>(found));
    }

    private AthletePoolDto toDto(AthletePool pool) {
        List<AthleteDto> members = pool.getMembers().stream()
                .map(AthleteService::toDto)
                .collect(Collectors.toList());
        return new AthletePoolDto(pool.getId(), pool.getName(), pool.getSport(), members);
    }
}
