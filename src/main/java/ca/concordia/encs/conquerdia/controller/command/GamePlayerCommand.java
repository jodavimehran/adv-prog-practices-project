package ca.concordia.encs.conquerdia.controller.command;

import ca.concordia.encs.conquerdia.exception.ValidationException;
import ca.concordia.encs.conquerdia.model.PlayersModel;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the <i>gameplayer</i> command
 */
public class GamePlayerCommand extends AbstractCommand {
    public final static String COMMAND_HELP_MSG = "The \"gameplayer\" command must has at least one option like \"-add\" or \"-remove\".";

    /**
     * {@inheritDoc}
     */
    @Override
    protected CommandType getCommandType() {
        return CommandType.GAME_PLAYER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCommandHelpMessage() {
        return COMMAND_HELP_MSG;
    }

    /**
     * @param inputCommandParts the command line parameters.
     */
    @Override
    public void runCommand(List<String> inputCommandParts) throws ValidationException {
        Iterator<String> iterator = inputCommandParts.iterator();
        iterator.next();
        while (iterator.hasNext()) {
            String option = iterator.next();
            String playerName = iterator.next();
            switch (option) {
                case ("-add"): {
                    try {
                        PlayersModel.getInstance().addPlayer(playerName);
                        phaseLogList.add(String.format("Player with name \"%s\" was added.", playerName));
                    } catch (ValidationException ex) {
                        resultList.addAll(ex.getValidationErrors());
                    }
                    break;
                }
                case "-remove": {
                    try {
                        PlayersModel.getInstance().removePlayer(playerName);
                        phaseLogList.add(String.format("Player with name \"%s\" was removed.", playerName));
                    } catch (ValidationException ex) {
                        resultList.addAll(ex.getValidationErrors());
                    }
                    break;
                }
                default: {
                    throw new ValidationException(COMMAND_HELP_MSG);
                }
            }
        }
    }
}
