import org.json.JSONObject;

import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.net.http.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class Dictionary {
    /**
     * Builds a dictionary of words loaded from various descriptions
     * found in millions of books from the OpenLibrary.
     * The ID specifies which book's description will be used.
     *
     * In a nutshell, this class does the following:
     * 1. Checks if dictionary of same type/ID already exists
     * 2. If yes:
     *    - Finds the path to the file
     *    - Loads the dictionary from the file
     *    - Returns the newly created object for later use
     * 3. If not:
     *    - Gets the book's description using the unique ID
     *    - Processes description to create a valid dictionary
     *    - Saves it to a file
     *    - Returns the dictionary
     *
     * Dictionary Properties:
     *
     * 1. Every word has to be unique.
     * 2. The dictionary has to include at least 20 candidate words.
     * 3. All words need to have at least 6 characters.
     * 4. 20% of all the words has to include at least 9 characters.
     *
     * @param  book_id a String that represents the unique ID of a book
     * @return         a Dictionary object specified from the book ID
     */
    private final String book_id;
    private final String filename;
    private final Path filepath;
    private String DICTIONARIES_FILENAME = ".medialab";
    private final Set<String> dictionary;
    Dictionary(String book_id) throws IOException, InterruptedException,
            HttpErrorException, InvalidRangeException, UndersizeException,
            UnbalancedException {
        this.book_id = book_id;
        this.filename = String.join("",
                                   "hangman_",
                                             book_id, ".txt");
        this.filepath = Paths.get(DICTIONARIES_FILENAME,
                                  this.filename);
        this.dictionary = new HashSet<String>();
        if (Files.notExists(this.filepath)) {
            String description = this.get_book_description();
            this.process_description(description);
            this.validate_dictionary();
            this.save_dictionary();
        }
        else {
            this.load_dictionary_words();
            this.validate_dictionary();
        }

    }
    public Set<String> getDictionary() {
        return this.dictionary;
    }


    private String get_book_description() throws IOException,
            InterruptedException, HttpErrorException {
        /**
         * Gets the book description by making an HTTP GET request.
         *
         * This request has the form: GET <uri>/works/<book_id>.
         * In return, we get the following JSON structure:
         * {
         *     "description":
         *     {
         *         "type": "",
         *         "value": ""
         *     },
         *     "identifiers":
         *     {
         *          ""
         *     }
         * }
         * From the 'description' we keep the 'value' field.
         *
         * We use the 'HTTPClient' Java module for the
         * HTTP GET request. Specifically, this function does the
         * following:
         * 1. Builds an 'HTTP client' which provides configuration
         * information like:
         *    a. the preferred protocol version (HTTP_2)
         *    b. follow redirects
         * 2. Builds an 'HTTP request' which sets:
         *    a. a request's URI: <uri>/works/book_id.json
         *    b. the type of the request: GET
         *    c. the headers: Content-type = application/json
         * 3. Sends the request synchronously.
         *    We also configure the 'BodyHandler' which dictates to
         *    interpret the HTTP Response as a 'String'.
         * 4. Gets the 'HTTP Response' which provides methods
         *    for accessing the response.
         * 5. If status code is not equal to 200 throws exception.
         * 6. Else, converts the response string to JSONObject
         * 7. Gets the 'value' field from the description.
         *
         * @return description A string which is a book's description.
         */
        String uri = String.join("",
                       "https://openlibrary.org/works/",
                                 book_id,
                                 ".json");
        HttpClient client = HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                 .uri(URI.create(uri))
                 .timeout(Duration.ofMinutes(2))
                 .header("Content-Type", "application/json")
                 .GET()
                 .build();
        HttpResponse<String> response = client.send(request,
                                                    BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JSONObject book_info = new JSONObject(response.body());
            return book_info.getJSONObject("description")
                    .getString("value");

        }
        else {
            String error_msg = "The HTTP GET request failed.";
            throw new HttpErrorException(response.statusCode(),
                                         error_msg);
        }
    }

    private void process_description(String description) {
        /**
         * Processes a book's description to convert it into
         * a Set of words.
         *
         * 1. Splits description into words -> Array of Strings.
         *    The delimiter is everything except word characters:
         *    [a-zA-Z0-9_] == '\W'.
         * 2. Creates a dictionary 'Set'. We use a Set because we
         *    accept no duplicates.
         * 3. Adds the valid (over 6 chars) words into the Set.
         *
         * @param description A string which is a book's description.
         * @return dictionary A Set of Strings which represents
         *                    the dictionary.
         */
        String[] words = description.split("\\W+");
        for (String word: words) {
            if (word.length() >= 6) {
                this.dictionary.add(word);
            }
        }
    }
    private void validate_dictionary() throws UndersizeException,
            InvalidRangeException, UnbalancedException {
        /**
         * Validates a dictionary based on the defined properties.
         * See them in the class' doc.
         *
         * Checks:
         * 1. if the length of the dictionary is lower than 20.
         *    If so, throws an 'UndersizeException'.
         * 2. if all the words' length is over 6 characters.
         *    If not, throws an 'InvalidRangeException'.
         * 3. if at least 20% of the words have 9 chars and more.
         *    If not, throws an 'UnbalancedException'.
         */
        int dictionary_size = this.dictionary.size();
        if (dictionary_size < 20) {
            throw new UndersizeException("The dictionary has to" +
                                         " include at least 20" +
                                         " candidate words");
        }
        int num_large_words = 0;
        for (String word: this.dictionary) {
            if (word.length() < 6)
                throw new InvalidRangeException("All words in the" +
                                                " the dictionary" +
                                                " need to have at" +
                                                " at least 6 chars");
            if (word.length() >= 9)
                num_large_words++;
        }
        if ((float) num_large_words / dictionary_size < 0.2) {
            throw new UnbalancedException("The 20% of the dict's" +
                                          " words needs to have at" +
                                          " least 9 characters.");

        }
    }
    private void save_dictionary() throws IOException {
        /**
         * Saves the dictionary into a file.
         *
         * Creates the corresponding '.medialab/<book_id>.txt'
         * file, and writes the dictionary's content there.
         * File contents: One word per line.
         *
         * This function does the following:
         * 1. Creates a new File object for the '.medialab' path.
         * 2. Gets the absolute path and checks its existence.
         *    If is not found, it creates the dir.
         * 3. Creates a File object for the '.medialab/<book_id>.txt'
         *    and creates the corresponding file.
         * 4. Iterates through the dictionary Set and stores each word
         *    in the file with a trailing newline.
         */
        File dictionaries_dir = new File(this.DICTIONARIES_FILENAME);
        File abs_dictionaries_dir = dictionaries_dir.getAbsoluteFile();
        if (!abs_dictionaries_dir.exists()) {
            // Handle the case where the folder cannot be created
            dictionaries_dir.mkdirs();
        }
        File dictionary_file = new File(String.valueOf(this.filepath));
        dictionary_file.createNewFile();
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(dictionary_file)))) {
            for (String word : this.dictionary) {
                System.out.println(word);
                writer.println(word);
            }
        }
    }

    public void load_dictionary_words() throws IOException {
        /**
         * Loads a list of words to the dictionary from
         * a provided filepath.
         *
         * 1. Creates a new File object specified by the filepath.
         * 2. Reads line by line the contents of the file.
         * 3. Adds every line (== word in our case) to the dictionary.
         */
        File dictionary_file = new File(String.valueOf(this.filepath));
        try (BufferedReader bufReader = new BufferedReader(
                new FileReader(dictionary_file))) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                this.dictionary.add(line);
            }
        }
    }


    public int get_dictionary_len() {
        return this.dictionary.size();
    }


}
