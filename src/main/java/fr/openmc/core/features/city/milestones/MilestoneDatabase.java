package fr.openmc.core.features.city.milestones;

import fr.openmc.core.features.city.City;
import fr.openmc.core.utils.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class MilestoneDatabase {
    private static HashMap<City, HashMap<String, MilestoneProgress>> progress = new HashMap<>();

    public static void loadForCity(City city) {
        try {
            if (progress.containsKey(city)) {
                return;
            }

            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("SELECT * FROM milestone_progress WHERE city_uuid = ?");
            statement.setString(1, city.getUUID());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String milestoneName = rs.getString("milestone_name");
                int currentProgress = rs.getInt("current_progress");
                int currentLevel = rs.getInt("current_level");

                Milestone milestone = MilestoneManager.getMilestone(milestoneName);
                if (milestone == null) {
                    continue;
                }

                MilestoneProgress milestoneProgress = new MilestoneProgress(city, milestone);
                milestoneProgress.setCurrentProgress(currentProgress);
                milestoneProgress.setCurrentLevel(currentLevel);

                if (!progress.containsKey(city)) {
                    progress.put(city, new HashMap<>());
                }

                progress.get(city).put(milestoneName, milestoneProgress);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveMilestoneProgress(MilestoneProgress progress) {
        try {
            PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO milestone_progress" +
                    "(city_uuid, milestone_name, current_progress, current_level) VALUES (?, ?, ?, ?)" +
                    "ON DUPLICATE KEY UPDATE current_progress = ?, current_level = ?");
            statement.setString(1, progress.getCity().getUUID());
            statement.setString(2, progress.getMilestone().getName());
            statement.setInt(3, progress.getCurrentProgress());
            statement.setInt(4, progress.getCurrentLevel());
            statement.setInt(5, progress.getCurrentProgress());
            statement.setInt(6, progress.getCurrentLevel());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static MilestoneProgress loadMilestoneProgress(City city, Milestone milestone) {
        loadForCity(city);
        return progress.get(city).get(milestone.getName());
    }
}