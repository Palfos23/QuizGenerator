package com.quizapp.service;

import com.quizapp.dto.AthleteDto;
import com.quizapp.dto.AthleteDescriptionDto;
import com.quizapp.dto.AthletePhotoDto;
import com.quizapp.dto.AthleteUsageDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.Athlete;
import com.quizapp.model.AthleteDescription;
import com.quizapp.model.AthletePhoto;
import com.quizapp.model.BullseyeEntry;
import com.quizapp.model.FiveOhOneEntry;
import com.quizapp.model.Grid;
import com.quizapp.model.ImposterTile;
import com.quizapp.model.Lineup;
import com.quizapp.model.PenaltyKick;
import com.quizapp.repository.AthleteDescriptionRepository;
import com.quizapp.repository.AthletePhotoRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeEntryRepository;
import com.quizapp.repository.FiveOhOneEntryRepository;
import com.quizapp.repository.GridCandidateRepository;
import com.quizapp.repository.GridEntryRepository;
import com.quizapp.repository.GridRepository;
import com.quizapp.repository.ImposterTileRepository;
import com.quizapp.repository.LineupCandidateRepository;
import com.quizapp.repository.LineupEntryRepository;
import com.quizapp.repository.LineupRepository;
import com.quizapp.repository.PenaltyKickRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final GridCandidateRepository gridCandidateRepository;
    private final GridEntryRepository gridEntryRepository;
    private final GridRepository gridRepository;
    private final AthletePhotoRepository athletePhotoRepository;
    private final AthleteDescriptionRepository athleteDescriptionRepository;
    private final LineupCandidateRepository lineupCandidateRepository;
    private final LineupEntryRepository lineupEntryRepository;
    private final LineupRepository lineupRepository;
    private final BullseyeEntryRepository bullseyeEntryRepository;
    private final FiveOhOneEntryRepository fiveOhOneEntryRepository;
    private final ImposterTileRepository imposterTileRepository;
    private final PenaltyKickRepository penaltyKickRepository;

    public AthleteService(AthleteRepository athleteRepository, GridCandidateRepository gridCandidateRepository,
                           GridEntryRepository gridEntryRepository, GridRepository gridRepository,
                           AthletePhotoRepository athletePhotoRepository,
                           AthleteDescriptionRepository athleteDescriptionRepository,
                           LineupCandidateRepository lineupCandidateRepository,
                           LineupEntryRepository lineupEntryRepository,
                           LineupRepository lineupRepository,
                           BullseyeEntryRepository bullseyeEntryRepository,
                           FiveOhOneEntryRepository fiveOhOneEntryRepository,
                           ImposterTileRepository imposterTileRepository,
                           PenaltyKickRepository penaltyKickRepository) {
        this.athleteRepository = athleteRepository;
        this.gridCandidateRepository = gridCandidateRepository;
        this.gridEntryRepository = gridEntryRepository;
        this.gridRepository = gridRepository;
        this.athletePhotoRepository = athletePhotoRepository;
        this.athleteDescriptionRepository = athleteDescriptionRepository;
        this.lineupCandidateRepository = lineupCandidateRepository;
        this.lineupEntryRepository = lineupEntryRepository;
        this.lineupRepository = lineupRepository;
        this.bullseyeEntryRepository = bullseyeEntryRepository;
        this.fiveOhOneEntryRepository = fiveOhOneEntryRepository;
        this.imposterTileRepository = imposterTileRepository;
        this.penaltyKickRepository = penaltyKickRepository;
    }

    // Wraps the plain static toDto with the athlete's additional photos -
    // used only where the photo library actually matters (the Subjects admin
    // page, the grid editor's candidate/entry picker), not every place an
    // AthleteDto gets built, to avoid an extra query per athlete in contexts
    // (player-facing search, pool listings) that never display it.
    // For a single athlete only (create/update) - for a whole list, use
    // toDtosWithPhotos below instead, which batches the query.
    AthleteDto toDtoWithPhotos(Athlete a) {
        AthleteDto dto = toDto(a);
        dto.setAdditionalPhotos(athletePhotoRepository.findByAthlete_IdOrderByIdAsc(a.getId()).stream()
                .map(AthleteService::toPhotoDto)
                .collect(Collectors.toList()));
        dto.setAdditionalDescriptions(athleteDescriptionRepository.findByAthlete_IdOrderByIdAsc(a.getId()).stream()
                .map(AthleteService::toDescriptionDto)
                .collect(Collectors.toList()));
        return dto;
    }

    // Batch version for lists - one query for every athlete's photos combined,
    // instead of one query per athlete. Critical once the list is in the
    // thousands (findAll/search on the Subjects admin page) - the per-athlete
    // version above does the exact same job correctly for one item, but does
    // real damage at list scale.
    List<AthleteDto> toDtosWithPhotos(List<Athlete> athletes) {
        List<Long> ids = athletes.stream().map(Athlete::getId).collect(Collectors.toList());
        Map<Long, List<AthletePhotoDto>> photosByAthleteId = athletePhotoRepository.findByAthlete_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAthlete().getId(),
                        Collectors.mapping(AthleteService::toPhotoDto, Collectors.toList())));
        Map<Long, List<AthleteDescriptionDto>> descriptionsByAthleteId = athleteDescriptionRepository.findByAthlete_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getAthlete().getId(),
                        Collectors.mapping(AthleteService::toDescriptionDto, Collectors.toList())));
        return athletes.stream().map(a -> {
            AthleteDto dto = toDto(a);
            dto.setAdditionalPhotos(photosByAthleteId.getOrDefault(a.getId(), List.of()));
            dto.setAdditionalDescriptions(descriptionsByAthleteId.getOrDefault(a.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }

    private static AthletePhotoDto toPhotoDto(AthletePhoto p) {
        AthletePhotoDto dto = new AthletePhotoDto();
        dto.setId(p.getId());
        dto.setPhotoUrl(p.getPhotoUrl());
        dto.setLabel(p.getLabel());
        return dto;
    }

    private static AthleteDescriptionDto toDescriptionDto(AthleteDescription d) {
        AthleteDescriptionDto dto = new AthleteDescriptionDto();
        dto.setId(d.getId());
        dto.setText(d.getText());
        dto.setLabel(d.getLabel());
        return dto;
    }

    // Replaces the full set of additional photos to match what was sent -
    // same full-replacement approach as pool membership: anything not in the
    // new list gets deleted, anything without an id is newly created,
    // anything with a matching id and unchanged content is left alone.
    private void applyAdditionalPhotos(Athlete athlete, List<AthletePhotoDto> incoming) {
        List<AthletePhoto> existing = athletePhotoRepository.findByAthlete_IdOrderByIdAsc(athlete.getId());
        Set<Long> incomingIds = incoming == null ? new HashSet<>() : incoming.stream()
                .map(AthletePhotoDto::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        for (AthletePhoto existingPhoto : existing) {
            if (!incomingIds.contains(existingPhoto.getId())) {
                athletePhotoRepository.delete(existingPhoto);
            }
        }
        if (incoming == null) return;
        for (AthletePhotoDto photoDto : incoming) {
            if (photoDto.getId() == null) {
                AthletePhoto photo = new AthletePhoto();
                photo.setAthlete(athlete);
                photo.setPhotoUrl(photoDto.getPhotoUrl());
                photo.setLabel(photoDto.getLabel());
                athletePhotoRepository.save(photo);
            } else {
                existing.stream().filter(p -> p.getId().equals(photoDto.getId())).findFirst().ifPresent(p -> {
                    p.setPhotoUrl(photoDto.getPhotoUrl());
                    p.setLabel(photoDto.getLabel());
                    athletePhotoRepository.save(p);
                });
            }
        }
    }

    // Same full-replacement approach as applyAdditionalPhotos above.
    private void applyAdditionalDescriptions(Athlete athlete, List<AthleteDescriptionDto> incoming) {
        List<AthleteDescription> existing = athleteDescriptionRepository.findByAthlete_IdOrderByIdAsc(athlete.getId());
        Set<Long> incomingIds = incoming == null ? new HashSet<>() : incoming.stream()
                .map(AthleteDescriptionDto::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        for (AthleteDescription existingDescription : existing) {
            if (!incomingIds.contains(existingDescription.getId())) {
                athleteDescriptionRepository.delete(existingDescription);
            }
        }
        if (incoming == null) return;
        for (AthleteDescriptionDto descriptionDto : incoming) {
            if (descriptionDto.getId() == null) {
                AthleteDescription description = new AthleteDescription();
                description.setAthlete(athlete);
                description.setText(descriptionDto.getText());
                description.setLabel(descriptionDto.getLabel());
                athleteDescriptionRepository.save(description);
            } else {
                existing.stream().filter(d -> d.getId().equals(descriptionDto.getId())).findFirst().ifPresent(d -> {
                    d.setText(descriptionDto.getText());
                    d.setLabel(descriptionDto.getLabel());
                    athleteDescriptionRepository.save(d);
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> findAll() {
        return toDtosWithPhotos(athleteRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<AthleteDto> search(String sport, String team, String nameContains) {
        List<Athlete> pool = sport != null ? athleteRepository.findBySport(sport) : athleteRepository.findAll();
        List<Athlete> filtered = pool.stream()
                .filter(a -> team == null || team.isBlank() || (a.getTeam() != null && a.getTeam().equalsIgnoreCase(team)))
                .filter(a -> nameContains == null || nameContains.isBlank()
                        || a.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .collect(Collectors.toList());
        return toDtosWithPhotos(filtered);
    }

    // Data-quality scan for the admin "Duplicate subjects" Insights page: within
    // each sport, groups together athletes whose names look like the same
    // person entered more than once - either an exact match once accents/
    // special letters are folded away (o/ae/a instead of o/ae/a with special
    // Nordic letters, accented Latin letters via Unicode normalization), or one
    // entry being just a last name that matches another entry's last name. This
    // is read-only and flags for manual review - it never merges or deletes
    // anything, since that would mean reassigning this athlete's Grid/Lineup/
    // Imposter board appearances, which needs a human decision about which
    // record should actually survive.
    // sport: optional - restricts the scan to one category instead of every
    // sport in the roster, so a large roster can be checked incrementally.
    // maxDistance: 0 = only an exact match (once accents/special letters are
    // normalized) counts as a duplicate; 1-3 additionally catches names that
    // are that many characters apart (typos, transliteration slips) once
    // normalized - clamped to [0,3] since a wider net starts matching
    // unrelated short names. The last-name-only check always runs regardless
    // of maxDistance, since it's a structural match (one entry is just a
    // surname), not something edit distance can express.
    @Transactional(readOnly = true)
    public List<com.quizapp.dto.AthleteDuplicateGroupDto> findDuplicateGroups(String sport, int maxDistance) {
        int distance = Math.max(0, Math.min(3, maxDistance));
        List<Athlete> pool = (sport != null && !sport.isBlank())
                ? athleteRepository.findBySport(sport)
                : athleteRepository.findAll();
        Map<String, List<Athlete>> bySport = pool.stream()
                .collect(Collectors.groupingBy(a -> a.getSport() == null ? "" : a.getSport().trim().toLowerCase()));

        List<com.quizapp.dto.AthleteDuplicateGroupDto> groups = new ArrayList<>();
        for (List<Athlete> athletes : bySport.values()) {
            if (athletes.size() < 2) continue;
            groups.addAll(clusterDuplicates(athletes, distance));
        }
        return groups;
    }

    private List<com.quizapp.dto.AthleteDuplicateGroupDto> clusterDuplicates(List<Athlete> athletes, int maxDistance) {
        int n = athletes.size();
        List<Set<String>> normalized = athletes.stream().map(a -> normalizeVariants(a.getName())).collect(Collectors.toList());
        // Union-find over this sport's athletes - two entries only need to be
        // linked by ANY pair inside a cluster (e.g. "E. Haaland" <-> "Erling
        // Haaland" <-> "Haaland") for the whole cluster to be reported together.
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matchReason(normalized.get(i), normalized.get(j), maxDistance) == null) continue;
                int ri = find(parent, i);
                int rj = find(parent, j);
                if (ri != rj) parent[ri] = rj;
            }
        }

        Map<Integer, List<Integer>> clusters = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }

        List<com.quizapp.dto.AthleteDuplicateGroupDto> groups = new ArrayList<>();
        for (List<Integer> indices : clusters.values()) {
            if (indices.size() < 2) continue;
            // Recompute reasons directly from the final cluster's members, rather
            // than trying to track them during union-find - simpler and always
            // correct, since a cluster is tiny (a handful of entries at most).
            Set<String> reasons = new java.util.LinkedHashSet<>();
            for (int a = 0; a < indices.size(); a++) {
                for (int b = a + 1; b < indices.size(); b++) {
                    String reason = matchReason(normalized.get(indices.get(a)), normalized.get(indices.get(b)), maxDistance);
                    if (reason != null) reasons.add(reason);
                }
            }
            List<Athlete> members = indices.stream().map(athletes::get).collect(Collectors.toList());
            groups.add(new com.quizapp.dto.AthleteDuplicateGroupDto(
                    members.get(0).getSport(), String.join("; ", reasons), toDtosWithPhotos(members)));
        }
        return groups;
    }

    private int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    // Exact-or-near match (after folding accents/special letters) catches
    // literal duplicates, spelling variants like "Ø. Hauge" vs "Oyvind
    // Hauge"'s surname, (via the two å variants below) "Håland" vs "Haaland",
    // and - once maxDistance > 0 - plain typos/transliteration slips within
    // that many characters. The closest pair across both sides' normalized
    // spellings wins. The last-name check catches an entry registered as
    // just "Haaland" that's really the same person as "Erling Haaland"
    // elsewhere - independent of maxDistance, since it's a word-level match,
    // not a character-level one.
    private String matchReason(Set<String> variantsA, Set<String> variantsB, int maxDistance) {
        if (variantsA.isEmpty() || variantsB.isEmpty()) return null;
        int best = Integer.MAX_VALUE;
        for (String a : variantsA) {
            for (String b : variantsB) {
                best = Math.min(best, levenshtein(a, b));
                if (best == 0) break;
            }
            if (best == 0) break;
        }
        if (best == 0) return "Same name once special letters/accents are normalized";
        if (best <= maxDistance) {
            return best + (best == 1 ? " character" : " characters") + " different once normalized";
        }
        for (String a : variantsA) {
            List<String> tokensA = List.of(a.split(" "));
            for (String b : variantsB) {
                List<String> tokensB = List.of(b.split(" "));
                if (tokensA.size() == 1 && tokensB.size() > 1 && tokensA.get(0).equals(tokensB.get(tokensB.size() - 1))) {
                    return "One entry looks like a last-name-only version of the other";
                }
                if (tokensB.size() == 1 && tokensA.size() > 1 && tokensB.get(0).equals(tokensA.get(tokensA.size() - 1))) {
                    return "One entry looks like a last-name-only version of the other";
                }
            }
        }
        return null;
    }

    // Classic edit-distance DP - cheap enough at these string lengths (person
    // names) to run for every pair within a sport group without needing any
    // fancier indexing.
    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // Folds case, Nordic/Germanic letters that Unicode normalization alone
    // won't decompose (o/ae/a/o are their own codepoints, not accented Latin
    // letters), then strips any remaining combining accents (e.g. e -> e,
    // n -> n) and punctuation, so "O'Brien" and "obrien" line up too.
    //
    // å gets TWO candidate foldings, not one: the simple single-letter fold
    // (a) alongside its historical two-letter transliteration (aa) - å is
    // literally a ligature of "aa", and that's still how it's often written in
    // ASCII-only contexts (most famously "Haaland" for "Håland"), so a single
    // fold would miss exactly that kind of pair. ø/æ only get one fold each
    // (o/ae) since there's no equivalent second convention for those.
    private Set<String> normalizeVariants(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        String lower = raw.trim().toLowerCase();
        Set<String> variants = new java.util.LinkedHashSet<>();
        for (String aReplacement : new String[]{"a", "aa"}) {
            String s = lower.replace("å", aReplacement)
                    .replace("ø", "o").replace("æ", "ae")
                    .replace("ö", "o").replace("ä", "a").replace("ü", "u")
                    .replace("ß", "ss").replace("þ", "th").replace("ð", "d")
                    .replace("ł", "l").replace("đ", "d");
            s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            s = s.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
            if (!s.isEmpty()) variants.add(s);
        }
        return variants;
    }

    @Transactional
    public AthleteDto create(AthleteDto dto) {
        Athlete athlete = new Athlete();
        athlete.setName(dto.getName());
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        athlete = athleteRepository.save(athlete);
        applyAdditionalPhotos(athlete, dto.getAdditionalPhotos());
        applyAdditionalDescriptions(athlete, dto.getAdditionalDescriptions());
        return toDtoWithPhotos(athlete);
    }

    // Bullseye/501's "these names weren't found as subjects" prompt - accepting
    // it creates a batch of bare name+sport athletes in one request (no photos/
    // team yet, same as any newly typed-in subject - an admin fills those in
    // later from the Subjects page). One transaction for the whole batch rather
    // than N separate create() calls, so a mid-batch failure doesn't leave a
    // half-imported set of subjects behind.
    @Transactional
    public List<AthleteDto> createBulk(List<AthleteDto> dtos) {
        List<Athlete> athletes = dtos.stream().map(dto -> {
            Athlete athlete = new Athlete();
            athlete.setName(dto.getName());
            athlete.setSport(dto.getSport());
            athlete.setTeam(dto.getTeam());
            return athlete;
        }).collect(Collectors.toList());
        return athleteRepository.saveAll(athletes).stream().map(AthleteService::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AthleteDto update(Long id, AthleteDto dto) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No athlete found with id " + id));
        String newName = dto.getName() != null ? dto.getName().trim() : null;
        if (newName != null && !newName.isBlank()
                && athleteRepository.existsBySportAndNameIgnoreCaseAndIdNot(dto.getSport(), newName, id)) {
            throw new IllegalArgumentException(
                    "Another subject named \"" + newName + "\" already exists in \"" + dto.getSport() + "\".");
        }
        athlete.setName(newName);
        athlete.setSport(dto.getSport());
        athlete.setTeam(dto.getTeam());
        athlete.setPhotoUrl(dto.getPhotoUrl());
        athlete = athleteRepository.save(athlete);
        applyAdditionalPhotos(athlete, dto.getAdditionalPhotos());
        applyAdditionalDescriptions(athlete, dto.getAdditionalDescriptions());
        return toDtoWithPhotos(athlete);
    }

    private List<Grid> findGridsReferencingAthlete(Long athleteId) {
        Map<Long, Grid> byId = new LinkedHashMap<>();
        gridRepository.findByCandidateAthleteId(athleteId).forEach(g -> byId.put(g.getId(), g));
        gridRepository.findByEntryAthleteId(athleteId).forEach(g -> byId.put(g.getId(), g));
        return new ArrayList<>(byId.values());
    }

    private List<Lineup> findLineupsReferencingAthlete(Long athleteId) {
        Map<Long, Lineup> byId = new LinkedHashMap<>();
        lineupRepository.findByCandidateAthleteId(athleteId).forEach(l -> byId.put(l.getId(), l));
        lineupRepository.findByEntryAthleteId(athleteId).forEach(l -> byId.put(l.getId(), l));
        return new ArrayList<>(byId.values());
    }

    // Every game type that can reference a subject, not just Grid/Starting XI -
    // shown to an admin before a delete is blocked (see delete() below) so the
    // message names the actual quiz instead of failing with a raw DB error.
    @Transactional(readOnly = true)
    public List<AthleteUsageDto> findUsage(Long athleteId) {
        List<AthleteUsageDto> usage = new ArrayList<>();

        for (Grid g : findGridsReferencingAthlete(athleteId)) {
            boolean isAnswer = g.getEntries().stream().anyMatch(e -> e.getAthlete().getId().equals(athleteId));
            usage.add(new AthleteUsageDto("Grid", g.getId(), g.getTitle(), isAnswer));
        }
        for (Lineup l : findLineupsReferencingAthlete(athleteId)) {
            boolean isAnswer = l.getEntries().stream().anyMatch(e -> e.getAthlete().getId().equals(athleteId));
            usage.add(new AthleteUsageDto("Starting XI", l.getId(), l.getTitle(), isAnswer));
        }
        for (BullseyeEntry e : bullseyeEntryRepository.findByAthlete_Id(athleteId)) {
            usage.add(new AthleteUsageDto("Bullseye", e.getQuestion().getId(), e.getQuestion().getTitle(), true));
        }
        for (FiveOhOneEntry e : fiveOhOneEntryRepository.findByAthlete_Id(athleteId)) {
            usage.add(new AthleteUsageDto("501", e.getCategory().getId(), e.getCategory().getTitle(), true));
        }
        for (ImposterTile t : imposterTileRepository.findByAthlete_Id(athleteId)) {
            usage.add(new AthleteUsageDto("Imposter", t.getImposterGrid().getId(), t.getImposterGrid().getTitle(), true));
        }
        for (PenaltyKick k : penaltyKickRepository.findByAthlete_Id(athleteId)) {
            usage.add(new AthleteUsageDto("Penalty Shootout", k.getShootout().getId(), k.getShootout().getTitle(), true));
        }
        return usage;
    }

    @Transactional
    public void delete(Long id, boolean force) {
        if (!athleteRepository.existsById(id)) {
            throw new ResourceNotFoundException("No athlete found with id " + id);
        }
        boolean blocked = gridCandidateRepository.existsByAthlete_Id(id)
                || lineupCandidateRepository.existsByAthlete_Id(id)
                || bullseyeEntryRepository.existsByAthlete_Id(id)
                || fiveOhOneEntryRepository.existsByAthlete_Id(id)
                || imposterTileRepository.existsByAthlete_Id(id)
                || penaltyKickRepository.existsByAthlete_Id(id);
        if (blocked) {
            if (!force) {
                throw new IllegalArgumentException(
                        "This subject is used in one or more quizzes - remove it from those first.");
            }
            // Direct delete statements, not collection-based removal (load the
            // collection, remove an element, let Hibernate's orphanRemoval figure
            // out the SQL). That approach proved unreliable across several attempts
            // for this exact scenario - a plain DELETE ... WHERE athlete_id = ? has
            // no ambiguity for Hibernate to get wrong.
            gridEntryRepository.deleteByAthlete_Id(id);
            gridCandidateRepository.deleteByAthlete_Id(id);
            lineupEntryRepository.deleteByAthlete_Id(id);
            lineupCandidateRepository.deleteByAthlete_Id(id);
            bullseyeEntryRepository.deleteByAthlete_Id(id);
            fiveOhOneEntryRepository.deleteByAthlete_Id(id);
            imposterTileRepository.deleteByAthlete_Id(id);
            penaltyKickRepository.deleteByAthlete_Id(id);
        }
        // Historical "who this imposter replaced" data, not a live displayed
        // reference - never blocks a delete, just forgotten along with it
        // (nullable field, same as athletePhotoRepository's cleanup below).
        imposterTileRepository.clearReplacedAthlete(id);
        athletePhotoRepository.deleteByAthlete_Id(id);
        athleteRepository.deleteById(id);
    }

    static AthleteDto toDto(Athlete a) {
        AthleteDto dto = new AthleteDto();
        dto.setId(a.getId());
        dto.setName(a.getName());
        dto.setSport(a.getSport());
        dto.setTeam(a.getTeam());
        dto.setPhotoUrl(a.getPhotoUrl());
        return dto;
    }
}
