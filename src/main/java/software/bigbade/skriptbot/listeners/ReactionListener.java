package software.bigbade.skriptbot.listeners;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import software.bigbade.skriptbot.api.ICommand;
import software.bigbade.skriptbot.utils.MessageUtils;
import software.bigbade.skriptbot.utils.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

@RequiredArgsConstructor
public class ReactionListener extends ListenerAdapter {
    private final List<ICommand> commands;
    private final String prefix;
    @Setter
    private JDA jda;

    //TODO refactor cognitive complexity
    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        MessageReaction.ReactionEmote reaction = event.getReaction().getReactionEmote();
        if (!reaction.isEmoji() || event.getUser() == null || event.getUser().isBot()) {
            return;
        }
        event.retrieveMessage().queue(message -> {
            if (!message.getAuthor().equals(jda.getSelfUser())) {
                return;
            }
            message.getChannel().getHistoryBefore(message, 1).queue(history -> {
                if (history.getRetrievedHistory().isEmpty() || !history.getRetrievedHistory().get(0).getAuthor().equals(event.getUser())) {
                    return;
                }
                if (reaction.getAsCodepoints().equals(MessageUtils.DELETE_REACTION)) {
                    message.delete().queue();
                    return;
                }
                Message lastMessage = history.getRetrievedHistory().get(0);
                String foundMessage = lastMessage.getContentStripped();
                if (!foundMessage.startsWith(prefix)) {
                    return;
                }

                String foundCommand = CommandListener.getCommandName(prefix, foundMessage);
                for (ICommand command : commands) {
                    if (StringUtils.matchesArray(foundCommand, command.getSymbol())) {
                        command.onReaction(lastMessage, message, reaction);
                    }
                }
            });
        });
    }
}