package ca.concordia.encs.conquerdia.controller.command;

import ca.concordia.encs.conquerdia.model.map.WorldMap;

import java.util.List;

/**
 *
 */
public class ValidateMapCommand extends AbstractCommand {

    private static final String COMMAND_HELP_MSG = "";

    @Override
    protected CommandType getCommandType() {
        return CommandType.VALIDATE_MAP;
    }

    @Override
    protected String getCommandHelpMessage() {
        return COMMAND_HELP_MSG;
    }

    /**
     * @param inputCommandParts the command line parameters.
     * @return List of Command Results
     */
    @Override
    public void runCommand(List<String> inputCommandParts) {
        resultList.add(WorldMap.getInstance().validateMap());
    }

}