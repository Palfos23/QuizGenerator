package com.quizapp.service;

import com.quizapp.dto.GridCategoryDto;
import com.quizapp.dto.GridCategoryRequest;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.GridCategory;
import com.quizapp.repository.AthletePoolRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.GridCategoryRepository;
import com.quizapp.repository.GridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GridCategoryService {

    private final GridCategoryRepository categoryRepository;
    private final AthleteRepository athleteRepository;
    private final ClubRepository clubRepository;
    private final GridRepository gridRepository;
    private final AthletePoolRepository athletePoolRepository;

    public GridCategoryService(GridCategoryRepository categoryRepository, AthleteRepository athleteRepository,
                                ClubRepository clubRepository, GridRepository gridRepository,
                                AthletePoolRepository athletePoolRepository) {
        this.categoryRepository = categoryRepository;
        this.athleteRepository = athleteRepository;
        this.clubRepository = clubRepository;
        this.gridRepository = gridRepository;
        this.athletePoolRepository = athletePoolRepository;
    }

    @Transactional(readOnly = true)
    public List<GridCategoryDto> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new GridCategoryDto(c.getId(), c.getName(), c.getGroupLabel()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GridCategoryDto create(GridCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A category named '" + name + "' already exists.");
        }
        GridCategory category = new GridCategory();
        category.setName(name);
        category.setGroupLabel(blankToTeam(request.getGroupLabel()));
        return toDto(categoryRepository.save(category));
    }

    private static String blankToTeam(String groupLabel) {
        return (groupLabel == null || groupLabel.isBlank()) ? "Team" : groupLabel.trim();
    }

    // Renaming cascades to every athlete/club/grid/pool currently using the old
    // name - since these columns store the category name directly rather than a
    // foreign key id, a rename without cascading would silently orphan
    // everything that already referenced it.
    @Transactional
    public GridCategoryDto update(Long id, GridCategoryRequest request) {
        GridCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No category found with id " + id));
        String newName = request.getName().trim();
        String oldName = category.getName();

        if (!oldName.equalsIgnoreCase(newName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("A category named '" + newName + "' already exists.");
        }

        category.setName(newName);
        category.setGroupLabel(blankToTeam(request.getGroupLabel()));
        categoryRepository.save(category);

        if (!oldName.equals(newName)) {
            athleteRepository.renameSport(oldName, newName);
            clubRepository.renameSport(oldName, newName);
            gridRepository.renameSport(oldName, newName);
            athletePoolRepository.renameSport(oldName, newName);
        }

        return toDto(category);
    }

    // Deletion is blocked if anything still uses this category - forces an
    // explicit reassignment first rather than silently orphaning data, same
    // principle as the athlete-delete-blocks-if-used-in-grids protection
    // elsewhere in this app.
    @Transactional
    public void delete(Long id) {
        GridCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No category found with id " + id));
        String name = category.getName();
        boolean inUse = athleteRepository.existsBySport(name)
                || clubRepository.existsBySport(name)
                || gridRepository.existsBySport(name)
                || athletePoolRepository.existsBySport(name);
        if (inUse) {
            throw new IllegalArgumentException(
                    "This category is still used by at least one athlete, club, grid, or pool - reassign those first.");
        }
        categoryRepository.deleteById(id);
    }

    private GridCategoryDto toDto(GridCategory c) {
        return new GridCategoryDto(c.getId(), c.getName(), c.getGroupLabel());
    }
}
