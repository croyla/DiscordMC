package org.animey.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Scanner;

public final class Discord extends JavaPlugin {
    public enum EventType{
        MESSAGE, ADVANCEMENT, DEATH, JOIN, LEAVE, START, STOP
    }
    private DiscordUtils discordUtils;
    public static class Config {
        public static String DISCORD_ROLE; // User set placeholders
        public static String DISCORD_ROLE_COLOR;
        public static String DISCORD_USERNAME;
        public static String DISCORD_MESSAGE_LINK;
        public static String DISCORD_USER_ID;
        public static String DISCORD_USER_COLOR;
        public static String DISCORD_NAME;
        public static String DISCORD_ROLES;
        public static String DISCORD_MESSAGE;
        public static String[] DISCORD_ROLES_FMT; // User set formats containing placeholders
        public static String DISCORD_MESSAGE_REPLY_FMT;
        public static String DISCORD_MC_MESSAGE_REPLY_FMT;
        public static String DISCORD_MESSAGE_FMT;
        public static String DISCORD_SINGLE_ROLE_FMT;
        public static boolean MINECRAFT_DEATH_EMBED;
        public static boolean MINECRAFT_JOIN_EMBED;
        public static boolean MINECRAFT_LEAVE_EMBED;
        public static boolean MINECRAFT_ADVANCEMENT_EMBED;
        public static boolean MINECRAFT_START_EMBED;
        public static boolean MINECRAFT_STOP_EMBED;
        public static Color MINECRAFT_DEATH_EMBED_COLOR;
        public static Color MINECRAFT_JOIN_EMBED_COLOR;
        public static Color MINECRAFT_LEAVE_EMBED_COLOR;
        public static Color MINECRAFT_ADVANCEMENT_EMBED_COLOR;
        public static Color MINECRAFT_START_EMBED_COLOR;
        public static Color MINECRAFT_STOP_EMBED_COLOR;
        public static String MINECRAFT_START_MESSAGE;
        public static String MINECRAFT_STOP_MESSAGE;
        private static void updateValues(){
            FileConfiguration config = getPlugin(Discord.class).getConfig();
            DISCORD_ROLE = config.getString("minecraft.discord-primary-role-placeholder");
            DISCORD_ROLE_COLOR = config.getString("minecraft.discord-role-color-placeholder");
            DISCORD_USERNAME = config.getString("minecraft.discord-username-placeholder");
            DISCORD_MESSAGE_LINK = config.getString("minecraft.discord-message-link-placeholder");
            DISCORD_USER_ID = config.getString("minecraft.discord-userid-placeholder");
            DISCORD_USER_COLOR = config.getString("minecraft.discord-usercolor-placeholder");
            DISCORD_NAME = config.getString("minecraft.discord-name-placeholder");
            DISCORD_ROLES = config.getString("minecraft.discord-roles-placeholder");
            DISCORD_MESSAGE = config.getString("minecraft.discord-message-placeholder");
            DISCORD_ROLES_FMT = new String[]{
                    config.getString("minecraft.discord-roles-format-first"),
                    config.getString("minecraft.discord-roles-format"),
                    config.getString("minecraft.discord-roles-format-last"),
            };
            DISCORD_MESSAGE_REPLY_FMT = config.getString("minecraft.discord-reply-format");
            DISCORD_MC_MESSAGE_REPLY_FMT = config.getString("minecraft.discord-reply-minecraft-format");
            DISCORD_MESSAGE_FMT = config.getString("minecraft.discord-message-format");
            DISCORD_SINGLE_ROLE_FMT = config.getString("minecraft.discord-roles-single-format").equalsIgnoreCase("first")
                    ? DISCORD_ROLES_FMT[0] : DISCORD_ROLES_FMT[2];
            MINECRAFT_DEATH_EMBED = config.getBoolean("discord.death-embed");
            MINECRAFT_JOIN_EMBED = config.getBoolean("discord.join-embed");
            MINECRAFT_LEAVE_EMBED = config.getBoolean("discord.leave-embed");
            MINECRAFT_STOP_EMBED = config.getBoolean("discord.stop-embed");
            MINECRAFT_START_EMBED = config.getBoolean("discord.start-embed");
            MINECRAFT_ADVANCEMENT_EMBED = config.getBoolean("discord.advancement-embed");
            MINECRAFT_DEATH_EMBED_COLOR = Color.decode(config.getString("discord.death-embed-color"));
            MINECRAFT_JOIN_EMBED_COLOR = Color.decode(config.getString("discord.join-embed-color"));
            MINECRAFT_START_EMBED_COLOR = Color.decode(config.getString("discord.start-embed-color"));
            MINECRAFT_STOP_EMBED_COLOR = Color.decode(config.getString("discord.stop-embed-color"));
            MINECRAFT_LEAVE_EMBED_COLOR = Color.decode(config.getString("discord.leave-embed-color"));
            MINECRAFT_ADVANCEMENT_EMBED_COLOR = Color.decode(config.getString("discord.advancement-embed-color"));
            MINECRAFT_START_MESSAGE = config.getString("discord.server-start-message");
            MINECRAFT_STOP_MESSAGE = config.getString("discord.server-stop-message");
        }
    }
    // https://discord.com/oauth2/authorize?client_id={client_id}&scope=bot+messages.read&permissions=275414780928
    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getDataFolder().mkdirs();
        saveDefaultConfig();
        Config.updateValues();
        String token = getToken(); // from file
        discordUtils = new DiscordUtils();
        MinecraftUtils minecraftUtils = new MinecraftUtils();
        discordUtils.setMinecraft(minecraftUtils);
        minecraftUtils.setDiscord(discordUtils);
        JDA jda = JDABuilder.createLight(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .addEventListeners(discordUtils)
                .build();
        try {
            jda = jda.awaitReady();
        } catch (InterruptedException e) {
            getComponentLogger().warn("Failed to wait for discord client ready.");
        }
        jda.updateCommands().addCommands(
                Commands.slash("listen", "toggle MC listen & output.")
                        .setGuildOnly(true)
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        ).queue(); // only release thread after jda is done loading
        discordUtils.setJDA(jda);
        getServer().getPluginManager().registerEvents(minecraftUtils, this);
        discordUtils.sendToDiscord(EventType.START, new TextComponent[]{Component.text(Config.MINECRAFT_START_MESSAGE)});
    }
    private String getToken() {
        String token = "token_here";
        File tokenFile = new File(this.getDataFolder(), "token.txt");
        if(!tokenFile.exists() || tokenFile.isDirectory()){
            try {
                if(tokenFile.createNewFile()){
                    FileWriter myWriter = new FileWriter(tokenFile);
                    myWriter.write(token);
                    myWriter.close();
                }
            } catch (IOException e) {
                getComponentLogger().error(Component.text(e.getMessage(), Style.style(TextColor.color(200, 50, 50))));
                throw new RuntimeException(e);
            }
            getComponentLogger().error(Component.text("PLEASE FILL OUT THE PLUGIN_FOLDER/TOKEN.TXT FILE WITH YOUR UNIQUE DISCORD BOT TOKEN.",
                    Style.style(TextColor.color(200, 50, 50), TextDecoration.BOLD)));
            throw new RuntimeException();
        }
        else {
            try {
                Scanner reader = new Scanner(tokenFile);
                if (reader.hasNext())
                    token = reader.nextLine();

            } catch (FileNotFoundException e) {
                getComponentLogger().error(Component.text(e.getMessage(), Style.style(TextColor.color(200, 50, 50))));
                throw new RuntimeException(e);
            }
        }
        if(token.equals("token_here")){
            getComponentLogger().error(Component.text("PLEASE CHANGE THE PLUGIN_FOLDER/TOKEN.TXT FILE TO YOUR UNIQUE DISCORD BOT TOKEN.",
                    Style.style(TextColor.color(200, 50, 50), TextDecoration.BOLD)));
            throw new RuntimeException();
        }
        return token;
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        discordUtils.sendToDiscord(EventType.STOP, new TextComponent[]{Component.text(Config.MINECRAFT_STOP_MESSAGE)});
    }
}
