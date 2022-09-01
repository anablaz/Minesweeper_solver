package vpisnaStevilkaAna;

import skupno.*;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.*;

public class Stroj_Ana implements Stroj {
    public static final boolean DEBUG_PRINTS = false;
    public static final int UNOPENED_FIELD = -1;

    /**
     * Field height
     */
    private int height;

    /**
     * Field width
     */
    private int width;

    /**
     * Number of moves
     */
    private int move;

    /**
     * Queue of fields to process
     */
    private Queue<Polje> queue;

    /**
     * Set to assure fields are only once in the queue
     */
    private Set<Polje> fieldsInQueue;

    /**
     * Fields which have all their neighbours open or marked as mines
     */
    private Set<Polje> completedFields;

    /**
     * Number of mines not found yet
     */
    private int numberOfMinesLeft;

    /**
     * Position of mines
     */
    private boolean[][] mines;

    /**
     * Randomness generator
     */
    private Random random;

    /**
     * Initialize all properties
     */
    @Override
    public void zacetek(int visina, int sirina, int stMin) {
        System.out.printf("<%s> zacetek(%d, %d, %d)%n%n", LocalTime.now(), visina, sirina, stMin);
        height = visina;
        width = sirina;
        move = 1;
        queue = new LinkedList<>();
        fieldsInQueue = new HashSet<>();
        completedFields = new HashSet<>();
        numberOfMinesLeft = stMin;
        mines = new boolean[visina][sirina];
        random = new Random(visina * 29 + sirina * 13 + stMin);
    }

    /**
     * Make a turn
     */
    @Override
    public Polje izberi(int[][] stanje, long preostaliCas) {
        System.out.printf("<%s> [%d] izberi(stanje, %d)%n", LocalTime.now(), this.move, preostaliCas);
        if (DEBUG_PRINTS) {
            this.printGameState(stanje);
        }

        Polje chosenField;
        if (this.move == 1) {
            chosenField = chooseForFirstRound();
        } else {
            chosenField = chooseForLaterRounds(stanje);
        }
        enqueueSafeFields(getOpenedNeighbours(chosenField, stanje));
        this.move++;

        if (DEBUG_PRINTS) {
            System.out.println("My choice: " + chosenField);
            System.out.println();
        }

        return chosenField;
    }

    /**
     * Print mines at game end
     */
    @Override
    public void konecIgre(boolean[][] mine, int razlog, int stOdprtih) {
        System.out.printf("<%s> konecIgre(mine, %s, %d)%n",
                LocalTime.now(), Konstante.OPIS[razlog], stOdprtih);
        printMines(mine);
    }

    private Polje chooseForFirstRound() {
        Polje chosenField = new Polje(this.height / 2, this.width / 2);
        enqueue(chosenField);

        return chosenField;
    }

    private void enqueue(Polje field) {
        if (!fieldsInQueue.contains(field) && !completedFields.contains(field)) {
            queue.add(field);
            fieldsInQueue.add(field);
        }
    }

    private Polje chooseForLaterRounds(int[][] state) {
        Polje field = dequeue();
        if (field == null) {
            return chooseWithBruteForce(state);
        }

        if (DEBUG_PRINTS) {
            System.out.println("Looking at: " + field);
        }

        List<Polje> neighbourFields = getNeighbourFields(field);
        int number = getNumberAtField(field, state);

        List<Polje> unopenedNeighbours = getUnopenedNeighbours(neighbourFields, state);
        if (unopenedNeighbours.size() == number) {
            for (Polje mine : unopenedNeighbours) {
                markMine(mine);
            }
        }
        if (countNeighbouringMines(neighbourFields) == number) {
            enqueueSafeFields(neighbourFields);
            completedFields.add(field);
        }

        Polje nextField = queue.peek();
        if (nextField != null && getNumberAtField(nextField, state) == UNOPENED_FIELD && !isMineAtField(nextField)) {
            return nextField;
        }
        return chooseForLaterRounds(state);
    }

    private void markMine(Polje mine) {
        boolean wasAlreadyMine = mines[mine.vr()][mine.st()];
        if (!wasAlreadyMine) {
            mines[mine.vr()][mine.st()] = true;
            numberOfMinesLeft--;
        }
    }

    private Polje dequeue() {
        Polje field = queue.poll();
        fieldsInQueue.remove(field);
        return field;
    }

    private void enqueueSafeFields(List<Polje> fields) {
        for (Polje fieldToAdd : fields) {
            if (!isMineAtField(fieldToAdd)) {
                enqueue(fieldToAdd);
            }
        }
    }

    private List<Polje> getUnopenedNeighbours(List<Polje> fields, int[][] state) {
        List<Polje> unopenedFields = new ArrayList<>(8);

        for (Polje fieldToCheck : fields) {
            if (getNumberAtField(fieldToCheck, state) == UNOPENED_FIELD) {
                unopenedFields.add(fieldToCheck);
            }
        }
        return unopenedFields;
    }

    private List<Polje> getOpenedNeighbours(Polje field, int[][] state) {
        List<Polje> unopenedFields = new ArrayList<>(8);
        List<Polje> neighbours = getNeighbourFields(field);

        for (Polje fieldToCheck : neighbours) {
            if (getNumberAtField(fieldToCheck, state) != UNOPENED_FIELD) {
                unopenedFields.add(fieldToCheck);
            }
        }
        return unopenedFields;
    }

    private int getNumberAtField(Polje field, int[][] state) {
        return state[field.vr()][field.st()];
    }

    private int countNeighbouringMines(List<Polje> fields) {
        int numberOfMines = 0;

        for (Polje field : fields) {
            if (isMineAtField(field)) {
                numberOfMines++;
            }
        }
        return numberOfMines;
    }

    private boolean isMineAtField(Polje field) {
        return mines[field.vr()][field.st()];
    }

    private List<Polje> getNeighbourFields(Polje field) {
        List<Polje> neighbours = new ArrayList<>(8);
        int row = field.vr();
        int column = field.st();
        if (row > 0 && column > 0) {
            neighbours.add(new Polje(row - 1, column - 1));
        }
        if (column > 0) {
            neighbours.add(new Polje(row, column - 1));
        }
        if (row < height - 1 && column > 0) {
            neighbours.add(new Polje(row + 1, column - 1));
        }
        if (row > 0) {
            neighbours.add(new Polje(row - 1, column));
        }
        if (row < height - 1) {
            neighbours.add(new Polje(row + 1, column));
        }
        if (row > 0 && column < width - 1) {
            neighbours.add(new Polje(row - 1, column + 1));
        }
        if (column < width - 1) {
            neighbours.add(new Polje(row, column + 1));
        }
        if (row < height - 1 && column < width - 1) {
            neighbours.add(new Polje(row + 1, column + 1));
        }

        return neighbours;
    }

    private Polje chooseWithBruteForce(int[][] state) {
        List<Polje> unopenedFields = new ArrayList<>();
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                if (state[i][j] == UNOPENED_FIELD && !mines[i][j]) {
                    unopenedFields.add(new Polje(i, j));
                }
            }
        }

        int numberOfFields = unopenedFields.size();
        if (DEBUG_PRINTS) {
            System.out.println("BRUTEFORCE " + numberOfFields);
        }
        if (numberOfFields > 32) {
            int randomIndex = random.nextInt(numberOfFields);
            enqueue(unopenedFields.get(randomIndex));
            return queue.peek();
        }

        List<Integer> combinations = getCombinations(state, unopenedFields);

        // Try find certain fields
        int alwaysMine = combinations.get(0);
        int neverMine = ~combinations.get(0);
        for (int combination : combinations) {
            alwaysMine &= combination;
            neverMine &= ~combination;
        }
        for (int i = 0; i < numberOfFields; i++) {
            if (BigInteger.valueOf(alwaysMine).testBit(i)) {
                markMine(unopenedFields.get(i));
            } else if (BigInteger.valueOf(neverMine).testBit(i)) {
                enqueue(unopenedFields.get(i));
            }
        }

        if (queue.isEmpty()) {
            int[] mines = new int[numberOfFields];
            int[] safe = new int[numberOfFields];
            for (int combination : combinations) {
                for (int i = 0; i < numberOfFields; i++) {
                    if (BigInteger.valueOf(combination).testBit(i)) {
                        mines[i]++;
                    } else {
                        safe[i]++;
                    }
                }
            }
            double highestSafety = 0;
            int highestSafetyIndex = 0;
            for (int i = 0; i < numberOfFields; i++) {
                if (mines[i] == 0) {
                    highestSafety = Double.MAX_VALUE;
                    highestSafetyIndex = i;
                } else {
                    double safety = (double) safe[i] / mines[i];
                    if (safety > highestSafety) {
                        highestSafety = safety;
                        highestSafetyIndex = i;
                    }
                }
            }

            enqueue(unopenedFields.get(highestSafetyIndex));
        }

        return queue.peek();
    }

    private List<Integer> getCombinations(int[][] state, List<Polje> unopenedFields) {
        Set<Polje> neighbours = new HashSet<>();
        for (Polje field : unopenedFields) {
            neighbours.addAll(getOpenedNeighbours(field, state));
        }

        double numberOfPossibilities = Math.pow(2, unopenedFields.size());
        List<Integer> combinations = new ArrayList<>();
        for (int combination = 0; combination < numberOfPossibilities; combination++) {
            if (isValidCombination(neighbours, combination, unopenedFields, state)) {
                combinations.add(combination);
            }
        }
        return combinations;
    }

    private boolean isValidCombination(Set<Polje> neighbours, int combination, List<Polje> unopenedFields, int[][] state) {
        var minesInCombination = Integer.bitCount(combination);
        if (minesInCombination != numberOfMinesLeft) {
            return false;
        }
        for (Polje neighbour : neighbours) {
            List<Polje> neighboursNeighbours = getNeighbourFields(neighbour);
            int neighboursMines = getNumberOfMinesForCombination(neighboursNeighbours, unopenedFields, combination);
            if (neighboursMines != getNumberAtField(neighbour, state)) {
                return false;
            }
        }
        return true;
    }

    private int getNumberOfMinesForCombination(List<Polje> fields, List<Polje> unopenedFields, long combination) {
        int neighboursMines = 0;
        for (Polje field : fields) {
            if (isMineAtField(field)) {
                neighboursMines++;
            } else if (unopenedFields.contains(field)) {
                int index = unopenedFields.indexOf(field);
                if (BigInteger.valueOf(combination).testBit(index)) {
                    neighboursMines++;
                }
            }
        }
        return neighboursMines;
    }

    private void printGameState(int[][] state) {
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                if (mines[i][j]) {
                    System.out.print("\u001B[34m+\u001B[0m");
                } else if (state[i][j] < 0) {
                    System.out.print('?');
                } else if (completedFields.contains(new Polje(i, j))) {
                    System.out.print("\u001B[31m" + state[i][j] + "\u001B[0m");
                } else if (queue.contains(new Polje(i, j))) {
                    System.out.print("\u001B[32m" + state[i][j] + "\u001B[0m");
                } else {
                    System.out.print(state[i][j]);
                }
            }
            System.out.println();
        }
    }

    private void printMines(boolean[][] mines) {
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                System.out.print(mines[i][j] ? "+" : "-");
            }
            System.out.println();
        }
    }
}