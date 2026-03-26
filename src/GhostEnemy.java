public class GhostEnemy extends Enemy {

    // İstersen sabitleri burada tut
    private static final int DEFAULT_MAX_HEALTH = 1;
    private static final double DEFAULT_SPEED = 3.4;   // şimdilik (tile / tick gibi düşün)
    private static final int DEFAULT_KILL_REWARD = 25;

    public GhostEnemy(int[] spawn, GameMap myMap) {
        super(spawn, DEFAULT_MAX_HEALTH, DEFAULT_SPEED, DEFAULT_KILL_REWARD, myMap);
    }
    @Override
    public String getSpritePath() {
        return "/assets/GhostEnemy.png";
    }



}
