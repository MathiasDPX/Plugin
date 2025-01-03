package fr.openmc.core.features.city.milestones;

import fr.openmc.core.features.city.City;
import lombok.Getter;

import java.util.function.Consumer;

public class MilestoneLevel {
    @Getter private int goal;
    private Consumer<City> reward;

    public MilestoneLevel(int goal, Consumer<City> reward) {
        this.goal = goal;
        this.reward = reward;
    }

    public void giveReward(City city) {
        reward.accept(city);
    }
}