package me.josvth.trade.transaction.inventory;

import me.josvth.bukkitformatlibrary.message.FormattedMessage;
import me.josvth.bukkitformatlibrary.message.managers.MessageManager;
import me.josvth.trade.Trade;
import me.josvth.trade.transaction.offer.Offer;
import me.josvth.trade.transaction.offer.description.ExperienceOfferDescription;
import me.josvth.trade.transaction.offer.description.ItemOfferDescription;
import me.josvth.trade.transaction.offer.description.MoneyOfferDescription;
import me.josvth.trade.transaction.offer.description.OfferDescription;
import me.josvth.trade.transaction.inventory.slot.*;
import me.josvth.trade.util.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class LayoutManager {

    private final Trade plugin;
    private final MessageManager messageManager;

    private final Map<String, Layout> layouts = new HashMap<String, Layout>();

    public LayoutManager(Trade plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void load(ConfigurationSection layoutSection, ConfigurationSection messageSection, ConfigurationSection offerSection) {

        final Map<String, FormattedMessage> defaultMessages = getMessagesFromSection(messageSection);
        final Map<Class<? extends Offer>, OfferDescription> defaultOfferDescriptions = getOfferDescriptionsFromSection(offerSection);

        // First we load our default default layout
        final Layout defaultLayout = new Layout("default");
        loadLayout(defaultLayout, layoutSection.getDefaultSection().getConfigurationSection("default"), defaultMessages, defaultOfferDescriptions);

        // Then we loud the other layouts in the section. Overriding our default if necessary
        for (String key : layoutSection.getKeys(false)) {

            try {
                if (layoutSection.isConfigurationSection(key)) {
                    Layout layout = layouts.get(key);
                    if (layout == null) {
                        layout = new Layout(key);
                    }

                    loadLayout(layout, layoutSection.getConfigurationSection(key), defaultMessages, defaultOfferDescriptions);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }

    }

    private void loadLayout(Layout layout, ConfigurationSection section, Map<String, FormattedMessage> defaultMessages, Map<Class<? extends Offer>, OfferDescription> defaultOfferDescriptions) throws IllegalArgumentException {

        if (section == null) {
            throw new IllegalArgumentException("Section is null.");
        }

        layout.getMessages().putAll(defaultMessages);
        layout.getOfferDescriptions().putAll(defaultOfferDescriptions);

        layout.setRows(section.getInt("rows"));

        layout.setOfferSize(section.getInt("offer-size", 4));

        layout.setTitle(new FormattedMessage(messageManager.preformatMessage(section.getString("title"))));

        layout.setPermission(section.getString("permission"));
        layout.setPriority(section.getInt("priority", 0));
        layout.setShared(section.getBoolean("shared", true));

        final int slotSize = layout.getRows() * 9;

        // Load slots
        if (section.isConfigurationSection("slots")) {

            final Slot[] slots = new Slot[slotSize];

            for (String slotKey : section.getConfigurationSection("slots").getKeys(false)) {

                if (section.getConfigurationSection("slots").isConfigurationSection(slotKey)) {

                    final int slotID;

                    try {
                        slotID = Integer.parseInt(slotKey);
                    } catch (NumberFormatException e) {
                        // TODO add message
                        continue;
                    }

                    if (slotID < 0 || slotID >= slotSize) {
                        // TODO add message
                        continue;
                    }

                    final ConfigurationSection slotSection = section.getConfigurationSection("slots." + slotKey);

                    final String type = slotSection.getString("type");

                    Slot slot = null;

                    if ("accept".equalsIgnoreCase(type)) {
                        slot = new AcceptSlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("accept-item"), messageManager),
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("accepted-item"), messageManager)
                        );
                    } else if ("refuse".equalsIgnoreCase(type)) {
                        slot = new RefuseSlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("refuse-item"), messageManager)
                        );
                    } else if ("close".equalsIgnoreCase(type)) {
                        slot = new CloseSlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("close-item"), messageManager)
                        );
                    } else if ("status".equalsIgnoreCase(type)) {
                        slot = new StatusSlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("considering-item"), messageManager),
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("accepted-item"), messageManager)
                        );
                    } else if ("trade".equalsIgnoreCase(type)) {
                        slot = new TradeSlot(
                                slotID,
                                slotSection.getInt("slot", 0)
                        );
                    } else if ("mirror".equalsIgnoreCase(type)) {
                        slot = new MirrorSlot(
                                slotID,
                                slotSection.getInt("slot", 0)
                        );
                    } else if ("money".equalsIgnoreCase(type)) {
                        if (plugin.useEconomy()) {
                            slot = new MoneySlot(
                                    slotID,
                                    ItemStackUtils.fromSection(slotSection.getConfigurationSection("money-item"), messageManager),
                                    slotSection.getInt("small-modifier", 5),
                                    slotSection.getInt("large-modifier", 10)
                            );
                        }
                    } else if ("experience".equalsIgnoreCase(type)) {
                        slot = new ExperienceSlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("experience-item"), messageManager),
                                slotSection.getInt("small-modifier", 1),
                                slotSection.getInt("large-modifier", 5)
                        );
                    } else if ("dummy".equalsIgnoreCase(type)) {
                        slot = new DummySlot(
                                slotID,
                                ItemStackUtils.fromSection(slotSection.getConfigurationSection("dummy-item"), messageManager)
                        );
                    }

                    slots[slotID] = slot;

                }

            }

            layout.setSlots(slots);

        }

        // Load offer descriptions
        layout.getOfferDescriptions().putAll(getOfferDescriptionsFromSection(section.getConfigurationSection("offers")));

        // Load messages
        layout.setKeyWhenMissing(plugin.getGeneralConfiguration().getBoolean("debug-mode", false));
        layout.getMessages().putAll(getMessagesFromSection(section.getConfigurationSection("messages")));

        layouts.put(layout.getName(), layout);

    }

    private Map<String, FormattedMessage> getMessagesFromSection(ConfigurationSection section) {

        final Map<String, FormattedMessage> messages = new HashMap<String, FormattedMessage>();

        if (section == null) {
            return messages;
        }

        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            if (entry.getValue() instanceof String) {
                messages.put(entry.getKey(), new FormattedMessage(messageManager.preformatMessage((String) entry.getValue())));
            }
        }

        return messages;

    }

    private Map<Class<? extends Offer>, OfferDescription> getOfferDescriptionsFromSection(ConfigurationSection section) {

        final Map<Class<? extends Offer>, OfferDescription> offerDescriptions = new HashMap<Class<? extends Offer>, OfferDescription>();

        if (section == null) {
            return offerDescriptions;
        }

        for (String offerKey : section.getKeys(false)) {

            if (section.isConfigurationSection(offerKey)) {

                final ConfigurationSection offerSection = section.getConfigurationSection(offerKey);

                if ("item".equalsIgnoreCase(offerKey)) {
                    final ItemOfferDescription description = new ItemOfferDescription();
                    offerDescriptions.put(description.getOfferClass(), description);
                } else if ("experience".equalsIgnoreCase(offerKey)) {
                    final ExperienceOfferDescription description = new ExperienceOfferDescription();
                    description.setSmallModifier(offerSection.getInt("small-modifier"));
                    description.setLargeModifier(offerSection.getInt("large-modifier"));
                    description.setExperienceItem(ItemStackUtils.fromSection(offerSection.getConfigurationSection("experience-item"), messageManager));
                    description.setExperienceItemMirror(ItemStackUtils.fromSection(offerSection.getConfigurationSection("experience-item-mirror"), messageManager));
                    offerDescriptions.put(description.getOfferClass(), description);
                } else if ("money".equalsIgnoreCase(offerKey)) {
                    final MoneyOfferDescription description = new MoneyOfferDescription();
                    description.setSmallModifier(offerSection.getInt("small-modifier"));
                    description.setLargeModifier(offerSection.getInt("large-modifier"));
                    description.setMoneyItem(ItemStackUtils.fromSection(offerSection.getConfigurationSection("money-item"), messageManager));
                    description.setMoneyItemMirror(ItemStackUtils.fromSection(offerSection.getConfigurationSection("money-item-mirror"), messageManager));
                    offerDescriptions.put(description.getOfferClass(), description);
                }

            }

        }

        return offerDescriptions;

    }

    public void unload() {
        layouts.clear();
    }

    public Layout getLayout(String playerNameA, String playerNameB) {

        Layout found = getDefaultLayout();

        final Player playerA = Bukkit.getPlayerExact(playerNameA);
        final Player playerB = Bukkit.getPlayerExact(playerNameB);

        for (Layout layout : layouts.values()) {
            if ((found != null && layout.getPriority() > found.getPriority())) {

                if (layout.getPermission() != null) {

                    final boolean AHasPermission = playerA != null && playerA.hasPermission(layout.getPermission());
                    final boolean BHasPermission = playerB != null && playerB.hasPermission(layout.getPermission());

                    if (AHasPermission && BHasPermission || (layout.isShared() && (AHasPermission || BHasPermission))) {
                        found = layout;
                    }

                } else {
                    found = layout;
                }

            }
        }

        return found;

    }

    public Layout getDefaultLayout() {
        return layouts.get("default");
    }
}
