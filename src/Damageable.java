public interface Damageable {
    int getHealth();
    int getMaxHealth();
    void setHealth(int health);
    
    
    // health -=dmg
    default void takeDamage(int dmg) {
        if (dmg < 0) return;
        setHealth(getHealth() - dmg);
    }
    
    
    
 
    default boolean isDead() {
        return getHealth() <= 0;
    }
 
    
    // health < maxHealth = 1 
    default boolean showHealth() {
    	return (getHealth()<getMaxHealth());
    }
    
    
}
