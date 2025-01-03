package fr.openmc.core.features.city.milestones;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Milestone {
    @Getter private String name;
    @Getter private ItemStack icon;
    @Getter private List<MilestoneLevel> levels;

    public Milestone(String name, ItemStack icon, List<MilestoneLevel> levels) {
        this.name = name;
        this.icon = icon;
        this.levels = levels;
    }
}