package com.quizapp.service;

import com.quizapp.dto.ImposterFlipResultDto;
import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.dto.ImposterPlayStateDto;
import com.quizapp.dto.ImposterRevealDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.ImposterGrid;
import com.quizapp.model.ImposterTile;
import com.quizapp.repository.ImposterGridRepository;
import com.quizapp.repository.ImposterTileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImposterGridPlayService {

    private final ImposterGridRepository gridRepository;
    private final ImposterTileRepository tileRepository;

    public ImposterGridPlayService(ImposterGridRepository gridRepository, ImposterTileRepository tileRepository) {
        this.gridRepository = gridRepository;
        this.tileRepository = tileRepository;
    }

    @Transactional(readOnly = true)
    public List<ImposterGridSummaryDto> list(String sport) {
        List<ImposterGrid> grids = sport != null && !sport.isBlank()
                ? gridRepository.findBySportOrderByCreatedAtDesc(sport)
                : gridRepository.findAllByOrderByCreatedAtDesc();
        return grids.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImposterPlayStateDto getPlayState(Long gridId) {
        ImposterGrid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No imposter grid found with id " + gridId));

        ImposterPlayStateDto dto = new ImposterPlayStateDto();
        dto.setId(grid.getId());
        dto.setTitle(grid.getTitle());
        dto.setDescription(grid.getDescription());
        dto.setDisplayMode(grid.getDisplayMode().name());
        dto.setTiles(grid.getTiles().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(t -> new ImposterPlayStateDto.TileView(t.getId(), t.getAthlete().getName(),
                        resolvedPhotoUrl(t), t.getClub() != null ? t.getClub().getLogoUrl() : null))
                .collect(Collectors.toList()));
        return dto;
    }

    // Same idea as GridPlayService.resolvedPhotoUrl - a tile-specific photo
    // choice if one was made, otherwise the athlete's own primary photo.
    private String resolvedPhotoUrl(ImposterTile t) {
        return t.getSelectedPhoto() != null ? t.getSelectedPhoto().getPhotoUrl() : t.getAthlete().getPhotoUrl();
    }

    @Transactional(readOnly = true)
    public ImposterFlipResultDto flip(Long gridId, Long tileId) {
        ImposterTile tile = tileRepository.findById(tileId)
                .orElseThrow(() -> new ResourceNotFoundException("No tile found with id " + tileId));
        if (!tile.getImposterGrid().getId().equals(gridId)) {
            throw new IllegalArgumentException("That tile doesn't belong to this grid.");
        }
        return new ImposterFlipResultDto(tile.getId(), tile.isImposter());
    }

    // Called once every tile's been flipped - the full imposter-to-replaced
    // mapping, never shown any earlier than that.
    @Transactional(readOnly = true)
    public List<ImposterRevealDto> reveal(Long gridId) {
        ImposterGrid grid = gridRepository.findById(gridId)
                .orElseThrow(() -> new ResourceNotFoundException("No imposter grid found with id " + gridId));
        return grid.getTiles().stream()
                .filter(ImposterTile::isImposter)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(t -> new ImposterRevealDto(
                        t.getAthlete().getName(),
                        t.getReplacedAthlete() != null ? t.getReplacedAthlete().getName() : null))
                .collect(Collectors.toList());
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
}
