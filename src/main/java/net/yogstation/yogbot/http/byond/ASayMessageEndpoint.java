package net.yogstation.yogbot.http.byond;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.core.GatewayDiscordClient;
import net.yogstation.yogbot.DatabaseManager;
import net.yogstation.yogbot.config.DiscordConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ASayMessageEndpoint extends MessageEndpoint {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	
	public ASayMessageEndpoint(WebClient webClient, ObjectMapper mapper, DatabaseManager database,
	                           GatewayDiscordClient client, DiscordConfig discordConfig) {
		super(webClient, mapper, database, client, discordConfig);
	}
	
	@Override
	public String getMethod() {
		return "asaymessage";
	}
	
	@Override
	protected String getWebhookUrl() {
		return discordConfig.asayWebhookUrl;
	}
}