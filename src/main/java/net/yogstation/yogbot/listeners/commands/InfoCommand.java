package net.yogstation.yogbot.listeners.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import net.yogstation.yogbot.ByondConnector;
import net.yogstation.yogbot.config.ByondConfig;
import net.yogstation.yogbot.config.DiscordChannelsConfig;
import net.yogstation.yogbot.config.DiscordConfig;
import net.yogstation.yogbot.util.Result;
import net.yogstation.yogbot.util.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InfoCommand extends TextCommand {
	private static final Set<String> timerModes = Set.of(
		"call",
		"recall",
		"igniting",
		"docked",
		"escape",
		"landing"
	);
	
	private final ByondConnector byondConnector;
	private final ByondConfig byondConfig;
	private final DiscordChannelsConfig channelsConfig;
	
	public InfoCommand(DiscordConfig discordConfig, ByondConnector byondConnector, ByondConfig byondConfig,
	                   DiscordChannelsConfig channelsConfig) {
		super(discordConfig);
		this.byondConnector = byondConnector;
		this.byondConfig = byondConfig;
		this.channelsConfig = channelsConfig;
	}
	
	@Override
	protected Mono<?> doCommand(MessageCreateEvent event) {
		String admins = AdminWhoCommand.getAdmins(event.getMessage().getChannelId(), byondConnector, channelsConfig);
		admins = admins.replaceAll("\t", "");
		
		Result<Object, String> pingResponse = byondConnector.request("?ping");
		if(pingResponse.hasError()) return reply(event, pingResponse.getError());
		int playerCount = (int) (float) (Float) pingResponse.getValue();
		
		Result<Object, String> statusResponse = byondConnector.request("?status");
		if(statusResponse.hasError()) return reply(event, statusResponse.getError());
		String statusString = (String) statusResponse.getValue();
		statusString = statusString.replaceAll("\0", "");
		LOGGER.info(statusString);
		Map<String, List<String>> statusValues = StringUtils.splitQuery(statusString);

		int roundDuration = Integer.parseInt(statusValues.getOrDefault("round_duration", List.of("0")).get(0)) / 60;
		String shuttleMode = statusValues.getOrDefault("shuttle_mode", List.of("idle")).get(0);
		String shuttleModeDisplay = switch (shuttleMode) {
			case "igniting", "docked" -> "Docked";
			case "recall" -> "Recalled";
			case "call", "landing" -> "Called";
			case "stranded" -> "Disabled";
			case "escape" -> "Departed";
			case "endgame: game over" -> "Round Over";
			case "recharging" -> "Charging";
			default -> "Idle";
		};
		
		String shuttle_timer = statusValues.getOrDefault("shuttle_timer", List.of("0")).get(0);
		int shuttleTime = Integer.parseInt(shuttle_timer) / 60;
		String roundId = statusValues.getOrDefault("round_id", List.of("Unknown")).get(0);
		if(roundId == null || roundId.equals("")) roundId = "Unknown";
		String securityLevel = statusValues.getOrDefault("security_level", List.of("Unknown")).get(0);
		Color embedColor = Color.of(switch (securityLevel) {
			case "green" -> 0x12A125;
			case "blue" -> 0x1242A1;
			case "red" -> 0xA11212;
			case "gamma" -> 0xD6690A;
			case "epsilon" -> 0x617444;
			case "delta" -> 0x2E0340;
			default -> 0x000000;
		});
		var embedBuilder = EmbedCreateSpec.builder()
			.color(embedColor)
			.author("Information", byondConfig.serverJoinAddress, "https://i.imgur.com/GPZgtbe.png")
			.description(String.format("Join the server now at %s", byondConfig.serverJoinAddress))
			.addField("Players online:", Integer.toString(playerCount), true)
			.addField("Current round:", roundId, true)
			.addField("Round duration:", roundDuration + " minutes", true)
			.addField("Shuttle status:", shuttleModeDisplay, true);
		if(timerModes.contains(shuttleMode))
			embedBuilder.addField("Shuttle timer:",  String.format("%d minutes", shuttleTime), true);
		embedBuilder.addField("Security level:", securityLevel, true);
		if(admins.trim().length() > 0) {
			embedBuilder.addField("Admins online:", admins, false);
		}
		return reply(event, embedBuilder.build());
	}

	@Override
	protected String getDescription() {
		return "Pings the server and names the admins";
	}

	@Override
	public String getName() {
		return "info";
	}
}