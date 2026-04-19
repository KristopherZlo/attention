package dev.creas.attention.client.screen;

import dev.creas.attention.client.config.AttentionConfigManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

public final class AttentionSettingsScreen extends AttentionSettingsScreenBase {
	public AttentionSettingsScreen(Screen parent, AttentionConfigManager configManager) {
		super(parent, configManager);
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
}
