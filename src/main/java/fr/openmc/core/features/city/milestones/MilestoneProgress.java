package fr.openmc.core.features.city.milestones;

import fr.openmc.core.features.city.City;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MilestoneProgress {
    @Getter private City city;
    @Getter private Milestone milestone;
    @Getter @Setter private int currentProgress;
    @Getter @Setter private int currentLevel;

    public MilestoneProgress(City city, Milestone milestone) {
        this.city = city;
        this.milestone = milestone;
        this.currentProgress = 0;
        this.currentLevel = 0;
    }

    public void incrementProgress(int amount) {
        this.currentProgress += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        List<MilestoneLevel> levels = milestone.getLevels();
        if (currentLevel >= levels.size()) return;

        MilestoneLevel nextLevel = levels.get(currentLevel);
        if (currentProgress >= nextLevel.getGoal()) {
            nextLevel.giveReward(city);
            currentLevel++;

            MilestoneDatabase.saveMilestoneProgress(this);
        }
    }

    private boolean isLevelMax() {
        return currentLevel >= milestone.getLevels().size();
    }
}