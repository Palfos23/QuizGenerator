package com.quizapp.repository;

import com.quizapp.model.GamePlayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GamePlayEventRepository extends JpaRepository<GamePlayEvent, Long> {

    // Grouped in the database rather than pulling every row into memory - unlike
    // the small tables StatisticsService otherwise tallies in Java, this one
    // grows forever.
    @Query("select e.gameType as gameType, count(e) as total from GamePlayEvent e group by e.gameType")
    List<GameTypeCount> countByGameType();

    interface GameTypeCount {
        String getGameType();
        long getTotal();
    }
}
