### Discord Minecraft integration (Paper Plugin)
#### Using Discord-JDA
Installing:
1. Please create a discord bot or obtain a bot-token for this instance to run on.
2. Add the bot to your server using the link ``https://discord.com/oauth2/authorize?client_id={client_id}&scope=bot+messages.read&permissions=275414780928``, replace `{client_id}` with your client / application id available in the discord developer dashboard
3. Proceed to populate "$SERVERDIR/discord/token.txt" with your bot-token
4. Run the minecraft server
5. Now go to your preferred discord channel and run `/listen`, please note the bot must be able to read and send messages in this channel, as well as create webhooks.