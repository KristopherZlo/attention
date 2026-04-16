package dev.creas.attention.client.screen;

import dev.creas.attention.client.config.AttentionConfig;
import dev.creas.attention.client.config.AttentionConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class AttentionSettingsScreen extends Screen {
	private final Screen parent;
	private final AttentionConfigManager configManager;
	private double indicatorRadiusPixels;

	public AttentionSettingsScreen(Screen parent, AttentionConfigManager configManager) {
		super(Text.literal("Attention"));
		this.parent = parent;
		this.configManager = configManager;
		this.indicatorRadiusPixels = configManager.getConfig().indicatorRadiusPixels();
	}

	@Override
	protected void init() {
		int contentWidth = 220;
		int left = (width - contentWidth) / 2;
		int top = height / 2 - 24;

		addDrawableChild(new RadiusSliderWidget(left, top, contentWidth, 20));
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> saveAndClose())
				.dimensions(left, top + 30, 106, 20)
				.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
				.dimensions(left + 114, top + 30, 106, 20)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		renderBackground(context, mouseX, mouseY, deltaTicks);
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 52, 0xFFFFFFFF);
	}

	@Override
	public void close() {
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
