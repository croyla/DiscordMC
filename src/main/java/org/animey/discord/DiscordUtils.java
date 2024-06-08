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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.util.Scanner;

public class DiscordUtils extends ListenerAdapter { // Send message, retrieve message, etc discord related
    MinecraftUtils minecraftUtils;
    TextChannel discordChannel;
    Webhook webhook;
    final ComponentLogger logger = JavaPlugin.getPlugin(Discord.class).getComponentLogger();
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
                    Component[] senderRoles = {Component.text(
                            "Roles: ",
                            TextColor.color(255, 255, 255)
                    )};
                    Role lastRole = event.getMember().getRoles().get(event.getMember().getRoles().size() - 1);
                    event.getMember().getRoles().forEach(
                            role -> {
                                if (role != lastRole)
                                    senderRoles[0] = senderRoles[0].append( // Use an array cause of strict finality in .forEach
                                            Component.text(
                                                    "█",
                                                    TextColor.color(role.getColorRaw())
                                            )
                                    ).append(
                                            Component.text(role.getName() + ", ", TextColor.color(role.getColorRaw()))
                                    ).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE);
                            }
                    );
                    Component senderHover = Component.text(
                                    event.getMessage().getMember().getEffectiveName(),
                                    TextColor.color(event.getMember().getColorRaw()),
                                    TextDecoration.BOLD
                            )
                            .appendNewline()
                            .append(
                                    Component.text(
                                            "Username: "+ event.getAuthor().getName(),
                                            TextColor.color(255, 255, 255)
                                    ).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                            )
                            .appendNewline()
                            .append(senderRoles[0])
                            .append( // Use an array cause of strict finality in .forEach
                                    Component.text(
                                            "█",
                                            TextColor.color(lastRole.getColorRaw())
                                    )
                            )
                            .append(
                                    Component.text(
                                            lastRole.getName(),
                                            TextColor.color(lastRole.getColorRaw())
                                    )
                            ).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                            .appendNewline()
                            .append(
                                    Component.text(
                                            "Message on discord",
                                            TextColor.color(255, 255, 255),
                                            TextDecoration.ITALIC
                                    ).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                            );
                    Component message = Component.text(
                                    "["+ event.getMember().getRoles().get(0).getName() + "] ",
                                    TextColor.color(event.getMember().getColorRaw())
                            )
                            .append(
                                    Component.text(
                                            event.getMember().getEffectiveName() +": ",
                                            TextColor.color(255, 255, 255)
                                    )
                            )
                            .append(
                                    Component.text(
                                            event.getMessage().getContentStripped(),
                                            TextColor.color(255, 255, 255)
                                    ).hoverEvent(
                                    HoverEvent.showText(
                                            Component.text("Go to message")
                                    )
                                    ).clickEvent(
                                            ClickEvent.openUrl(
                                                    event.getMessage().getJumpUrl()
                                            )
                                    )
                            )
                            .hoverEvent(HoverEvent.showText(senderHover))
                            .clickEvent(ClickEvent.openUrl("https://discord.com/channels/@me/"+ event.getMember().getId() +"/"));
                    if(event.getMessage().getType().equals(MessageType.INLINE_REPLY)) {
                        Component name;
                        if(event.getMessage().getReferencedMessage().isWebhookMessage() && event.getMessage().getReferencedMessage().getApplicationIdLong() == info.getIdLong()) // faulty, assumes all webhook messages are from our webhook
                            name = Component.text("[MC] "+ event.getMessage().getReferencedMessage().getAuthor().getName());
                        else
                            name = Component.text(event.getMessage().getReferencedMessage().getAuthor().getEffectiveName());
                        minecraftUtils.sendToMinecraft(Component.text("Replying to ➡ ",
                                        TextColor.color(100, 100, 100), TextDecoration.ITALIC)
                                .append(name)
                                .append(
                                        Component.text(
                                                ": " + event.getMessage().getReferencedMessage().getContentStripped(),
                                                TextColor.color(180, 180, 180)
                                        )
                                ).hoverEvent(
                                        HoverEvent.showText(
                                                Component.text("Go to message")
                                        )
                                ).clickEvent(
                                        ClickEvent.openUrl(
                                                event.getMessage().getReferencedMessage().getJumpUrl()
                                        )
                                )
                        );
                    }
                    minecraftUtils.sendToMinecraft(message);
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
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content() + " died",
                        null, "https://mc-heads.net/avatar/"+ minecraftUtils.getPlayerId(content[0].content()));
                messageBuilder.setFooter(content[1].content());
                messageBuilder.setColor(Color.RED);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
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
                    break;
                }
            case ADVANCEMENT:
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content() + " made an advancement",
                        null, "https://mc-heads.net/avatar/"+ minecraftUtils.getPlayerId(content[0].content()));
                messageBuilder.setFooter(content[1].content());
                messageBuilder.setColor(Color.GRAY);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                break;
            case JOIN:
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content() + " has connected",
                        null, "https://mc-heads.net/avatar/"+ minecraftUtils.getPlayerId(content[0].content()));
                messageBuilder.setColor(Color.GREEN);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                break;
            case LEAVE:
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content() + " has disconnected",
                        null, "https://mc-heads.net/avatar/"+ minecraftUtils.getPlayerId(content[0].content()));
                messageBuilder.setColor(Color.RED);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                break;
            case STOP:
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content());
                messageBuilder.setColor(Color.RED);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
                break;
            case START:
                messageBuilder = new EmbedBuilder();
                messageBuilder.setAuthor(content[0].content());
                messageBuilder.setColor(Color.GREEN);
                discordChannel.sendMessageEmbeds(messageBuilder.build()).queue();
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
        builder.setWait(true);
        return builder.build();
    }

    private void setDiscordChannel(TextChannel channel){
        File channelFile = new File("discord/channel");
        File webhookFile = new File("discord/webhook");
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
        new File("discord").mkdirs();
        String webhookId = "channel_id";
        File tokenFile = new File("discord/channel");
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
        new File("discord").mkdirs();
        String webhookId = "webhook_id";
        File tokenFile = new File("discord/webhook");
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
}
