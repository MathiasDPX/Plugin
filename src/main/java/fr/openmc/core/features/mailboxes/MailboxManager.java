package fr.openmc.core.features.mailboxes;


import fr.openmc.core.OMCPlugin;
import fr.openmc.core.commands.CommandsManager;
import fr.openmc.core.features.mailboxes.letter.LetterHead;
import fr.openmc.core.features.mailboxes.menu.PlayerMailbox;
import fr.openmc.core.features.mailboxes.menu.letter.Letter;
import fr.openmc.core.features.mailboxes.utils.MailboxInv;
import fr.openmc.core.features.mailboxes.utils.MailboxMenuManager;
import fr.openmc.core.utils.database.DatabaseManager;
import fr.openmc.core.utils.serializer.BukkitSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.openmc.core.features.mailboxes.utils.MailboxUtils.*;

// Author Gexary
public class MailboxManager {
    private static final OMCPlugin plugin = OMCPlugin.getInstance();

    public MailboxManager() {
        OMCPlugin.registerEvents(
                new MailboxListener()
        );

        CommandsManager.getHandler().register(
            new MailboxCommand(OMCPlugin.getInstance())
        );
    }

    public static void init_db(Connection conn) throws SQLException {
        conn.prepareStatement("CREATE TABLE IF NOT EXISTS mailbox_items (" +
                "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "sender_id VARCHAR(36) NOT NULL," +
                "receiver_id VARCHAR(36) NOT NULL," +
                "items BLOB NOT NULL," +
                "items_count INT NOT NULL," +
                "sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "refused BOOLEAN NOT NULL DEFAULT FALSE" +
                ")").executeUpdate();
    }

    public static boolean sendItems(Player sender, OfflinePlayer receiver, ItemStack[] items) {
        String receiverUUID = receiver.getUniqueId().toString();
        if (!canSend(sender, receiver)) return false;
        String receiverName = receiver.getName();
        int itemsCount = Arrays.stream(items).mapToInt(ItemStack::getAmount).sum();
        String senderUUID = sender.getUniqueId().toString();

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO `mailbox_items` (sender_id, receiver_id, items, items_count) VALUES (?, ?, ?, ?);", PreparedStatement.RETURN_GENERATED_KEYS)) {
            byte[] itemsBytes = BukkitSerializer.serializeItemStacks(items);
            statement.setString(1, senderUUID);
            statement.setString(2, receiverUUID);
            statement.setBytes(3, itemsBytes);
            statement.setInt(4, itemsCount);
            if (statement.executeUpdate() == 0) return false;
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (result.next()) {
                    int id = result.getInt(1);
                    Player receiverPlayer = receiver.getPlayer();
                    if (receiverPlayer != null) {
                        if (MailboxMenuManager.playerInventories.get(receiverPlayer) instanceof PlayerMailbox receiverMailbox) {
                            LetterHead letterHead = new LetterHead(sender, id, itemsCount, result.getTimestamp(1).toLocalDateTime());
                            receiverMailbox.addLetter(letterHead);
                        } else sendNotification(receiverPlayer, itemsCount, id, sender.getName());
                    }
                    sendSuccessSendingMessage(sender, receiverName, itemsCount);
                    return true;
                }
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sendFailureSendingMessage(sender, receiverName);
            return false;
        }
    }

    public static void sendItemsToAOfflinePlayerBatch(Map<OfflinePlayer, ItemStack[]> playerItemsMap) {
        String insertQuery = "INSERT INTO `mailbox_items` (sender_id, receiver_id, items, items_count) VALUES (?, ?, ?, ?);";

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<OfflinePlayer, ItemStack[]> entry : playerItemsMap.entrySet()) {
                OfflinePlayer player = entry.getKey();
                ItemStack[] items = entry.getValue();

                String receiverUUID = player.getUniqueId().toString();
                int itemsCount = Arrays.stream(items).mapToInt(ItemStack::getAmount).sum();
                String senderUUID = player.getUniqueId().toString();

                byte[] itemsBytes = BukkitSerializer.serializeItemStacks(items);

                statement.setString(1, senderUUID);
                statement.setString(2, receiverUUID);
                statement.setBytes(3, itemsBytes);
                statement.setInt(4, itemsCount);

                statement.addBatch();
            }

            int[] results = statement.executeBatch();

            for (int result : results) {
                if (result == PreparedStatement.EXECUTE_FAILED) {
                    System.out.println("Une des insertions a échoué.");
                }
            }

        } catch (SQLException sqlEx) {
            Logger.getLogger(MailboxManager.class.getName()).log(Level.SEVERE, "Erreur lors de l'envoi des items batch à des joueurs hors ligne", sqlEx);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // todo
    public static boolean canSend(Player sender, OfflinePlayer receiver) {
        return true;
    }

    private static void sendNotification(Player receiver, int itemsCount, int id, String name) {
        Component message = Component.text("Vous avez reçu ", NamedTextColor.DARK_GREEN)
                                     .append(Component.text(itemsCount, NamedTextColor.GREEN))
                                     .append(Component.text(" item" + (itemsCount > 1 ? "s" : "") + " de la part de ", NamedTextColor.DARK_GREEN))
                                     .append(Component.text(name, NamedTextColor.GREEN))
                                     .append(Component.text("\nCliquez-ici", NamedTextColor.YELLOW))
                                     .clickEvent(getRunCommand("open " + id))
                                     .hoverEvent(getHoverEvent("Ouvrir la lettre #" + id))
                                     .append(Component.text(" pour ouvrir la lettre", NamedTextColor.GOLD));
        sendSuccessMessage(receiver, message);
        Title titleComponent = getTitle(itemsCount, name);
        receiver.playSound(receiver.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
        receiver.showTitle(titleComponent);
    }

    private static @NotNull Title getTitle(int itemsCount, String name) {
        Component subtitle = Component.text(name, NamedTextColor.GOLD).append(Component.text(" vous a envoyé ", NamedTextColor.YELLOW))
                                      .append(Component.text(itemsCount, NamedTextColor.GOLD))
                                      .append(Component.text(" item" + (itemsCount > 1 ? "s" : ""), NamedTextColor.YELLOW));
        Component title = Component.text("Nouvelle lettre !", NamedTextColor.GREEN);
        return Title.title(title, subtitle);
    }

    private static void sendFailureSendingMessage(Player player, String receiverName) {
        Component message = Component.text("Une erreur est apparue lors de l'envoie des items à ", NamedTextColor.DARK_RED)
                                     .append(Component.text(receiverName, NamedTextColor.RED));
        sendFailureMessage(player, message);
    }

    private static void sendSuccessSendingMessage(Player player, String receiverName, int itemsCount) {
        Component message = Component.text(itemsCount, NamedTextColor.GREEN)
                                     .append(Component.text(" " + getItemCount(itemsCount) + " envoyé" + (itemsCount > 1 ? "s" : "") + " à ", NamedTextColor.DARK_GREEN))
                                     .append(Component.text(receiverName, NamedTextColor.GREEN));
        sendSuccessMessage(player, message);
    }

    public static void givePlayerItems(Player player, ItemStack[] items) {
        HashMap<Integer, ItemStack> remainingItems = player.getInventory().addItem(items);
        for (ItemStack item : remainingItems.values())
            player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    public static void cancelLetter(Player player, int id) {
        MailboxInv inv = MailboxMenuManager.playerInventories.get(player);
        if (inv instanceof PlayerMailbox playerMailbox) {
            playerMailbox.removeLetter(id);
        } else if (inv instanceof Letter letter) {
            letter.cancel();
        }
    }
}
