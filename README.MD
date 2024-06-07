### Discord Minecraft integration (Paper Plugin)
#### Using Discord-JDA
Installing:
1. Please create a discord bot or obtain a bot-token for this instance to run on.
2. Add the bot to your server using the link ``https://discord.com/oauth2/authorize?client_id={client_id}&scope=bot+messages.read&permissions=275414780928``, replace `{client_id}` with your client / application id available in the discord developer dashboard
3. Proceed to populate "$SERVERDIR/discord/token.txt" with your bot-token
4. Run the minecraft server
5. Now go to your preferred discord channel and run `/listen`, please note the bot must be able to read and send messages in this channel, as well as create webhooks.
   ![image](https://github.com/croyla/DiscordMC/assets/22202283/110a4219-5f5d-4079-8f7d-2d5360870afa)
   ![image](https://github.com/croyla/DiscordMC/assets/22202283/d43bc49b-d049-4473-ac06-8c5e976c319e)
   ![image](https://github.com/croyla/DiscordMC/assets/22202283/6882c5e0-9906-4ca7-b3fe-41410c859399)

### Planned additions:
- Hover over discord usernames in minecraft chat for discord overview (Roles, username, etc)
- Discord - Minecraft account linking for similar hover feature in minecraft chat. (Optional "PasswordReset" functionality with my Auth plugin)
- Message formatting configuration for discord and minecraft messages
- ClickEvents for minecraft messages for links, replies, discord profiles to open discord relevant links.