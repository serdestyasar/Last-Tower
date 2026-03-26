import java.util.ArrayList;

public abstract class Tower implements Damageable {

    protected int x, y;          // tile koordinatı
    protected int health;
    protected int maxHealth;

    protected int price;         //fiyatı
    protected int range;         // tile cinsinden (örn 2 => 2 tile)
    protected int damage;
    protected int fireRateMs;    // kaç ms'de bir vurur

    protected long lastShotTimeMs = 0;
    
    //dönme
    protected double facingRad = 0;
    public double getFacingRad() { return facingRad; }
    
    public Tower(int x, int y, int maxHealth, int price, int range, int damage, int fireRateMs) {
        this.x = x;
        this.y = y;

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.price = price;
        this.range = range;
        this.damage = damage;
        this.fireRateMs = fireRateMs;
    }

    // ---- Damageable ----
    @Override
    public int getHealth() { 
    	return health; 
    	}

    
    public void setPrice(int price) {
		this.price = price;
	}

	@Override
    public int getMaxHealth() {
    	return maxHealth;
    	}

    @Override
    public void setHealth(int h) { 
    	this.health = h; 
    	}

    // ---- Getters ----
    public int getX() { 
    	return x; 
    	}
    public int getY() { 
    	return y;
    	}
    public int getPrice() {
    	return price;
    	}
    public int getRange() { 
    	return range; 
    	}
    public int getDamage() { 
    	return damage;
    	}

    
    //enemy range de mi evet ise 1 
    protected boolean isInRange(Enemy enemy) {

        if (enemy == null) return false;

        double ex = enemy.getX();
        double ey = enemy.getY();
        

        double dx = ex - this.x;
        double dy = ey - this.y;

        return (dx * dx + dy * dy) <= (range * range);
    }
    
    // en öndeki ve rangeinde enemy ye ateş et
	public void update(long nowMs, ArrayList<Enemy> enemies) {
	    if (enemies == null || enemies.isEmpty()) return;
	
	    // fire rate kontrolü
	    if (nowMs - lastShotTimeMs < fireRateMs) return;
	
	    for (Enemy e : enemies) {
	        if (e == null) continue;
	

	        if (e.getHealth() <= 0) continue;
	
	        if (isInRange(e)) {

	            e.setHealth(Math.max(0, e.getHealth() - damage));
	

	            lastShotTimeMs = nowMs;
	            break; 
	        }
	    }
	}
	
	public void tryShoot(ArrayList<Enemy> enemies, ArrayList<Projectile> projectiles, long nowMs) {
	    if (nowMs - lastShotTimeMs < fireRateMs) return;

	    for (Enemy en : enemies) {
	        if (!en.isDead() && isInRange(en)) {
	            double sx = this.x + 0.5; // tower center (tile)
	            double sy = this.y + 0.5;

	            // enemy center tile
	            double ex = en.getX();
	            double ey = en.getY();

	            // projectile: start tower center, target enemy
	            projectiles.add(new Projectile(sx, sy, en, this.damage, 8.0 /*tile/sec*/));
	            
	            //dönmek için
	            if (en != null) {
	                double dx = en.getX() - this.x;
	                double dy = en.getY() - this.y;
	                facingRad = Math.atan2(dy, dx);
	            }
	            lastShotTimeMs = nowMs;
	            break;
	        }
	    }
	}

	protected boolean isDestroyed() {
		if(health<=0) return true;
		return false;
	};

}
