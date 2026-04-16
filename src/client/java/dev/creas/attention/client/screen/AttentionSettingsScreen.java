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
