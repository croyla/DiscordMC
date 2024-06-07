package org.animey.discord;

import com.google.gson.JsonParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.InputStreamReader;
import java.net.URL;

public class MinecraftUtils implements Listener { // Methods like sendDiscordMessage reside here
    DiscordUtils discordUtils;
    public void setDiscord(DiscordUtils discordUtils){
        this.discordUtils = discordUtils;
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncChatEvent e){
        if(e.isCancelled())
            return;
        TextComponent[] component = {Component.text(e.getPlayer().getName()), (TextComponent) e.message()};
        discordUtils.sendToDiscord(Discord.EventType.MESSAGE, component);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e){
        TextComponent[] component = {Component.text(e.getPlayer().getName())};
        discordUtils.sendToDiscord(Discord.EventType.JOIN, component);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent e){
        String tl  = PlainTextComponentSerializer.plainText().serializeOrNull(e.message());
        assert tl != null;
        TextComponent[] component = {Component.text(e.getPlayer().getName()), Component.text(tl)};
        discordUtils.sendToDiscord(Discord.EventType.ADVANCEMENT, component);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e){
        if(e.isCancelled())
            return;
        String tl = PlainTextComponentSerializer.plainText().serializeOrNull(e.deathMessage());
        assert tl != null;
        TextComponent[] component = {Component.text(e.getPlayer().getName()), Component.text(tl)};
        discordUtils.sendToDiscord(Discord.EventType.DEATH, component);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent e){
        TextComponent[] component = {Component.text(e.getPlayer().getName())};
        discordUtils.sendToDiscord(Discord.EventType.LEAVE, component);
    }

    public void sendToMinecraft(Component message){ // Component for hovers
        Bukkit.broadcast(message);
    }

    public String getPlayerId(String string) {
        try {
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + string);
            InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
            return JsonParser.parseReader(reader_0).getAsJsonObject().get("id").getAsString();
        } catch (Exception e){
            return "null";
        }
    }
}
