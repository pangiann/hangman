import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import static java.lang.String.valueOf;

public class Hangman {

    public static void print_probabilities(int pos, Round round) {
        System.out.println("Position = " + valueOf(pos));
        HashMap<Character, Float> char_probs = round.getCharProbabilities(pos);
        char_probs.forEach((key, value) -> {
            System.out.println("key-value = " + key + " " + value);
        });
        System.out.print("\n\n");


    }

    public static void main(String[] args) throws IOException,
            InterruptedException, HttpErrorException, InvalidRangeException,
            UndersizeException, UnbalancedException {

        Dictionary my_dictionary = new Dictionary("OL31390631M");
        String selected_word = "BREWING";
        Round round = new Round("BREWING",
                my_dictionary.getDictionary());

        Player player = new Player();
        //print_probabilities(selected_word, new_round);
        while (!round.end_of_game() && player.is_alive()) {
            System.out.print("Choose the position of your guess: ");

            Scanner sc = new Scanner(System.in);
            int position = sc.nextInt();

            print_probabilities(position, round);
            System.out.print("Choose the character of your guess: ");
            char guess_char = sc.next().charAt(0);

            int points = round.play(position, guess_char);
            if (points < 0) {
                player.reduce_lives();
            }
            player.update_points(points);
            System.out.println("Number of points: " + player.getPoints());
            System.out.println("Remaining lives: " + player.getLives());


        }



        //print_probabilities(selected_word, new_round);


    }
}