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
		addDrawableChild(ButtonWidget.builder(Text.literal("Defaults"), button -> {
			loadDraft(AttentionConfig.defaults());
			syncWidgetsFromDraft();
		}).dimensions(layout.right(), layout.buttonY(), actionWidth, CONTROL_HEIGHT).build());
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
				AttentionConfig.parsePlayerFilterText(playerFilterText)
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
	private void saveAndClose() {
		configManager.setConfig(configManager.getConfig().withIndicatorRadiusPixels(indicatorRadiusPixels));
		close();
	}

	private static double valueToRadius(double sliderValue) {
		double min = AttentionConfig.minIndicatorRadiusPixels();
		double max = AttentionConfig.maxIndicatorRadiusPixels();
		return min + clamp01(sliderValue) * (max - min);
	}

	private static double radiusToValue(double radiusPixels) {
		double min = AttentionConfig.minIndicatorRadiusPixels();
		double max = AttentionConfig.maxIndicatorRadiusPixels();
		return clamp01((radiusPixels - min) / (max - min));
	}

	private static double clamp01(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	private final class RadiusSliderWidget extends SliderWidget {
		private RadiusSliderWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Text.literal(""), radiusToValue(indicatorRadiusPixels));
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal("Marker radius: " + Math.round(indicatorRadiusPixels) + " px"));
		}

		@Override
		protected void applyValue() {
			indicatorRadiusPixels = valueToRadius(value);
			updateMessage();
		}
	}
}

