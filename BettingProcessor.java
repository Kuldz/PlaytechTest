import java.io.*;
import java.util.*;

class Player implements Comparable<Player> {
    private String id;
    private long balance;
    private int betsPlaced;
    private int betsWon;
    private List<String> illegalOperations;

    public Player(String id) {
        this.id = id;
        this.balance = 0;
        this.betsPlaced = 0;
        this.betsWon = 0;
        this.illegalOperations = new ArrayList<>();
    }

    public void deposit(long amount) {
        if (amount > 0) {
            balance += amount;
        } else {
            illegalOperations.add("DEPOSIT");
        }
    }

    public void placeBet(String matchId, long amount, String side, Map<String, Double> matchRates, String result) {
        if (amount > 0 && amount <= balance && matchRates.containsKey(matchId)) {
            betsPlaced++;

            // Assuming side is either "A" or "B"
            boolean betWon = result.equals(side);

            if (betWon) {
                // Player won the bet
                balance += Math.floor(amount * matchRates.get(matchId));
                betsWon++;
            } else {
                // Player lost the bet
                balance -= amount;
            }
        } else {
            illegalOperations.add("BET");
        }
    }

    public void withdraw(long amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
        } else {
            illegalOperations.add("WITHDRAW");
        }
    }

    public boolean isLegitimate() {
        return illegalOperations.isEmpty();
    }

    public String getId() {
        return id;
    }

    public long getBalance() {
        return balance;
    }

    public double getWinRate() {
        return betsPlaced == 0 ? 0 : (double) betsWon / betsPlaced;
    }

    public String getFirstIllegalOperation() {
        return illegalOperations.isEmpty() ? null : illegalOperations.get(0);
    }

    @Override
    public int compareTo(Player otherPlayer) {
        return this.getId().compareTo(otherPlayer.getId());
    }
}

public class BettingProcessor {
    private static Map<String, Player> players = new HashMap<>();
    private static Map<String, Double> matchRates = new HashMap<>();

    public static void main(String[] args) {
        readAndProcessData("player_data.txt");
        readAndProcessData("match_data.txt");
        writeResultsToFile();
    }

    private static void readAndProcessData(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processLine(String[] data) {
        if (data.length < 2) {
            return;
        }

        String playerId = data[0];
        String operation = data[1];

        try {
            switch (operation) {
                case "DEPOSIT":
                    players.computeIfAbsent(playerId, Player::new).deposit(Long.parseLong(data[3]));
                    break;
                case "BET":
                    Player player = players.get(playerId);
                    if (player != null && matchRates.containsKey(data[2]) && isNumeric(data[4])) {
                        player.placeBet(data[2], Long.parseLong(data[4]), data[5], matchRates, data[6]);
                    }
                    break;
                case "WITHDRAW":
                    Player withdrawPlayer = players.get(playerId);
                    if (withdrawPlayer != null) {
                        withdrawPlayer.withdraw(Long.parseLong(data[3]));
                    }
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static boolean isNumeric(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void writeResultsToFile() {
        try (PrintWriter writer = new PrintWriter("result.txt")) {
            writeLegitimatePlayers(writer);
            writer.println();
            writeIllegitimatePlayers(writer);
            writer.println();
            writeCasinoHostBalance(writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void writeLegitimatePlayers(PrintWriter writer) {
        players.values().stream()
                .filter(Player::isLegitimate)
                .sorted()
                .forEach(player -> writer.println(player.getId() + " " + player.getBalance() + " " + String.format("%.2f", player.getWinRate())));
    }

    private static void writeIllegitimatePlayers(PrintWriter writer) {
        players.values().stream()
                .filter(player -> !player.isLegitimate())
                .sorted(Comparator.comparing(Player::getId))
                .forEach(player -> {
                    String operation = player.getFirstIllegalOperation();
                    String formattedLine = String.format("%s %s %s %s %s",
                            player.getId(),
                            operation,
                            (operation.equals("WITHDRAW")) ? "null" : player.getBalance(),
                            (operation.equals("WITHDRAW")) ? "null" : "0.0",
                            (operation.equals("WITHDRAW")) ? "null" : "null");
                    writer.println(formattedLine);
                });
    }

    private static void writeCasinoHostBalance(PrintWriter writer) {
        long hostBalance = players.values().stream()
                .filter(Player::isLegitimate)
                .mapToLong(Player::getBalance)
                .sum();
        writer.println(hostBalance);
    }
}
