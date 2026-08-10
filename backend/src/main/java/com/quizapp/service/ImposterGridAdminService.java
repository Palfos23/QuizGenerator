package com.quizapp.service;

import com.quizapp.dto.ImposterGridAdminDetailDto;
import com.quizapp.dto.ImposterGridRequest;
import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.Club;
import com.quizapp.model.ImposterGrid;
import com.quizapp.model.ImposterTile;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.ClubRepository;
import com.quizapp.repository.ImposterGridRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImposterGridAdminService {

    private final ImposterGridRepository gridRepository;
    private final AthleteRepository athleteRepository;
    private final ClubRepository clubRepository;
    private final AthleteService athleteService;

    public ImposterGridAdminService(ImposterGridRepository gridRepository, AthleteRepository athleteRepository,
                                     ClubRepository clubRepository, AthleteService athleteService) {
        this.gridRepository = gridRepository;
        this.athleteRepository = athleteRepository;
        this.clubRepository = clubRepository;
        this.athleteService = athleteService;
    }

    @Transactional(readOnly = true)
    public List<ImposterGridSummaryDto> findAll() {
        return gridRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImposterGridAdminDetailDto getOne(Long id) {
        return toDetailDto(gridRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No imposter grid found with id " + id)));
    }

    @Transactional
    public ImposterGridAdminDetailDto create(ImposterGridRequest request) {
        ImposterGrid grid = new ImposterGrid();
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
    }

    @Transactional
    public ImposterGridAdminDetailDto update(Long id, ImposterGridRequest request) {
        ImposterGrid grid = gridRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No imposter grid found with id " + id));
        applyRequest(grid, request);
        return toDetailDto(gridRepository.save(grid));
    }

    @Transactional
    public void delete(Long id) {
        gridRepository.deleteById(id);
    }

    private void applyRequest(ImposterGrid grid, ImposterGridRequest request) {
        grid.setTitle(request.getTitle());
        grid.setDescription(request.getDescription());
        grid.setSport(request.getSport());
        grid.setDisplayMode(ImposterGrid.DisplayMode.valueOf(request.getDisplayMode()));

        List<ImposterTile> tiles = new ArrayList<>();
        int index = 0;
        for (ImposterGridRequest.TileInput input : request.getTiles()) {
            Athlete athlete = athleteRepository.findById(input.getAthleteId())
                    .orElseThrow(() -> new IllegalArgumentException("No subject found with id " + input.getAthleteId()));

            ImposterTile tile = new ImposterTile();
            tile.setAthlete(athlete);
            tile.setImposter(input.isImposter());
            tile.setOrderIndex(index++);

            if (input.isImposter() && input.getReplacedAthleteId() != null) {
                Athlete replaced = athleteRepository.findById(input.getReplacedAthleteId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No subject found with id " + input.getReplacedAthleteId()));
                tile.setReplacedAthlete(replaced);
            } else {
                tile.setReplacedAthlete(null);
            }

            if (input.getClubId() != null) {
                Club club = clubRepository.findById(input.getClubId())
                        .orElseThrow(() -> new IllegalArgumentException("No club found with id " + input.getClubId()));
                tile.setClub(club);
            } else {
                tile.setClub(null);
            }

            tiles.add(tile);
        }
        grid.setTiles(tiles);
    }

    private ImposterGridSummaryDto toSummaryDto(ImposterGrid grid) {
        ImposterGridSummaryDto dto = new ImposterGridSummaryDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setDescription(grid.getDescription());
        dto.setSport(grid.getSport());
        dto.setTileCount(grid.getTiles().size());
        dto.setImposterCount((int) grid.getTiles().stream().filter(ImposterTile::isImposter).count());
        return dto;
    }

    private ImposterGridAdminDetailDto toDetailDto(ImposterGrid grid) {
        ImposterGridAdminDetailDto dto = new ImposterGridAdminDetailDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setDescription(grid.getDescription());
        dto.setSport(grid.getSport());
        dto.setDisplayMode(grid.getDisplayMode().name());
        dto.setTiles(grid.getTiles().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(t -> new ImposterGridAdminDetailDto.TileDetail(
                        t.getId(),
                        athleteService.toDtoWithPhotos(t.getAthlete()),
                        t.isImposter(),
                        t.getReplacedAthlete() != null ? athleteService.toDtoWithPhotos(t.getReplacedAthlete()) : null,
                        t.getClub() != null ? ClubService.toDto(t.getClub()) : null))
                .collect(Collectors.toList()));
        return dto;
    }
}
