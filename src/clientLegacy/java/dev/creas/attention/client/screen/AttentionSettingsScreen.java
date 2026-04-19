package dev.creas.attention.client.screen;

import dev.creas.attention.client.config.AttentionConfigManager;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public final class AttentionSettingsScreen extends AttentionSettingsScreenBase {
	public AttentionSettingsScreen(Screen parent, AttentionConfigManager configManager) {
		super(parent, configManager);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_TAB && completePlayerSearchName()) {
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		return button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handlePlayerListClick(mouseX, mouseY);
	}
}
