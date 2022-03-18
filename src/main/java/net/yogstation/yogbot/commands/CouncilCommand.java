package net.yogstation.yogbot.commands;

import java.util.Arrays;
import java.util.List;

public class CouncilCommand extends ImageCommand {
	@Override
	protected List<String> getImages() {
		return Arrays.asList(
			"https://cdn.discordapp.com/attachments/134720091576205312/871910842164183070/8PQmYdL.png",
			"https://cdn.discordapp.com/attachments/734475284446707753/826240137225830400/image0.png", //Ashcorr gets fooled by Morderhel
			"https://cdn.discordapp.com/attachments/734475284446707753/804697809323556864/unknown.png", //Get a sense of humor
			"https://cdn.discordapp.com/attachments/734475284446707753/804192496322084864/ban.png", //Banned by public vote
			"https://cdn.discordapp.com/attachments/734475284446707753/800864882870059028/image0.png" //Council don't know hotkeys
		);
	}

	@Override
	protected String getTitle() {
		return "Council Image";
	}

	@Override
	public String getName() {
		return "council";
	}
}
