package fr.openmc.core.features.city.milestones;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MilestoneManager {
    private static Map<String, Milestone> milestones = new HashMap<>();

    public static void registerMilestone(Milestone milestone) {
        milestones.put(milestone.getName(), milestone);
    }

    public static Milestone getMilestone(String name) {
        return milestones.get(name);
    }

    public static void init() {
        MilestoneLevel level1 = new MilestoneLevel(10, city -> city.updateBalance(100.0));
        MilestoneLevel level2 = new MilestoneLevel(20, city -> city.updateBalance(200.0));
        MilestoneLevel level3 = new MilestoneLevel(30, city -> city.updateBalance(300.0));

        Milestone milestone = new Milestone("Milestone 1", new ItemStack(Material.COBBLESTONE), List.of(level1, level2, level3));

        registerMilestone(milestone);
    }
}
