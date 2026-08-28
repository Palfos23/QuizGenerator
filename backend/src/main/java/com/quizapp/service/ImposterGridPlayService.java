package com.quizapp.service;

import com.quizapp.dto.ImposterFlipResultDto;
import com.quizapp.dto.ImposterGridSummaryDto;
import com.quizapp.dto.ImposterPlayStateDto;
import com.quizapp.dto.ImposterRevealDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.ImposterGrid;
import com.quizapp.model.ImposterTile;
import com.quizapp.repository.ImposterGridRepository;
import com.quizapp.repository.ImposterGridSummaryProjection;
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
        List<ImposterGridSummaryProjection> rows = sport != null && !sport.isBlank()
                ? gridRepository.findSummariesBySportOrderByCreatedAtDesc(sport)
                : gridRepository.findSummariesOrderByCreatedAtDesc();
        return rows.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    /**
     * For "Random" Imposter's round-start picker (mirrors GridPlayService
     * .getBattleRoundChoices / LineupPlayService.getBattleRoundChoices): a
     * small pool of candidate boards for the upcoming round, minus whatever's
     * already been played this game. Unlike Grid/Lineup, every ImposterGrid is
     * always battle-eligible - there's no per-board "exclude from battle" flag.
     */
    @Transactional(readOnly = true)
    public List<ImposterGridSummaryDto> getBattleRoundChoices(int count, List<Long> excludeIds) {
        List<Long> ids = gridRepository.findAllIds().stream()
                .filter(id -> excludeIds == null || !excludeIds.contains(id))
                .collect(Collectors.toList());
        java.util.Collections.shuffle(ids);
        List<Long> sampled = ids.stream().limit(count).collect(Collectors.toList());
        if (sampled.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return gridRepository.findSummariesByIdIn(sampled).stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    /** Resolves specific ids to summaries, in no particular order - for re-displaying an already-generated set of choices. */
    @Transactional(readOnly = true)
    public List<ImposterGridSummaryDto> resolveSummaries(java.util.Collection<Long> ids) {
        return gridRepository.findSummariesByIdIn(ids).stream().map(this::toSummaryDto).collect(Collectors.toList());
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
        dto.setUpdatedAt(grid.getUpdatedAt());
        dto.setFitImages(grid.isFitImages());
        dto.setImposterCount((int) grid.getTiles().stream().filter(ImposterTile::isImposter).count());
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
        return new ImposterFlipResultDto(tile.getId(), tile.isImposter(), revealPhotoUrl(tile));
    }

    // Which photo to show once a tile is flipped - a dedicated reveal photo
    // for that specific outcome (correct or imposter) if the admin set one,
    // otherwise whatever was already showing before the flip.
    private String revealPhotoUrl(ImposterTile t) {
        if (t.isImposter()) {
            if (t.isRevealImposterUseDefaultPhoto()) return t.getAthlete().getPhotoUrl();
            if (t.getRevealImposterPhoto() != null) return t.getRevealImposterPhoto().getPhotoUrl();
        } else {
            if (t.isRevealCorrectUseDefaultPhoto()) return t.getAthlete().getPhotoUrl();
            if (t.getRevealCorrectPhoto() != null) return t.getRevealCorrectPhoto().getPhotoUrl();
        }
        return resolvedPhotoUrl(t);
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
                        t.getId(),
                        t.getAthlete().getName(),
                        t.getReplacedAthlete() != null ? t.getReplacedAthlete().getName() : null))
                .collect(Collectors.toList());
    }

    private ImposterGridSummaryDto toSummaryDto(ImposterGridSummaryProjection row) {
        ImposterGridSummaryDto dto = new ImposterGridSummaryDto();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setDescription(row.getDescription());
        dto.setSport(row.getSport());
        dto.setTileCount(row.getTileCount().intValue());
        dto.setImposterCount(row.getImposterCount().intValue());
        return dto;
    }
}
