public class DefaultEnemy extends Enemy {

    // İstersen sabitleri burada tut
    private static final int DEFAULT_MAX_HEALTH = 60;
    private static final double DEFAULT_SPEED = 1.0;  
    private static final int DEFAULT_KILL_REWARD = 20;

    public DefaultEnemy(int[] spawn, GameMap myMap) {
        super(spawn, DEFAULT_MAX_HEALTH, DEFAULT_SPEED, DEFAULT_KILL_REWARD, myMap);
    }
    @Override
    public String getSpritePath() {
        return "/assets/deafult_enemy.png";
    }



}
