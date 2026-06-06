package com.betclick.service;

import com.betclick.model.Market;
import com.betclick.model.Selection;
import com.betclick.repository.MarketRepository;
import com.betclick.repository.SelectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WinningSelectionResolver {

    private static final Logger log = LoggerFactory.getLogger(WinningSelectionResolver.class);
    private static final Pattern THRESHOLD_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    private final MarketRepository marketRepository;
    private final SelectionRepository selectionRepository;

    public WinningSelectionResolver(MarketRepository marketRepository, SelectionRepository selectionRepository) {
        this.marketRepository = marketRepository;
        this.selectionRepository = selectionRepository;
    }

    public List<Long> resolveWinningSelections(Long eventId, int resultA, int resultB) {
        List<Long> winningIds = new ArrayList<>();
        List<Market> markets = marketRepository.findByEventIdAndIsActiveTrue(eventId);

        log.debug("Resolving winning selections for event ID {} (result={}-{}). Found {} active markets.",
                eventId, resultA, resultB, markets.size());

        for (Market market : markets) {
            List<Selection> selections = selectionRepository.findByMarketIdAndIsActiveTrue(market.getId());
            String marketName = market.getName().toLowerCase();

            if (isWinnerMarket(marketName)) {
                log.debug("Market '{}' (ID {}) classified as Winner Market.", market.getName(), market.getId());
                for (Selection selection : selections) {
                    String name = selection.getName().trim();
                    if (resultA > resultB) {
                        if (name.equals("1") || name.startsWith("1 ") || name.startsWith("1(")) {
                            winningIds.add(selection.getId());
                            log.debug("Selection '{}' (ID {}) wins.", selection.getName(), selection.getId());
                        }
                    } else if (resultA < resultB) {
                        if (name.equals("2") || name.startsWith("2 ") || name.startsWith("2(")) {
                            winningIds.add(selection.getId());
                            log.debug("Selection '{}' (ID {}) wins.", selection.getName(), selection.getId());
                        }
                    } else {
                        String lowerName = name.toLowerCase();
                        if (lowerName.equals("x") || lowerName.startsWith("x ") || lowerName.startsWith("x(") || lowerName.contains("remis")) {
                            winningIds.add(selection.getId());
                            log.debug("Selection '{}' (ID {}) wins.", selection.getName(), selection.getId());
                        }
                    }
                }
            } else if (isOverUnderMarket(marketName)) {
                double threshold = parseThreshold(market.getName());
                double totalScore = resultA + resultB;
                log.debug("Market '{}' (ID {}) classified as Over/Under Market with threshold {}. Total score: {}",
                        market.getName(), market.getId(), threshold, totalScore);

                for (Selection selection : selections) {
                    String name = selection.getName().toLowerCase();
                    if (name.contains("powyżej") || name.contains("powyzej") || name.contains("over") || name.contains(">")) {
                        if (totalScore > threshold) {
                            winningIds.add(selection.getId());
                            log.debug("Selection '{}' (ID {}) wins ({} > {}).", selection.getName(), selection.getId(), totalScore, threshold);
                        }
                    } else if (name.contains("poniżej") || name.contains("ponizej") || name.contains("under") || name.contains("<")) {
                        if (totalScore < threshold) {
                            winningIds.add(selection.getId());
                            log.debug("Selection '{}' (ID {}) wins ({} < {}).", selection.getName(), selection.getId(), totalScore, threshold);
                        }
                    }
                }
            } else {
                log.warn("Market '{}' (ID {}) name not recognized. Skipping auto-settlement for this market.",
                        market.getName(), market.getId());
            }
        }

        return winningIds;
    }

    private boolean isWinnerMarket(String name) {
        return name.contains("zwycięzca") || name.contains("zwyciezca") ||
               name.contains("winner") || name.contains("1x2") || name.contains("12");
    }

    private boolean isOverUnderMarket(String name) {
        return name.contains("powyżej/poniżej") || name.contains("powyzej/ponizej") ||
               name.contains("over/under") || name.contains("powyżej / poniżej") ||
               name.contains("powyzej / ponizej") || name.contains("suma goli") ||
               name.contains("suma punktów") || name.contains("suma punktow");
    }

    private double parseThreshold(String name) {
        Matcher matcher = THRESHOLD_PATTERN.matcher(name);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group());
            } catch (NumberFormatException e) {
                log.debug("Could not parse threshold from market name '{}'", name, e);
            }
        }
        return 2.5;
    }
}
