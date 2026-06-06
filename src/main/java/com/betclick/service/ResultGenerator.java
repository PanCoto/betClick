package com.betclick.service;

import com.betclick.model.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ResultGenerator {

    private final long seed;

    public ResultGenerator(@Value("${app.random.seed:42}") long seed) {
        this.seed = seed;
    }

    public SimulatedResult generateResult(Event event) {
        long eventSeed = this.seed + event.getId();
        Random rand = new Random(eventSeed);

        String sport = "";
        if (event.getLeague() != null && event.getLeague().getSport() != null) {
            sport = event.getLeague().getSport().getName();
        }

        int scoreA = 0;
        int scoreB = 0;

        if (sport == null) {
            sport = "";
        }

        switch (sport.toLowerCase()) {
            case "piłka nożna":
            case "pilka nozna":
                scoreA = rand.nextInt(5);
                scoreB = rand.nextInt(5);
                break;

            case "koszykówka":
            case "koszykowka":
                scoreA = 70 + rand.nextInt(51);
                scoreB = 70 + rand.nextInt(51);
                while (scoreA == scoreB) {
                    scoreA += rand.nextInt(5) + 1;
                }
                break;

            case "tenis":
                if (rand.nextBoolean()) {
                    scoreA = 2;
                    scoreB = rand.nextBoolean() ? 0 : 1;
                } else {
                    scoreA = rand.nextBoolean() ? 0 : 1;
                    scoreB = 2;
                }
                break;

            case "siatkówka":
            case "siatkowka":
                if (rand.nextBoolean()) {
                    scoreA = 3;
                    scoreB = rand.nextInt(3);
                } else {
                    scoreA = rand.nextInt(3);
                    scoreB = 3;
                }
                break;

            case "hokej":
                scoreA = rand.nextInt(8);
                scoreB = rand.nextInt(8);
                break;

            case "boks":
            case "mma":
                if (rand.nextBoolean()) {
                    scoreA = 1;
                    scoreB = 0;
                } else {
                    scoreA = 0;
                    scoreB = 1;
                }
                break;

            case "żużel":
            case "zuzel":
                scoreA = 30 + rand.nextInt(31);
                scoreB = 30 + rand.nextInt(31);
                break;

            default:
                scoreA = rand.nextInt(6);
                scoreB = rand.nextInt(6);
                break;
        }

        return new SimulatedResult(scoreA, scoreB);
    }

    public static class SimulatedResult {
        private final int scoreA;
        private final int scoreB;

        public SimulatedResult(int scoreA, int scoreB) {
            this.scoreA = scoreA;
            this.scoreB = scoreB;
        }

        public int getScoreA() {
            return scoreA;
        }

        public int getScoreB() {
            return scoreB;
        }
    }
}
