package net.yogstation.yogbot.listeners.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import net.yogstation.yogbot.DatabaseManager;
import net.yogstation.yogbot.config.DiscordConfig;
import net.yogstation.yogbot.permissions.PermissionsManager;
import net.yogstation.yogbot.util.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

@Component
public class ActivityCommand extends PermissionsCommand {

	private static final String activityQueryFormat = """
			/*
MIT License

Copyright (c) 2021 alexkar598

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED D"AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

SELECT adminlist.ckey as 'Ckey',
	COALESCE(round((SELECT SUM(rolelog.delta)
	FROM %s as rolelog
		WHERE rolelog.ckey = adminlist.ckey
			AND rolelog.job = 'Admin'
			AND rolelog.datetime > (Now() - INTERVAL 2 week)) / 60, 1), 0) as Activity,
	adminlist.rank as AdminRank
FROM %s as adminlist
JOIN %s as ranklist ON adminlist.rank = ranklist.`rank`;
			""";

	private static final List<String> exemptRanks = Arrays.asList(
			"Host", "Council Member", "RetCoder", "Tribunal", "Retired Admin",
			"Senior Coder", "Head Developer", "Maintainer", "Admin Observer",
			"#Forum Mod", "Bot", "Community Manager"
	);

	private static final List<String> ignore_ranks = Arrays.asList("Maintainer", "Bot");

	private final DatabaseManager database;
	
	public ActivityCommand(DiscordConfig discordConfig, PermissionsManager permissions, DatabaseManager database) {
		super(discordConfig, permissions);
		this.database = database;
	}
	
	@Override
	public Mono<?> doCommand(MessageCreateEvent event) {
		try {
			Mono<?> action = Mono.empty();
			Connection conn = database.getConnection();
			Statement activityStatement = conn.createStatement();
			ResultSet activityResults = activityStatement.executeQuery(String.format(activityQueryFormat,
                 database.prefix("role_time_log"), database.prefix("admin"), database.prefix("admin_ranks") ));

			List<Activity> activityData = new ArrayList<>();
			int adminLen = 8;
			int rankLen = 4;
			while(activityResults.next()) {
				if(ignore_ranks.contains(activityResults.getString("AdminRank"))) continue;

				Activity activityDatum = new Activity(
						activityResults.getString("Ckey"),
						activityResults.getString("AdminRank"),
						activityResults.getFloat("Activity")
				);
				activityData.add(activityDatum);
				if(activityDatum.ckey().length() > adminLen) adminLen = activityDatum.ckey().length();
				if(activityDatum.rank().length() > rankLen) rankLen = activityDatum.rank().length();
			}
			activityResults.close();
			activityStatement.close();
			activityData.sort(Activity::compareTo);

			Set<String> loaAdmins = new HashSet<>();
			Statement loaStatement = conn.createStatement();
			ResultSet loaResults = loaStatement.executeQuery(String.format("SELECT ckey from %s WHERE Now() < expiry_time && revoked IS NULL;", database.prefix("loa")));

			while(loaResults.next()) loaAdmins.add(ckey_ize(loaResults.getString("ckey")));
			loaResults.close();
			loaStatement.close();
			conn.close();

			StringBuilder output = new StringBuilder("```diff\n");

			StringBuilder title = new StringBuilder("  ");
			title.append(StringUtils.center("Username", adminLen));
			title.append(" ");
			title.append(StringUtils.center("Rank", rankLen));
			title.append(" Activity");
			output.append(title);

			output.append('\n');
			output.append(StringUtils.padStart("", title.length(), '='));
			output.append('\n');

			for(Activity activity : activityData) {
				StringBuilder line = new StringBuilder();
				boolean loa = loaAdmins.contains(activity.ckey());
				boolean exempt = exemptRanks.contains(activity.rank());

				if(activity.activity() >= 12) line.append('+');
				else if(loa || exempt) line.append(' ');
				else line.append('-');
				line.append(' ');

				line.append(StringUtils.padStart(activity.ckey(), adminLen));
				line.append(' ');
				line.append(StringUtils.padStart(activity.rank(), rankLen));
				line.append(' ');
				line.append(StringUtils.padStart(String.format("%.1f", activity.activity()), 8));
				line.append(' ');
				if(loa) line.append("(LOA)");
				else if(exempt) line.append("(Exempt)");
				line.append('\n');

				if(output.length() + line.length() > 1990) {
					output.append("```");
					action = action.and(send(event, output.toString()));
					output.setLength(0); // Empty the string builder
					output.append("```diff\n");
				}

				output.append(line);
			}

			output.append("```");
			action = action.and(send(event, output.toString()));
			return action;

		} catch (SQLException e) {
			LOGGER.error("Error getting activity", e);
			return reply(event, "Unable to reach the database, try again later");
		}
	}

	@Override
	protected String getRequiredPermissions() {
		return "note";
	}

	@Override
	protected String getDescription() {
		return "Activity fun.";
	}

	@Override
	public String getName() {
		return "activity";
	}

	private String ckey_ize(String key) {
		return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}
	
	record Activity(String ckey, String rank,
	                       float activity) implements Comparable<Activity> {
		
		@Override
		public String ckey() {
			return ckey;
		}
		
		@Override
		public String rank() {
			return rank;
		}
		
		@Override
		public float activity() {
			return activity;
		}
		
		@Override
		public int compareTo(Activity o) {
			return Float.compare(o.activity, activity);
		}
	}
}