package dev.creas.attention.client.screen;

import dev.creas.attention.client.config.AttentionConfig;
import dev.creas.attention.client.config.AttentionConfigManager;
import dev.creas.attention.client.config.PlayerFilterMode;
import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.client.hud.AttentionMarkerRenderer;
import dev.creas.attention.marker.MarkerRadiusMath;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public final class AttentionSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 212;
	private static final int MIN_PANEL_WIDTH = 170;
	private static final int MAX_PREVIEW_HEIGHT = 142;
	private static final int CONTROL_HEIGHT = 20;
	private static final int ROW_HEIGHT = 24;
	private static final int PLAYER_ROW_HEIGHT = 28;
	private static final int PLAYER_FACE_SIZE = 18;
	private static final int PLAYER_LIST_BUTTON_WIDTH = 54;
	private static final int GUTTER = 12;

	private final Screen parent;
	private final AttentionConfigManager configManager;

	private SettingsPage page = SettingsPage.RADII;
	private double detectionRadiusBlocks;
	private double minIndicatorRadiusPixels;
	private double maxIndicatorRadiusPixels;
	private boolean reactOnlyInSurvival;
	private boolean reactToPlayers;
	private boolean reactToTargetingHostiles;
	private boolean reactToApproachingHostiles;
	private PlayerFilterMode playerFilterMode;
	private String playerFilterText;
	private String playerSearchText = "";
	private double playerListScroll;

	private ValueSlider detectionRadiusSlider;
	private ValueSlider minRadiusSlider;
	private ValueSlider maxRadiusSlider;
	private ButtonWidget reactOnlyInSurvivalButton;
	private ButtonWidget reactToPlayersButton;
	private ButtonWidget reactToTargetingHostilesButton;
	private ButtonWidget reactToApproachingHostilesButton;
	private ButtonWidget playerFilterModeButton;
	private TextFieldWidget playerSearchField;

	public AttentionSettingsScreen(Screen parent, AttentionConfigManager configManager) {
		super(Text.literal("Attention Settings"));
		this.parent = parent;
		this.configManager = configManager;
		loadDraft(configManager.getConfig());
	}

	@Override
	protected void init() {
		detectionRadiusSlider = null;
		minRadiusSlider = null;
		maxRadiusSlider = null;
		reactOnlyInSurvivalButton = null;
		reactToPlayersButton = null;
		reactToTargetingHostilesButton = null;
		reactToApproachingHostilesButton = null;
		playerFilterModeButton = null;
		playerSearchField = null;

		Layout layout = layout();
		int tabWidth = (layout.panelWidth() - 8) / 3;
		addPageTab(layout.left(), layout.tabsY(), tabWidth, SettingsPage.RADII);
		addPageTab(layout.left() + tabWidth + 4, layout.tabsY(), tabWidth, SettingsPage.FILTERS);
		addPageTab(layout.left() + ((tabWidth + 4) * 2), layout.tabsY(), tabWidth, SettingsPage.PLAYERS);

		if (page == SettingsPage.RADII) {
			initRadiiPage(layout);
		} else if (page == SettingsPage.FILTERS) {
			initFiltersPage(layout);
		} else {
			initPlayersPage(layout);
		}

		int actionWidth = (layout.panelWidth() - 4) / 2;
		addDrawableChild(ButtonWidget.builder(Text.literal("Defaults"), button -> openDefaultsConfirmation())
				.dimensions(layout.right(), layout.buttonY(), actionWidth, CONTROL_HEIGHT)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> saveAndClose())
				.dimensions(layout.right() + actionWidth + 4, layout.buttonY(), actionWidth, CONTROL_HEIGHT)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Layout layout = layout();
		context.fill(0, 0, width, height, 0xAA101010);
		drawPreview(context, layout.right(), layout.contentY(), layout.panelWidth(), layout.previewHeight());
		drawLabels(context, layout);
		if (page == SettingsPage.PLAYERS) {
			drawPlayerList(context, layout, mouseX, mouseY);
		}
		super.render(context, mouseX, mouseY, deltaTicks);
		if (page == SettingsPage.PLAYERS) {
			drawPlayerSearchSuggestion(context);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (keyInput.key() == GLFW.GLFW_KEY_TAB && completePlayerSearchName()) {
			return true;
		}

		return super.keyPressed(keyInput);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubleClick) {
		if (super.mouseClicked(click, doubleClick)) {
			return true;
		}

		return click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handlePlayerListClick(click.x(), click.y());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (page == SettingsPage.PLAYERS && isInside(mouseX, mouseY, playerListBox(layout()))) {
			playerListScroll = clamp(playerListScroll - (verticalAmount * PLAYER_ROW_HEIGHT), 0.0D, maxPlayerListScroll(layout()));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void close() {
		saveDraft();

		if (client != null) {
			client.setScreen(parent);
		}
	}

	private void initRadiiPage(Layout layout) {
		int x = layout.left();
		int y = layout.contentY() + 24;
		detectionRadiusSlider = addDrawableChild(new ValueSlider(
				x,
				y,
				layout.panelWidth(),
				"Detection radius",
				AttentionConfig.minDetectionRadiusBlocks(),
				AttentionConfig.maxDetectionRadiusBlocks(),
				1.0D,
				() -> detectionRadiusBlocks,
				value -> detectionRadiusBlocks = value,
				value -> Math.round(value) + " blocks"
		));
		minRadiusSlider = addDrawableChild(new ValueSlider(
				x,
				y + ROW_HEIGHT,
				layout.panelWidth(),
				"Minimum marker radius",
				AttentionConfig.minimumIndicatorRadiusPixels(),
				AttentionConfig.maximumIndicatorRadiusPixels(),
				1.0D,
				() -> minIndicatorRadiusPixels,
				value -> {
					minIndicatorRadiusPixels = value;
					if (minIndicatorRadiusPixels > maxIndicatorRadiusPixels) {
						maxIndicatorRadiusPixels = minIndicatorRadiusPixels;
						if (maxRadiusSlider != null) {
							maxRadiusSlider.syncFromState();
						}
					}
				},
				value -> Math.round(value) + " px"
		));
		maxRadiusSlider = addDrawableChild(new ValueSlider(
				x,
				y + (ROW_HEIGHT * 2),
				layout.panelWidth(),
				"Maximum marker radius",
				AttentionConfig.minimumIndicatorRadiusPixels(),
				AttentionConfig.maximumIndicatorRadiusPixels(),
				1.0D,
				() -> maxIndicatorRadiusPixels,
				value -> {
					maxIndicatorRadiusPixels = value;
					if (maxIndicatorRadiusPixels < minIndicatorRadiusPixels) {
						minIndicatorRadiusPixels = maxIndicatorRadiusPixels;
						if (minRadiusSlider != null) {
							minRadiusSlider.syncFromState();
						}
					}
				},
				value -> Math.round(value) + " px"
		));
	}

	private void initFiltersPage(Layout layout) {
		int x = layout.left();
		int y = layout.contentY() + 24;
		reactOnlyInSurvivalButton = addDrawableChild(ButtonWidget.builder(toggleText("Survival only", reactOnlyInSurvival), button -> {
			reactOnlyInSurvival = !reactOnlyInSurvival;
			refreshToggleLabels();
		}).dimensions(x, y, layout.panelWidth(), CONTROL_HEIGHT).build());
		reactToTargetingHostilesButton = addDrawableChild(ButtonWidget.builder(toggleText("Hostiles targeting you", reactToTargetingHostiles), button -> {
			reactToTargetingHostiles = !reactToTargetingHostiles;
			refreshToggleLabels();
		}).dimensions(x, y + ROW_HEIGHT, layout.panelWidth(), CONTROL_HEIGHT).build());
		reactToApproachingHostilesButton = addDrawableChild(ButtonWidget.builder(toggleText("Hostiles approaching", reactToApproachingHostiles), button -> {
			reactToApproachingHostiles = !reactToApproachingHostiles;
			refreshToggleLabels();
		}).dimensions(x, y + (ROW_HEIGHT * 2), layout.panelWidth(), CONTROL_HEIGHT).build());
		reactToPlayersButton = addDrawableChild(ButtonWidget.builder(toggleText("React to players", reactToPlayers), button -> {
			reactToPlayers = !reactToPlayers;
			refreshToggleLabels();
		}).dimensions(x, y + (ROW_HEIGHT * 3), layout.panelWidth(), CONTROL_HEIGHT).build());
	}

	private void initPlayersPage(Layout layout) {
		int x = layout.left();
		int y = layout.contentY() + 24;
		playerSearchField = addDrawableChild(new TextFieldWidget(textRenderer, x, y, layout.panelWidth(), CONTROL_HEIGHT, Text.literal("Player search")));
		playerSearchField.setMaxLength(64);
		playerSearchField.setText(playerSearchText);
		playerSearchField.setPlaceholder(Text.literal("Filter online players"));
		playerSearchField.setChangedListener(text -> {
			playerSearchText = text;
			playerListScroll = 0.0D;
		});

		playerFilterModeButton = addDrawableChild(ButtonWidget.builder(playerFilterModeText(), button -> {
			playerFilterMode = playerFilterMode.next();
			refreshToggleLabels();
			refreshPlayerFilterState();
		}).dimensions(x, y + ROW_HEIGHT, layout.panelWidth(), CONTROL_HEIGHT).build());
		refreshPlayerFilterState();
	}

	private void addPageTab(int x, int y, int width, SettingsPage targetPage) {
		ButtonWidget tab = addDrawableChild(ButtonWidget.builder(Text.literal(targetPage.label), button -> {
			if (page != targetPage) {
				page = targetPage;
				clearAndInit();
			}
		}).dimensions(x, y, width, CONTROL_HEIGHT).build());
		tab.active = page != targetPage;
	}

	private void drawLabels(DrawContext context, Layout layout) {
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);

		if (page == SettingsPage.RADII) {
			context.drawTextWithShadow(textRenderer, Text.literal("Radius response"), layout.left(), layout.contentY(), 0xFFFFFFFF);
			context.drawTextWithShadow(textRenderer, Text.literal("Close threats use the minimum radius."), layout.left(), layout.contentY() + 102, 0xFFA0A0A0);
			context.drawTextWithShadow(textRenderer, Text.literal("Far threats use the maximum radius."), layout.left(), layout.contentY() + 114, 0xFFA0A0A0);
		} else if (page == SettingsPage.FILTERS) {
			context.drawTextWithShadow(textRenderer, Text.literal("Threat filters"), layout.left(), layout.contentY(), 0xFFFFFFFF);
			context.drawTextWithShadow(textRenderer, Text.literal("Default mode reacts only in survival."), layout.left(), layout.contentY() + 126, 0xFFA0A0A0);
		} else {
			context.drawTextWithShadow(textRenderer, Text.literal("Online players"), layout.left(), layout.contentY(), 0xFFFFFFFF);
			context.drawTextWithShadow(textRenderer, Text.literal("Listed: " + selectedPlayerNames().size()), layout.left() + layout.panelWidth() - 58, layout.contentY(), 0xFFA0A0A0);
		}
	}

	private void drawPreview(DrawContext context, int x, int y, int panelWidth, int panelHeight) {
		int panelColor = 0x66111111;
		int borderColor = 0x99FFFFFF;
		context.fill(x, y, x + panelWidth, y + panelHeight, panelColor);
		context.fill(x, y, x + panelWidth, y + 1, borderColor);
		context.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, borderColor);
		context.fill(x, y, x + 1, y + panelHeight, borderColor);
		context.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, borderColor);

		int centerX = x + (panelWidth / 2);
		int centerY = y + (panelHeight / 2);
		float timeSeconds = (System.currentTimeMillis() % 6000L) / 1000.0F;
		float previewAngle = AttentionMarkerController.normalizeRelativeAngle((timeSeconds * 58.0F) - 180.0F);
		float distanceFactor = clamp01((float) ((Math.sin(timeSeconds * 1.15F) + 1.0D) * 0.5D));
		float previewRadius = MarkerRadiusMath.visibleRadius((float) minIndicatorRadiusPixels, (float) maxIndicatorRadiusPixels, distanceFactor);
		float previewAlpha = 0.55F + (clamp01((float) ((Math.sin(timeSeconds * 2.1F) + 1.0D) * 0.5D)) * 0.45F);

		context.drawTextWithShadow(textRenderer, Text.literal("Preview"), x + 8, y + 8, 0xFFFFFFFF);
		drawCrosshair(context, centerX, centerY);
		AttentionMarkerRenderer.drawMarker(context, centerX, centerY, previewRadius, previewAngle, previewAlpha);
		context.drawTextWithShadow(textRenderer, Text.literal("Detection: " + Math.round(detectionRadiusBlocks) + " blocks"), x + 8, y + panelHeight - 26, 0xFFE6E6E6);
		context.drawTextWithShadow(textRenderer, Text.literal("Radius: " + Math.round(minIndicatorRadiusPixels) + "-" + Math.round(maxIndicatorRadiusPixels) + " px"), x + 8, y + panelHeight - 14, 0xFFE6E6E6);
	}

	private void drawCrosshair(DrawContext context, int centerX, int centerY) {
		int color = 0xFFFFFFFF;
		context.fill(centerX - 5, centerY, centerX + 6, centerY + 1, color);
		context.fill(centerX, centerY - 5, centerX + 1, centerY + 6, color);
	}

	private void saveAndClose() {
		close();
	}

	private void openDefaultsConfirmation() {
		if (client == null) {
			return;
		}

		client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						loadDraft(AttentionConfig.defaults());
					}

					if (client != null) {
						client.setScreen(this);
					}
				},
				Text.literal("Reset to defaults?"),
				Text.literal("Do you want to reset Attention settings to defaults?"),
				Text.literal("Reset"),
				Text.literal("Cancel")
		) {
			@Override
			public boolean shouldPause() {
				return false;
			}
		});
	}

	private void saveDraft() {
		configManager.setConfig(new AttentionConfig(
				detectionRadiusBlocks,
				minIndicatorRadiusPixels,
				maxIndicatorRadiusPixels,
				reactOnlyInSurvival,
				reactToPlayers,
				reactToTargetingHostiles,
				reactToApproachingHostiles,
				playerFilterMode,
				selectedPlayerNames()
		));
	}

	private void loadDraft(AttentionConfig config) {
		detectionRadiusBlocks = config.detectionRadiusBlocks();
		minIndicatorRadiusPixels = config.minIndicatorRadiusPixels();
		maxIndicatorRadiusPixels = config.maxIndicatorRadiusPixels();
		reactOnlyInSurvival = config.reactOnlyInSurvival();
		reactToPlayers = config.reactToPlayers();
		reactToTargetingHostiles = config.reactToTargetingHostiles();
		reactToApproachingHostiles = config.reactToApproachingHostiles();
		playerFilterMode = config.playerFilterMode();
		playerFilterText = AttentionConfig.formatPlayerFilterText(config.playerFilterNames());
	}

	private void syncWidgetsFromDraft() {
		if (detectionRadiusSlider != null) {
			detectionRadiusSlider.syncFromState();
		}
		if (minRadiusSlider != null) {
			minRadiusSlider.syncFromState();
		}
		if (maxRadiusSlider != null) {
			maxRadiusSlider.syncFromState();
		}
		if (playerSearchField != null) {
			playerSearchField.setText(playerSearchText);
		}
		refreshToggleLabels();
		refreshPlayerFilterState();
	}

	private void refreshToggleLabels() {
		if (reactOnlyInSurvivalButton != null) {
			reactOnlyInSurvivalButton.setMessage(toggleText("Survival only", reactOnlyInSurvival));
		}
		if (reactToPlayersButton != null) {
			reactToPlayersButton.setMessage(toggleText("React to players", reactToPlayers));
		}
		if (reactToTargetingHostilesButton != null) {
			reactToTargetingHostilesButton.setMessage(toggleText("Hostiles targeting you", reactToTargetingHostiles));
		}
		if (reactToApproachingHostilesButton != null) {
			reactToApproachingHostilesButton.setMessage(toggleText("Hostiles approaching", reactToApproachingHostiles));
		}
		if (playerFilterModeButton != null) {
			playerFilterModeButton.setMessage(playerFilterModeText());
		}
	}

	private void refreshPlayerFilterState() {
		if (playerSearchField == null) {
			return;
		}

		playerSearchField.active = true;
		playerSearchField.setEditable(true);
	}

	private boolean completePlayerSearchName() {
		if (client == null
				|| client.getNetworkHandler() == null
				|| playerSearchField == null
				|| !playerSearchField.active
				|| !playerSearchField.isFocused()) {
			return false;
		}

		String prefix = playerSearchText.trim();

		if (prefix.isEmpty()) {
			return true;
		}

		List<String> matches = onlinePlayerNamesStartingWith(prefix);

		if (matches.isEmpty()) {
			return true;
		}

		String completion = matches.size() == 1 ? matches.getFirst() : longestCommonPrefix(matches);
		if (completion.length() <= prefix.length()) {
			completion = matches.getFirst();
		}

		playerSearchText = completion;
		playerSearchField.setText(completion);
		playerSearchField.setCursorToEnd(false);
		playerListScroll = 0.0D;
		return true;
	}

	private static String longestCommonPrefix(List<String> values) {
		String prefix = values.getFirst();
		for (int valueIndex = 1; valueIndex < values.size(); valueIndex++) {
			String value = values.get(valueIndex);
			int maxLength = Math.min(prefix.length(), value.length());
			int prefixLength = 0;

			while (prefixLength < maxLength
					&& Character.toLowerCase(prefix.charAt(prefixLength)) == Character.toLowerCase(value.charAt(prefixLength))) {
				prefixLength++;
			}

			prefix = prefix.substring(0, prefixLength);
			if (prefix.isEmpty()) {
				return prefix;
			}
		}

		return prefix;
	}

	private void drawPlayerList(DrawContext context, Layout layout, int mouseX, int mouseY) {
		PlayerListBox box = playerListBox(layout);
		List<PlayerListEntry> players = filteredOnlinePlayers();
		List<String> listedNames = selectedPlayerNames();
		int maxScroll = maxPlayerListScroll(layout);
		playerListScroll = clamp(playerListScroll, 0.0D, maxScroll);
		int scroll = (int) Math.floor(playerListScroll);

		context.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), 0x55111111);
		context.fill(box.x(), box.y(), box.x() + box.width(), box.y() + 1, 0x66FFFFFF);
		context.fill(box.x(), box.y() + box.height() - 1, box.x() + box.width(), box.y() + box.height(), 0x44FFFFFF);

		if (players.isEmpty()) {
			String message = playerSearchText.isBlank() ? "No online players" : "No matching players";
			context.drawCenteredTextWithShadow(textRenderer, Text.literal(message), box.x() + (box.width() / 2), box.y() + 18, 0xFFA0A0A0);
			return;
		}

		context.enableScissor(box.x(), box.y(), box.x() + box.width(), box.y() + box.height());
		int firstIndex = Math.max(0, scroll / PLAYER_ROW_HEIGHT);
		int rowY = box.y() + (firstIndex * PLAYER_ROW_HEIGHT) - scroll;

		for (int index = firstIndex; index < players.size() && rowY < box.y() + box.height(); index++) {
			if (rowY + PLAYER_ROW_HEIGHT >= box.y()) {
				drawPlayerRow(context, box, players.get(index), listedNames, rowY, mouseX, mouseY);
			}
			rowY += PLAYER_ROW_HEIGHT;
		}

		context.disableScissor();
		drawPlayerListScrollbar(context, box, players.size(), maxScroll);
	}

	private void drawPlayerRow(DrawContext context, PlayerListBox box, PlayerListEntry entry, List<String> listedNames, int rowY, int mouseX, int mouseY) {
		String playerName = playerName(entry);
		boolean listed = listedNames.contains(normalizePlayerName(playerName));
		int buttonX = playerListButtonX(box);
		int buttonY = rowY + 5;
		boolean hovered = isInside(mouseX, mouseY, buttonX, buttonY, PLAYER_LIST_BUTTON_WIDTH, 18);
		int rowColor = hovered ? 0x331F1F1F : 0x22111111;

		context.fill(box.x() + 1, rowY, box.x() + box.width() - 1, rowY + PLAYER_ROW_HEIGHT - 1, rowColor);
		PlayerSkinDrawer.draw(context, entry.getSkinTextures(), box.x() + 5, rowY + 5, PLAYER_FACE_SIZE);
		context.drawTextWithShadow(textRenderer, trimToWidth(playerName, buttonX - box.x() - 34), box.x() + 29, rowY + 10, 0xFFFFFFFF);
		drawListButton(context, buttonX, buttonY, PLAYER_LIST_BUTTON_WIDTH, 18, listed ? "Remove" : "Add", listed, hovered);
	}

	private void drawListButton(DrawContext context, int x, int y, int width, int height, String label, boolean listed, boolean hovered) {
		int fill = listed ? 0xAA2A2A2A : 0xAA1F3323;
		int border = hovered ? 0xFFFFFFFF : 0x88FFFFFF;
		int textColor = listed ? 0xFFE6E6E6 : 0xFFFFFFFF;

		context.fill(x, y, x + width, y + height, fill);
		context.fill(x, y, x + width, y + 1, border);
		context.fill(x, y + height - 1, x + width, y + height, border);
		context.fill(x, y, x + 1, y + height, border);
		context.fill(x + width - 1, y, x + width, y + height, border);
		context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + (width / 2), y + 5, textColor);
	}

	private void drawPlayerListScrollbar(DrawContext context, PlayerListBox box, int playerCount, int maxScroll) {
		if (maxScroll <= 0) {
			return;
		}

		int contentHeight = playerCount * PLAYER_ROW_HEIGHT;
		int trackX = box.x() + box.width() - 3;
		int thumbHeight = Math.max(12, (box.height() * box.height()) / contentHeight);
		int thumbY = box.y() + (int) ((box.height() - thumbHeight) * (playerListScroll / maxScroll));

		context.fill(trackX, box.y() + 2, trackX + 1, box.y() + box.height() - 2, 0x44FFFFFF);
		context.fill(trackX - 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xAAFFFFFF);
	}

	private void drawPlayerSearchSuggestion(DrawContext context) {
		if (playerSearchField == null || !playerSearchField.isFocused()) {
			return;
		}

		String suggestion = firstOnlinePlayerStartingWith(playerSearchText);
		if (suggestion.isEmpty() || suggestion.length() <= playerSearchText.length()) {
			return;
		}

		String suffix = suggestion.substring(playerSearchText.length());
		int x = playerSearchField.getX() + 4 + textRenderer.getWidth(playerSearchText);
		int y = playerSearchField.getY() + ((playerSearchField.getHeight() - 8) / 2);
		context.drawText(textRenderer, suffix, x, y, 0x66FFFFFF, false);
	}

	private boolean handlePlayerListClick(double mouseX, double mouseY) {
		if (page != SettingsPage.PLAYERS) {
			return false;
		}

		Layout layout = layout();
		PlayerListBox box = playerListBox(layout);
		if (!isInside(mouseX, mouseY, box)) {
			return false;
		}

		List<PlayerListEntry> players = filteredOnlinePlayers();
		playerListScroll = clamp(playerListScroll, 0.0D, maxPlayerListScroll(layout));
		int index = (int) ((mouseY - box.y() + playerListScroll) / PLAYER_ROW_HEIGHT);
		if (index < 0 || index >= players.size()) {
			return false;
		}

		int rowY = box.y() + (index * PLAYER_ROW_HEIGHT) - (int) Math.floor(playerListScroll);
		int buttonX = playerListButtonX(box);
		int buttonY = rowY + 5;
		if (!isInside(mouseX, mouseY, buttonX, buttonY, PLAYER_LIST_BUTTON_WIDTH, 18)) {
			return false;
		}

		toggleListedPlayer(playerName(players.get(index)));
		return true;
	}

	private List<PlayerListEntry> filteredOnlinePlayers() {
		String query = normalizePlayerName(playerSearchText);
		return onlinePlayers().stream()
				.filter(entry -> query.isEmpty() || normalizePlayerName(playerName(entry)).contains(query))
				.toList();
	}

	private List<PlayerListEntry> onlinePlayers() {
		if (client == null || client.getNetworkHandler() == null) {
			return List.of();
		}

		return client.getNetworkHandler().getPlayerList().stream()
				.filter(entry -> !isLocalPlayer(entry))
				.filter(entry -> !playerName(entry).isBlank())
				.sorted(Comparator.comparing(entry -> normalizePlayerName(playerName(entry))))
				.toList();
	}

	private List<String> onlinePlayerNamesStartingWith(String prefix) {
		String normalizedPrefix = normalizePlayerName(prefix);
		return onlinePlayers().stream()
				.map(AttentionSettingsScreen::playerName)
				.filter(name -> normalizePlayerName(name).startsWith(normalizedPrefix))
				.distinct()
				.sorted(Comparator.comparing(AttentionSettingsScreen::normalizePlayerName))
				.toList();
	}

	private String firstOnlinePlayerStartingWith(String prefix) {
		if (prefix.isBlank()) {
			return "";
		}

		return onlinePlayerNamesStartingWith(prefix).stream().findFirst().orElse("");
	}

	private List<String> selectedPlayerNames() {
		String localPlayerName = normalizePlayerName(client == null || client.player == null ? "" : client.player.getGameProfile().name());
		return AttentionConfig.parsePlayerFilterText(playerFilterText).stream()
				.filter(name -> !name.equals(localPlayerName))
				.toList();
	}

	private void toggleListedPlayer(String playerName) {
		String normalizedName = normalizePlayerName(playerName);
		if (normalizedName.isEmpty() || isLocalPlayerName(normalizedName)) {
			return;
		}

		LinkedHashSet<String> names = new LinkedHashSet<>(selectedPlayerNames());
		if (!names.remove(normalizedName)) {
			names.add(normalizedName);
		}
		playerFilterText = AttentionConfig.formatPlayerFilterText(names);
	}

	private boolean isLocalPlayer(PlayerListEntry entry) {
		return client != null
				&& client.player != null
				&& (Objects.equals(entry.getProfile().id(), client.player.getGameProfile().id())
				|| normalizePlayerName(playerName(entry)).equals(normalizePlayerName(client.player.getGameProfile().name())));
	}

	private boolean isLocalPlayerName(String normalizedName) {
		return client != null
				&& client.player != null
				&& normalizedName.equals(normalizePlayerName(client.player.getGameProfile().name()));
	}

	private int maxPlayerListScroll(Layout layout) {
		int contentHeight = filteredOnlinePlayers().size() * PLAYER_ROW_HEIGHT;
		return Math.max(0, contentHeight - playerListBox(layout).height());
	}

	private PlayerListBox playerListBox(Layout layout) {
		int y = layout.contentY() + 72;
		int height = Math.max(32, layout.buttonY() - y - 4);
		return new PlayerListBox(layout.left(), y, layout.panelWidth(), height);
	}

	private static int playerListButtonX(PlayerListBox box) {
		return box.x() + box.width() - PLAYER_LIST_BUTTON_WIDTH - 8;
	}

	private String trimToWidth(String text, int maxWidth) {
		if (textRenderer.getWidth(text) <= maxWidth) {
			return text;
		}

		return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
	}

	private static String playerName(PlayerListEntry entry) {
		return entry.getProfile().name() == null ? "" : entry.getProfile().name();
	}

	private static String normalizePlayerName(String playerName) {
		return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
	}

	private Layout layout() {
		int panelWidth = Math.min(PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, (width - GUTTER - 32) / 2));
		int totalWidth = (panelWidth * 2) + GUTTER;
		int left = Math.max(8, (width - totalWidth) / 2);
		int right = left + panelWidth + GUTTER;
		int tabsY = 28;
		int contentY = 56;
		int buttonY = Math.max(contentY + 96, height - 26);
		int previewHeight = Math.max(92, Math.min(MAX_PREVIEW_HEIGHT, buttonY - contentY - 8));
		return new Layout(left, right, tabsY, contentY, buttonY, panelWidth, previewHeight);
	}

	private Text playerFilterModeText() {
		return Text.literal("Player filter mode: " + playerFilterMode.label());
	}

	private static Text toggleText(String label, boolean enabled) {
		return Text.literal(label + ": " + (enabled ? "ON" : "OFF"));
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean isInside(double mouseX, double mouseY, PlayerListBox box) {
		return isInside(mouseX, mouseY, box.x(), box.y(), box.width(), box.height());
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private enum SettingsPage {
		RADII("Radii"),
		FILTERS("Filters"),
		PLAYERS("Players");

		private final String label;

		SettingsPage(String label) {
			this.label = label;
		}
	}

	private record Layout(int left, int right, int tabsY, int contentY, int buttonY, int panelWidth, int previewHeight) {
	}

	private record PlayerListBox(int x, int y, int width, int height) {
	}

	private final class ValueSlider extends SliderWidget {
		private final String label;
		private final double minValue;
		private final double maxValue;
		private final double step;
		private final DoubleSupplier getter;
		private final DoubleConsumer setter;
		private final Function<Double, String> formatter;
		private boolean syncing;

		private ValueSlider(
				int x,
				int y,
				int width,
				String label,
				double minValue,
				double maxValue,
				double step,
				DoubleSupplier getter,
				DoubleConsumer setter,
				Function<Double, String> formatter
		) {
			super(x, y, width, CONTROL_HEIGHT, Text.literal(""), 0.0D);
			this.label = label;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.step = step;
			this.getter = getter;
			this.setter = setter;
			this.formatter = formatter;
			syncFromState();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(label + ": " + formatter.apply(currentValue())));
		}

		@Override
		protected void applyValue() {
			if (syncing) {
				return;
			}

			setter.accept(currentValue());
			updateMessage();
		}

		private void syncFromState() {
			syncing = true;
			value = toSliderValue(getter.getAsDouble());
			updateMessage();
			syncing = false;
		}

		private double currentValue() {
			double rawValue = minValue + (clamp01((float) value) * (maxValue - minValue));
			double snappedValue = Math.round(rawValue / step) * step;
			return Math.max(minValue, Math.min(maxValue, snappedValue));
		}

		private double toSliderValue(double actualValue) {
			double clampedValue = Math.max(minValue, Math.min(maxValue, actualValue));
			if (maxValue <= minValue) {
				return 0.0D;
			}

			return (clampedValue - minValue) / (maxValue - minValue);
		}
	}
}
