package com.quizapp.service;

import com.quizapp.exception.GameAccessDeniedException;
import com.quizapp.model.AppUser;
import com.quizapp.model.BattleGameType;
import com.quizapp.model.RoomGameType;
import com.quizapp.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

// The single place that knows the per-game play-access rule - see AppUser's
// canPlayX flags. A live DB lookup per call rather than a JWT claim, on
// purpose: flipping a flag in the database should take effect immediately,
// not just on the account's next login.
@Service
public class PlayAccessService {

    private final AppUserRepository appUserRepository;

    public PlayAccessService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public void requireTensionAccess(Authentication authentication) {
        require(authentication, "Tension", AppUser::isCanPlayTension);
    }

    public void requireGridBattleAccess(Authentication authentication) {
        require(authentication, "Grid Battle", AppUser::isCanPlayGridBattle);
    }

    public void requireFiveOhOneAccess(Authentication authentication) {
        require(authentication, "501", AppUser::isCanPlayFiveOhOne);
    }

    public void requireImposterAccess(Authentication authentication) {
        require(authentication, "Imposter", AppUser::isCanPlayImposter);
    }

    public void requireStartingXiBattleAccess(Authentication authentication) {
        require(authentication, "XI Battle", AppUser::isCanPlayStartingXiBattle);
    }

    public void requireBullseyeAccess(Authentication authentication) {
        require(authentication, "Bullseye", AppUser::isCanPlayBullseye);
    }

    public void requirePenaltyShootoutAccess(Authentication authentication) {
        require(authentication, "Penalty Shootout", AppUser::isCanPlayPenaltyShootout);
    }

    // For RoomController.create/join, where the game being started is only known at runtime.
    public void requireAccessForGameType(Authentication authentication, RoomGameType type) {
        switch (type) {
            case TENSION -> requireTensionAccess(authentication);
            case GRID_BATTLE -> requireGridBattleAccess(authentication);
            case FIVE_O_ONE -> requireFiveOhOneAccess(authentication);
            case IMPOSTER -> requireImposterAccess(authentication);
            case STARTING_XI_BATTLE -> requireStartingXiBattleAccess(authentication);
        }
    }

    // For GamePlayEventController, where a pass-and-play session self-reports
    // that it finished (see GamePlayEvent) - same access rule as actually
    // opening that game, so the stats endpoint can't be used to probe or pad
    // counts for a game this account isn't even allowed to play.
    public void requireAccessForBattleGameType(Authentication authentication, BattleGameType type) {
        switch (type) {
            case GRID_BATTLE -> requireGridBattleAccess(authentication);
            case FIVE_O_ONE -> requireFiveOhOneAccess(authentication);
            case IMPOSTER -> requireImposterAccess(authentication);
            case STARTING_XI_BATTLE -> requireStartingXiBattleAccess(authentication);
            case BULLSEYE -> requireBullseyeAccess(authentication);
        }
    }

    // For PlayAccessController's upfront "can this account even open this
    // game" check - keyed by the same short string each frontend *View.vue
    // uses as its route/game identifier.
    public void requireAccessForKey(Authentication authentication, String gameKey) {
        switch (gameKey) {
            case "tension" -> requireTensionAccess(authentication);
            case "grid-battle" -> requireGridBattleAccess(authentication);
            case "501" -> requireFiveOhOneAccess(authentication);
            case "imposter" -> requireImposterAccess(authentication);
            case "starting-xi-battle" -> requireStartingXiBattleAccess(authentication);
            case "bullseye" -> requireBullseyeAccess(authentication);
            case "penalty-shootout" -> requirePenaltyShootoutAccess(authentication);
            default -> throw new IllegalArgumentException("Unknown game: " + gameKey);
        }
    }

    // Admin accounts are a completely separate credential system (AdminUser,
    // not AppUser) - an admin JWT's subject wouldn't resolve to an AppUser row
    // to begin with, so this check is mostly belt-and-suspenders, but it keeps
    // the exemption explicit rather than relying on that lookup failing quietly.
    private void require(Authentication authentication, String gameLabel, Predicate<AppUser> hasAccess) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        AppUser user = appUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new GameAccessDeniedException(gameLabel));
        if (!hasAccess.test(user)) {
            throw new GameAccessDeniedException(gameLabel);
        }
    }
}
