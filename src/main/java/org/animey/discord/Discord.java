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
import org.bukkit.plugin.java.JavaPlugin;

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

    // https://discord.com/oauth2/authorize?client_id={client_id}&scope=bot+messages.read&permissions=275414780928
    @Override
    public void onEnable() {
        // Plugin startup logic
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
        discordUtils.sendToDiscord(EventType.START, new TextComponent[]{Component.text("Server is up")});
    }
    private String getToken() {
        new File("discord").mkdirs();
        String token = "token_here";
        File tokenFile = new File("discord/token.txt");
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
            getComponentLogger().error(Component.text("PLEASE FILL OUT THE SERVERDIR/DISCORD/TOKEN.TXT FILE WITH YOUR UNIQUE DISCORD BOT TOKEN.",
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
            getComponentLogger().error(Component.text("PLEASE CHANGE THE SERVERDIR/DISCORD/TOKEN.TXT FILE TO YOUR UNIQUE DISCORD BOT TOKEN.",
                    Style.style(TextColor.color(200, 50, 50), TextDecoration.BOLD)));
            throw new RuntimeException();
        }
        return token;
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        discordUtils.sendToDiscord(EventType.STOP, new TextComponent[]{Component.text("Server is down")});
    }
}
