import java.util.*;

import static java.lang.String.valueOf;

public class Round {
    /**
     * Represents an active Round in the Hangman Game.
     * Specifically, keeps track and updates the main state
     * of the game comprised of:
     * - The selected word to play
     * - An active dictionary which includes only
     *   words equal to the playing word with
     *   respect to:
     *       a. the length of the word
     *       b. the already shown letters
     * - A list of chars for each word's position with their
     *   probability to be the correct guess.
     * - Number of total and correct guesses
     * - History of guesses
     * - The state of the guessing word.
     *
     * In a nutshell, this class does the following
     * in each round:
     * 1. Removes unlike words in regard to the
     *    selected one.
     * 2. Computes the new probabilities for chars in
     *    each position
     * 3. Computes the points of a winning or losing round
     *
     * @param word A string that is the currently playing word
     * @param dictionary A Set<String> of words
     */
    private final String word;
    private final Set<String> dictionary;
    private Vector<HashMap<Character, Float>> char_probabilities;
    private int correct_guesses = 0;
    private int total_guesses = 0;
    private ArrayList<Character> past_guesses;
    private Boolean[] guessing_word;

    Round(String word, Set<String> dictionary) {
        this.word = word;

        this.dictionary = new HashSet<String>(dictionary);
        this.dictionary.removeIf(temp_word ->
                temp_word.length() != this.word.length());

        this.char_probabilities = new Vector<HashMap<Character,
                Float>>(this.word.length());
        for (int i = 0; i < this.word.length(); i++) {
            this.char_probabilities.add(new HashMap<Character, Float>());
        }

        this.past_guesses = new ArrayList<Character>();

        this.guessing_word = new Boolean[this.word.length()];
        Arrays.fill(guessing_word, false);

        this.compute_probabilities();
    }

    private void remove_unlike_words(int pos_guess, char char_guess,
                                     boolean success) {
        // irrespective
        // dissimilar
        /**
         * Removes unlike words in regard to the current playing word.
         * There are two possible cases when a user guesses a char
         * c in a position x in the word:
         * 1. Success: Removes all the words that **DO NOT** have char c
         *             in position x
         * 2. Failure: Removes all the words that **DO** have char c
         *             in position x
         */
        if (success)
            this.dictionary.removeIf(word
                    -> word.charAt(pos_guess) != char_guess);
        else
            this.dictionary.removeIf(word
                    -> word.charAt(pos_guess) == char_guess);

    }

    private int compute_points(int pos_guess, char char_guess,
                               boolean success) {
        /**
         * Computes the points that a player gets in a round
         * considering a successful or not guess and in regard to
         * the win probabilities of the guess.
         *
         * Specifically:
         * If the player guesses correctly:
         *   - 5 points: if P > 0.6
         *   - 10 points: if  0.4 < P <= 0.6
         *   - 15 points: if 0.25 < P <= 0.4
         *   - 30 points: if P <= 0.25
         * Else:
         *   - Loses 15 points
         */
        if (success) {
            float probability = this.char_probabilities.get(pos_guess)
                    .get(char_guess);
            if (probability >= 0.6)
                return 5;
            else if (probability < 0.6 && probability >= 0.4)
                return 10;
            else if (probability < 0.4 && probability >= 0.25)
                return 15;
            else
                return 30;
        }
        return -15;
    }

    private void compute_probabilities() {
        /**
         * Computes the probability for a character to be the
         * correct guess in a word's position by finding the
         * percentage of words in the dictionary that have this
         * character in the same position.
         *
         * **NOTE**: char_probabilities is equal to a dictionary's
         *           word size. Both sizes are equal to the size of
         *           the selected word.
         *
         * This function does the following:
         * 1. Iterates over char_probabilities Vector.
         * 2. For each Map(char, float) (one for each position):
         *     - Clears the Map
         *     - For each String in the dictionary:
         *         - Gets the char of the current position
         *         - If it is the first char found in this position:
         *           adds it to the Map with counter = 1
         *         - Else, increases counter for this char
         *     - Divides the number of appearances of a char in a
         *       position with the number of words in total in the
         *       dictionary to compute the asked percentage.
         * 3. Sets the updated map to the char_probability vector.
         */
        HashMap<Character, Float> work_map;
        for (int i = 0; i < this.char_probabilities.size(); i++) {
            work_map = this.char_probabilities.get(i);
            work_map.clear();
            for (String word: this.dictionary) {
                Character work_char = word.charAt(i);
                work_map.put(work_char,
                        work_map.getOrDefault(work_char, 0.0f) + 1);
            }
            work_map.replaceAll((key, value) ->
                    value = value / this.dictionary.size());
            this.char_probabilities.set(i, work_map);
        }

    }

    public int play(int pos_guess, char char_guess) {
        /**
         * Plays a new round in the game where the player guesses a
         * character in a certain position in the word.
         * Computes the points of the player considering a successful
         * or not guess. Updates the state of the game accordingly.
         *
         * More specifically:
         * 1. Adds guess to past guesses for keeping track of history
         * 2. Increments the total number of guesses
         * 3. Checks if the guess is correct
         * 4. If yes:
         *      - Updates current guessing word
         *      - Increments number of correct guesses
         * 5. Computes number of points of this round for the player
         *    in regard to probability.
         * 6. Removes unlike words
         * 7. Computes new probabilities
         *
         * **NOTE**: If there's already a correct guess in the
         *           position, returns 0 points. This will avoid
         *           adding points for the same guess.
         */
        boolean success = false;
        if (guessing_word[pos_guess])
            return 0;
        this.past_guesses.add(char_guess);
        this.total_guesses++;
        if (char_guess == this.word.charAt(pos_guess)) {
            this.guessing_word[pos_guess] = true;
            this.correct_guesses++;
            success = true;
        }
        int points = compute_points(pos_guess, char_guess, success);
        remove_unlike_words(pos_guess, char_guess, success);
        compute_probabilities();
        return points;
    }

    public boolean end_of_game() {
        return correct_guesses >= this.word.length();
    }

    public HashMap<Character, Float> getCharProbabilities(int position) {
        return this.char_probabilities.get(position);
    }

    public ArrayList<Character> getPastGuesses() {
        return past_guesses;
    }

    public Set<String> getDictionary() {
        return this.dictionary;
    }


}