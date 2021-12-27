public class Player {
    /**
     * Represents the Player's state during the execution of the
     * Game. Keeps track and updates accordingly:
     * 1. The remaining lives of the player
     * 2. The points they have gained so far
     */
    private int points;
    private int lives;

    Player() {
        this.points = 0;
        this.lives = 6;
    }

    public void update_points(int new_points) {
        /**
         * Updates the points of the player.
         * Points can only be a zero or positive number.
         */
        if (this.points + new_points < 0)
            this.points = 0;
        else this.points += new_points;
    }

    public void reduce_lives() {
        /**
         * Decrements the number of player's lives.
         */
        this.lives--;
    }

    public int getPoints() {
        return this.points;
    }

    public int getLives() {
        return this.lives;
    }

    public boolean is_alive() {
        return lives > 0;
    }


}
