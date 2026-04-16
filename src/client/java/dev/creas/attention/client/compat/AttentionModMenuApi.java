package dev.creas.attention.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.creas.attention.client.AttentionClient;

public final class AttentionModMenuApi implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AttentionClient::createSettingsScreen;
	}
}
