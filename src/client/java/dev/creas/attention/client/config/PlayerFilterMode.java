package dev.creas.attention.client.config;

public enum PlayerFilterMode {
	ALL_PLAYERS,
	ONLY_LISTED_PLAYERS,
	ALL_EXCEPT_LISTED_PLAYERS;

	public String label() {
		return switch (this) {
			case ALL_PLAYERS -> "All players";
			case ONLY_LISTED_PLAYERS -> "Only listed players";
			case ALL_EXCEPT_LISTED_PLAYERS -> "All except listed";
		};
	}

	public PlayerFilterMode next() {
		return switch (this) {
			case ALL_PLAYERS -> ONLY_LISTED_PLAYERS;
			case ONLY_LISTED_PLAYERS -> ALL_EXCEPT_LISTED_PLAYERS;
			case ALL_EXCEPT_LISTED_PLAYERS -> ALL_PLAYERS;
		};
	}
}
