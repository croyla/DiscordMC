package org.animey.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Scanner;

public class DiscordUtils extends ListenerAdapter { // Send message, retrieve message, etc discord related
    MinecraftUtils minecraftUtils;
    TextChannel discordChannel;
    Webhook webhook;
    JavaPlugin plugin = JavaPlugin.getPlugin(Discord.class);
    final ComponentLogger logger = plugin.getComponentLogger();
    JDA jda;
    ApplicationInfo info;
    public void setMinecraft(MinecraftUtils minecraftUtils){
        this.minecraftUtils = minecraftUtils;
    }
    public void setJDA(JDA jda){
        this.jda = jda;
        discordChannel = getDiscordChannel();
        if(webhook == null && discordChannel != null) webhook = getWebhook();
        if(discordChannel != null && webhook != null) {
            logger.debug(Component.text("Webhook name " + webhook.getName()));
            logger.info(Component.text("Successfully connected to discord channel "+ discordChannel.getName()));
        }
        info = jda.retrieveApplicationInfo().complete();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getChannel().asTextChannel().equals(discordChannel)){
            // Send to minecraft server
            if(!event.getAuthor().isBot() && !event.getAuthor().isSystem() && !event.getMessage().isWebhookMessage()
            && !event.getMessage().isEphemeral())
                if(!event.getAuthor().getFlags().contains(User.UserFlag.BOT_HTTP_INTERACTIONS) ||
                        !event.getAuthor().getFlags().contains(User.UserFlag.VERIFIED_BOT)){
                    if(event.getMessage().getType().equals(MessageType.INLINE_REPLY)) {
                        String replyFmt;
                        if(event.getMessage().getReferencedMessage().isWebhookMessage() && event.getMessage().getReferencedMessage().getApplicationIdLong() == info.getIdLong())
                            replyFmt = Discord.Config.DISCORD_MC_MESSAGE_REPLY_FMT;
                        else
                            replyFmt = Discord.Config.DISCORD_MESSAGE_REPLY_FMT;
                        replyFmt = placeVars(event.getMessage().getReferencedMessage(), replyFmt);
                        minecraftUtils.sendToMinecraft(MiniMessage.miniMessage().deserialize(replyFmt));

                    }
                    minecraftUtils.sendToMinecraft(
                            MiniMessage.miniMessage().deserialize(placeVars(event.getMessage(), Discord.Config.DISCORD_MESSAGE_FMT)));
                }
        } else if(event.isFromType(ChannelType.PRIVATE)){
            // Attempt linking of discord account and minecraft account
            logger.warn(Component.text("Not yet impl."));
        }
    }

    public void sendToDiscord(Discord.EventType type, TextComponent[] content) {
        if (discordChannel == null) {
            discordChannel = getDiscordChannel();
            if (discordChannel == null) {
                logger.warn(Component.text("Please use the /listen command on your discord server text channel."));
                return;
            }
        }
        EmbedBuilder messageBuilder;
        switch (type) {
            case DEATH:
                if(Discord.Config.MINECRAFT_DEATH_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content() + " died",
                            null, "https://mc-heads.net/avatar/" + minecraftUtils.getPlayerId(content[0].content()));
                    messageBuilder.setFooter(content[1].content());
                    messageBuilder.setColor(Discord.Config.MINECRAFT_DEATH_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                } else discordChannel.sendMessage(content[1].content()).queue();
                break;
            case MESSAGE:
                if(webhook == null)
                    webhook = getWebhook();
                if (webhook != null) {
                    // Change appearance of webhook message
                    WebhookMessage message = new WebhookMessageBuilder()
                            .setUsername(content[0].content()) // use this username
                            .setAvatarUrl("https://mc-heads.net/avatar/"+ minecraftUtils.getPlayerId(content[0].content())) // use this avatar
                            .setContent(content[1].content())
                            .build();
                    WebhookClient webhookClient = getWebhookClient();
                    webhookClient.send(message);
                    webhookClient.close();
                }
                break;
            case ADVANCEMENT:
                if(Discord.Config.MINECRAFT_ADVANCEMENT_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content() + " made an advancement",
                            null, "https://mc-heads.net/avatar/" + minecraftUtils.getPlayerId(content[0].content()));
                    messageBuilder.setFooter(content[1].content());
                    messageBuilder.setColor(Discord.Config.MINECRAFT_ADVANCEMENT_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                } else discordChannel.sendMessage(content[1].content()).queue();
                break;
            case JOIN:
                if(Discord.Config.MINECRAFT_JOIN_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content() + " has connected",
                            null, "https://mc-heads.net/avatar/" + minecraftUtils.getPlayerId(content[0].content()));
                    messageBuilder.setColor(Discord.Config.MINECRAFT_JOIN_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                } else discordChannel.sendMessage(content[0].content() + " has connected").queue();
                break;
            case LEAVE:
                if(Discord.Config.MINECRAFT_LEAVE_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content() + " has disconnected",
                            null, "https://mc-heads.net/avatar/" + minecraftUtils.getPlayerId(content[0].content()));
                    messageBuilder.setColor(Discord.Config.MINECRAFT_LEAVE_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                } else discordChannel.sendMessage(content[0].content() + " has disconnected").queue();
                break;
            case STOP:
                if(Discord.Config.MINECRAFT_STOP_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content());
                    messageBuilder.setColor(Discord.Config.MINECRAFT_STOP_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).complete(); // Complete as we want to halt the shutdown of server till our message is sent
                } else discordChannel.sendMessage(content[0].content()).complete();
                break;
            case START:
                if(Discord.Config.MINECRAFT_START_EMBED) {
                    messageBuilder = new EmbedBuilder();
                    messageBuilder.setAuthor(content[0].content());
                    messageBuilder.setColor(Discord.Config.MINECRAFT_START_EMBED_COLOR);
                    discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                } else discordChannel.sendMessage(content[0].content()).queue();
                break;
            default:
                discordChannel.sendMessage(content[0].content()).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("listen")) {
            if (event.getChannel().equals(discordChannel)) {
                event.reply("Not listening anymore.").queue();
                setDiscordChannel(null);
            } // unset this
            else {
                setDiscordChannel(null);
                setDiscordChannel(event.getChannel().asTextChannel());
                event.reply("Listening here now.").queue();
            } // overwrite previous listening channel
        }
    }

    private WebhookClient getWebhookClient() {
        if (webhook == null)
            return null;
        // Using the builder
        WebhookClientBuilder builder = new WebhookClientBuilder(webhook.getUrl());
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("Hello");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(false); // we dont want to wait for messages to send every time, as this adds unnecessary delay in minecraft.
        return builder.build();
    }

    private void setDiscordChannel(TextChannel channel){
        File channelFile = new File(plugin.getDataFolder(),"channel");
        File webhookFile = new File(plugin.getDataFolder(), "webhook");
        if (channel == null) {
            webhook = null;
            discordChannel = null;
            if(channelFile.exists())
                channelFile.delete();
            if(webhookFile.exists()) {
                getWebhook().delete().queue(); // erase the webhook from discord server.
                webhookFile.delete();
            }
        } else {
            discordChannel = channel;
            webhook = discordChannel.createWebhook("Minecraft-Chat").complete();
            try {
                if(!webhookFile.exists() || webhookFile.isDirectory()) webhookFile.createNewFile();
                if(!channelFile.exists() || channelFile.isDirectory()) channelFile.createNewFile();
                FileWriter myWriter = new FileWriter(channelFile);
                myWriter.write(channel.getId());
                myWriter.close();
                myWriter = new FileWriter(webhookFile);
                myWriter.write(webhook.getId());
                myWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private TextChannel getDiscordChannel(){
        String webhookId = "channel_id";
        File tokenFile = new File(plugin.getDataFolder(),"channel");
        if(!tokenFile.exists())
            return null;
        try {
            Scanner reader = new Scanner(tokenFile);
            if (reader.hasNext())
                webhookId = reader.nextLine(); else
                logger.error(Component.text("channel reading error", Style.style(TextColor.color(200, 50, 50))));

        } catch (FileNotFoundException e) {
            logger.error(Component.text(e.getMessage(), Style.style(TextColor.color(200, 50, 50))));
            throw new RuntimeException(e);
        }
        return jda.getTextChannelById(Long.parseLong(webhookId));
    }

    private Webhook getWebhook(){
        String webhookId = "webhook_id";
        File tokenFile = new File(plugin.getDataFolder(), "webhook");
        try {
            Scanner reader = new Scanner(tokenFile);
            if (reader.hasNext())
                webhookId = reader.nextLine(); else
                logger.error(Component.text("webhook reading error", Style.style(TextColor.color(200, 50, 50))));

        } catch (FileNotFoundException e) {
            logger.error(Component.text(e.getMessage(), Style.style(TextColor.color(200, 50, 50))));
            throw new RuntimeException(e);
        }
        return jda.retrieveWebhookById(Long.parseLong(webhookId)).complete();
    }
    private String placeVars(Message event, String fmt) {

        String ret = fmt
                .replace(Discord.Config.DISCORD_USER_COLOR, event.isWebhookMessage() ? "" : "#"+ String.format("%06X",event.getMember().getColor().getRGB()).substring(2))
                .replace(Discord.Config.DISCORD_ROLE_COLOR, event.isWebhookMessage() ? "" : "#"+ String.format("%06X",event.getMember().getColor().getRGB()).substring(2))
                .replace(Discord.Config.DISCORD_MESSAGE_LINK, event.getJumpUrl())
                .replace(Discord.Config.DISCORD_USER_ID, event.isWebhookMessage() ? "" : event.getMember().getId())
                .replace(Discord.Config.DISCORD_ROLES, event.isWebhookMessage() ? "" : formattedRoles(event.getMember().getRoles()));
        String[] splitMessage = ret.split(Discord.Config.DISCORD_MESSAGE);
        StringBuilder joined = new StringBuilder();
        for (String s : splitMessage) { // prevent variables in message messing with USERNAME and NAME
            s = s.replace(Discord.Config.DISCORD_USERNAME, event.getAuthor().getName());
            if(!event.isWebhookMessage() && !event.getMember().getRoles().isEmpty())
                s = s.replace(Discord.Config.DISCORD_ROLE, event.getMember().getRoles().get(0).getName());
            s = s.replace(Discord.Config.DISCORD_NAME, event.getAuthor().getEffectiveName());
            if(joined.length() == 0)
                joined.append(s);
            else joined.append(event.getContentStripped()).append(s);
        }
        return joined.toString();
    }
    private String formattedRoles(List<Role> roles) {
        if(roles.isEmpty())
            return "";
        if(roles.size() == 1)
            return Discord.Config.DISCORD_SINGLE_ROLE_FMT
                    .replace(Discord.Config.DISCORD_ROLE, roles.get(0).getName())
                    .replace(Discord.Config.DISCORD_ROLE_COLOR, "#"+ String.format("%06X", roles.get(0).getColor() == null ? Color.GRAY.getRGB() : roles.get(0).getColor().getRGB()).substring(2));
        String first = Discord.Config.DISCORD_ROLES_FMT[0]
                .replace(Discord.Config.DISCORD_ROLE, roles.get(0).getName())
                .replace(Discord.Config.DISCORD_ROLE_COLOR, "#"+ String.format("%06X", roles.get(0).getColor() == null ? Color.GRAY.getRGB() : roles.get(0).getColor().getRGB()).substring(2));
        String last = Discord.Config.DISCORD_ROLES_FMT[2]
                .replace(Discord.Config.DISCORD_ROLE, roles.get(roles.size() - 1).getName())
                .replace(Discord.Config.DISCORD_ROLE_COLOR, "#"+ String.format("%06X", roles.get(roles.size() - 1).getColor() == null ? Color.GRAY.getRGB() : roles.get(roles.size() - 1).getColor().getRGB()).substring(2));
        if(roles.size() == 2)
            return first + last;

        String[] ret = {first};
        roles.forEach(role -> {
                    if(role != roles.get(0) && role != roles.get(roles.size() - 1))
                        ret[0] = ret[0] + Discord.Config.DISCORD_ROLES_FMT[1]
                                .replace(Discord.Config.DISCORD_ROLE, role.getName())
                                .replace(Discord.Config.DISCORD_ROLE_COLOR, "#" + String.format("%06X", role.getColor() == null ? Color.GRAY.getRGB() : role.getColor().getRGB()).substring(2));
                }
        );
        return ret[0] + last;
    }
}



