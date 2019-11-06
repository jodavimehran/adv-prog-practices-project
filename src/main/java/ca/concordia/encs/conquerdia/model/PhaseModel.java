package ca.concordia.encs.conquerdia.model;

import ca.concordia.encs.conquerdia.controller.command.CommandType;
import ca.concordia.encs.conquerdia.exception.ValidationException;
import ca.concordia.encs.conquerdia.model.map.Country;
import ca.concordia.encs.conquerdia.model.map.WorldMap;
import ca.concordia.encs.conquerdia.util.Observable;

import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represent the phases of the game
 */
public class PhaseModel extends Observable {
    private static PhaseModel instance;
    private final List<String> phaseLog = new ArrayList<>();
    private PhaseTypes currentPhase = PhaseTypes.NONE;
    private int numberOfInitialArmies = -1;
    private boolean allCountriesArePopulated;

    /**
     * private Constructor to implementation of the Singleton Pattern
     */
    private PhaseModel() {
    }

    /**
     * This method is used for getting a single instance of the {@link PhaseModel}
     *
     * @return single instance of the {@link PhaseModel phase}
     */
    public static PhaseModel getInstance() {
        if (instance == null) {
            synchronized (PhaseModel.class) {
                if (instance == null) {
                    instance = new PhaseModel();
                }
            }
        }
        return instance;
    }

    /**
     * @return true if all countries are populated
     */
    public boolean isAllCountriesArePopulated() {
        return allCountriesArePopulated;
    }

    /**
     * @return number of initial armies
     */
    public int getNumberOfInitialArmies() {
        return numberOfInitialArmies;
    }


    /**
     * @return current player
     */
    public Player getCurrentPlayer() {
        return PlayersModel.getInstance().getCurrentPlayer();
    }

    /**
     * @return result
     */
    public List<String> changePhase() {
        List<String> results = new ArrayList<>();
        switch (currentPhase) {
            case NONE: {
                if (WorldMap.getInstance().isMapLoaded()) {
                    changePhase(PhaseTypes.START_UP);
                } else if (WorldMap.getInstance().isReadyForEdit()) {
                    changePhase(PhaseTypes.EDIT_MAP);
                }
                break;
            }
            case EDIT_MAP: {
                if (WorldMap.getInstance().isMapLoaded()) {
                    changePhase(PhaseTypes.START_UP);
                }
                break;
            }
            case START_UP: {
                if (allCountriesArePopulated) {
                    if (PlayersModel.getInstance().isThereAnyUnplacedArmy()) {
                        results.add(String.format("Dear %s, you have %d unplaced armies.", getCurrentPlayer().getName(), getCurrentPlayer().getUnplacedArmies()));
                        results.add("Use \"placearmy\" to place one of them or use \"placeall\" to automatically randomly place all remaining unplaced armies for all players.");
                    } else {
                        PlayersModel.getInstance().giveTurnToFirstPlayer();
                        changePhase(PhaseTypes.REINFORCEMENT);
                        CardExchangeModel.getInstance().setReinforcementPhaseActive(true);
                        results.add("================================================================================================================================================");
                        results.add("All players have placed their armies.");
                        results.add("Startup phase is finished.");
                        results.add("The turn-based main phase of the game is about to begin.");
                        results.add("================================================================================================================================================");

                        getCurrentPlayer().calculateNumberOfReinforcementArmies();
                        results.add(String.format("Dear %s, Congratulations! You've got %d armies at this phase! You can place them wherever in your territory.", getCurrentPlayer().getName(), getCurrentPlayer().getUnplacedArmies()));
                    }
                }
                break;
            }
            case REINFORCEMENT: {
                if (getCurrentPlayer().getUnplacedArmies() > 0) {
                    results.add(String.format("You have %d reinforcement army. You can place them wherever in your territory.", getCurrentPlayer().getUnplacedArmies()));
                } else if (getCurrentPlayer().getCards().size() >= 5) {
                    results.add("You have more than five cards. You must exchange them by using \"exchangecards\" command.");
                } else {
                    changePhase(PhaseTypes.ATTACK);
                    CardExchangeModel.getInstance().setReinforcementPhaseActive(false);
                }
                break;
            }
            case ATTACK: {
    			getCurrentPlayer().setHasSuccessfulAttack(false);
    			if (getCurrentPlayer().isAttackFinished()) {
    				getCurrentPlayer().winCard();
                    changePhase(PhaseTypes.FORTIFICATION);
                }
                
                break;
            }
            case FORTIFICATION: {
                if (getCurrentPlayer().isFortificationFinished()) {
                    PlayersModel.getInstance().giveTurnToAnotherPlayer();
                    changePhase(PhaseTypes.REINFORCEMENT);
                    getCurrentPlayer().calculateNumberOfReinforcementArmies();
                    results.add(String.format("Dear %s, Congratulations! You've got %d armies at this phase! You can place them wherever in your territory.", getCurrentPlayer().getUnplacedArmies()));
                }
                break;
            }
        }
        return results;
    }

    /**
     * @param phaseTypes
     */
    private void changePhase(PhaseTypes phaseTypes) {
        phaseLog.clear();
        currentPhase = phaseTypes;
        phaseLog.add(LocalTime.now() + " - " + phaseTypes.getName() + " phase has start");
        setChanged();
        notifyObservers(this);
    }


    /**
     * @param commandType
     * @return
     */
    public boolean isValidCommand(CommandType commandType) {
        return currentPhase.validCommands.contains(commandType);
    }

    /**
     * @return
     */
    public String getValidCommands() {
        return currentPhase.validCommands.stream().map(CommandType::getName).collect(Collectors.joining(", "));
    }

    /**
     * @return
     */
    public String getPhaseStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Phase: ").append(currentPhase.getName());
        if (getCurrentPlayer() != null && (currentPhase.equals(PhaseTypes.REINFORCEMENT) || currentPhase.equals(PhaseTypes.FORTIFICATION) || currentPhase.equals(PhaseTypes.ATTACK)))
            sb.append(",Player: ").append(getCurrentPlayer().getName());
        if (!phaseLog.isEmpty()) {
            for (String log : phaseLog) {
                sb.append(System.getProperty("line.separator")).append(log);
            }
        }
        return sb.toString();
    }

    /**
     * @param logs
     */
    public void addPhaseLogs(List<String> logs) {
        if (logs != null && !logs.isEmpty()) {
            LocalTime now = LocalTime.now();
            logs.stream().forEach(log -> phaseLog.add(now + " - " + log));
            setChanged();
            notifyObservers(this);
        }
    }

    /**
     * This method randomly assign a country to a player
     */
    public void populateCountries() throws ValidationException {
        if (allCountriesArePopulated) {
            throw new ValidationException("All countries are populated before!");
        }
        int numberOfPlayers = PlayersModel.getInstance().getNumberOfPlayers();
        if (numberOfPlayers < 3) {
            throw new ValidationException("The game need at least Three players to start.");
        }
        Set<Country> countries = WorldMap.getInstance().getCountries();
        int numberOfCountries = countries.size();
        if (numberOfPlayers > numberOfCountries)
            throw new ValidationException("Too Many Players! Number of player must be equal or lower than number of countries in map!");
        SecureRandom randomNumber = new SecureRandom();
        int randomInt = randomNumber.nextInt(numberOfPlayers - 1);
        for (int i = 0; i < randomInt; i++) {
            PlayersModel.getInstance().giveTurnToAnotherPlayer();
        }
        Player firstPlayer = getCurrentPlayer();
        PlayersModel.getInstance().setFirstPlayer(firstPlayer);
        while (!countries.isEmpty()) {
            Country[] countryArray = new Country[countries.size()];
            countryArray = countries.toArray(countryArray);
            int value = randomNumber.nextInt(countries.size());
            Country country = countryArray[value];

            country.setOwner(getCurrentPlayer());
            country.placeOneArmy();
            getCurrentPlayer().addCountry(country);
            if (getCurrentPlayer().ownedAll(country.getContinent().getCountriesName()))
                getCurrentPlayer().addContinent(country.getContinent());
            countries.remove(country);
            PlayersModel.getInstance().giveTurnToAnotherPlayer();
        }
        PlayersModel.getInstance().giveTurnToFirstPlayer();
        numberOfInitialArmies = calculateNumberOfInitialArmies(numberOfPlayers);
        PlayersModel.getInstance().getPlayers().forEach(player -> player.addUnplacedArmies(numberOfInitialArmies - player.getNumberOfCountries()));
        allCountriesArePopulated = true;
    }

    /**
     * Calculate number of initial armies depending on the number of players.
     *
     * @param numberOfPlayers the number of players
     * @return number of initial armies
     */
    private int calculateNumberOfInitialArmies(int numberOfPlayers) {
        switch (numberOfPlayers) {
            case 3:
                return 35;
            case 4:
                return 30;
            case 5:
                return 25;
            default:
                return 20;
        }
    }


    /**
     * Phase Types
     */
    public enum PhaseTypes {
        NONE("None", new HashSet<>(Arrays.asList(CommandType.LOAD_MAP, CommandType.EDIT_MAP))),
        EDIT_MAP("Edit Map", new HashSet<>(Arrays.asList(CommandType.LOAD_MAP, CommandType.EDIT_CONTINENT, CommandType.EDIT_COUNTRY, CommandType.EDIT_NEIGHBOR, CommandType.SHOW_MAP, CommandType.SAVE_MAP, CommandType.VALIDATE_MAP))),
        START_UP("Startup", new HashSet<>(Arrays.asList(CommandType.SHOW_MAP, CommandType.GAME_PLAYER, CommandType.POPULATE_COUNTRIES, CommandType.PLACE_ARMY, CommandType.PLACE_ALL))),
        REINFORCEMENT("Reinforcement", new HashSet<>(Arrays.asList(CommandType.SHOW_MAP, CommandType.REINFORCE))),
        ATTACK("Attack", new HashSet<>(Arrays.asList(CommandType.SHOW_MAP, CommandType.ATTACK, CommandType.DEFEND, CommandType.ATTACK_MOVE))),
        FORTIFICATION("Fortification", new HashSet<>(Arrays.asList(CommandType.SHOW_MAP)));

        private final String name;
        private final Set<CommandType> validCommands;

        /**
         * @param validCommands The legal command that can be executed during this Phase of game
         */
        PhaseTypes(String name, Set<CommandType> validCommands) {
            this.name = name;
            this.validCommands = validCommands;
        }

        /**
         * @return name of the phase
         */
        public String getName() {
            return name;
        }

    }
}
