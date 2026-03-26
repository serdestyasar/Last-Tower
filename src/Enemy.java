import java.util.ArrayList;
import java.util.Arrays;
public abstract class Enemy implements Damageable {
    protected double x, y;  // mevcut koordinatlar
    protected int health;
    protected int maxHealth;

    protected double speed;   
    protected int killReward; // öldürünce kazanılan para
    protected int pathIndex =0 ; // path in hangi indexinde
    protected boolean reachedEnd = false; // end'e ulaştı mı?
    protected ArrayList<ArrayList<Integer>> path = new ArrayList<>();  // ilerleyeceği path [[x1,y1],[x2,y2]] şeklinde
    
    //dönmesi için
    protected double facingRad = 0;
    public double getFacingRad() { return facingRad; }
    
    public Enemy(int[] spawn , int maxHealth, double speed, int killReward,GameMap myMap) {
        this.x = spawn[0];
        this.y = spawn[1];

        this.maxHealth = maxHealth;
        this.health = maxHealth;

        this.speed = speed;
        this.killReward = killReward;
        
        this.path = calculatePath(myMap);
    }
    
    
    //path i setler , 0'ıncı index spawn değil onun yanındaki yer path -> path [[x1,y1],[x2,y2]] şeklinde , en sonda end var 
    public ArrayList<ArrayList<Integer>> calculatePath(GameMap myMap){
    	
    	
    	ArrayList<ArrayList<Integer>> path = new ArrayList<>(); 
    	int[] current_location = myMap.getSpawn();
    	
    	
    	
    	int current_x = current_location[0];
    	int current_y = current_location[1];
    	boolean is_end_found = false;
    	
    	while(!is_end_found) {
    		
    		//path in son elemanını current olarak ayarla
    		if(path.size()>0) {
    			current_x = path.get(path.size() - 1).get(0);
    			current_y = path.get(path.size() - 1).get(1);

    		}
    		
	    	for(int x=-1 ; x<2 ; x++ ) {
	    		for (int y = -1 ; y<2 ; y++) {
	    			if((x == 0 && y != 0) || (x != 0 && y == 0)) { // çapraz yok sağ sol ileri geri
		    			int check_x = current_x + x;
		    			int check_y = current_y + y ;
		    			
		    			if(myMap.inBounds(check_x, check_y)) { //sınır kontrol
		    				if(myMap.isPath(check_x, check_y)) { //path mi 
		    					
		    					//path de zaten var mı 
		    					boolean already_exists = false;
		    					if(path.size()>0) {
			    					for(ArrayList<Integer>p : path) {
			    						if(p.get(0) ==check_x && p.get(1)==check_y) {
			    							already_exists = true;
			    						}
			    					}
		    					}
		    					
		    					if(!already_exists) path.add(new ArrayList<>(Arrays.asList(check_x,check_y)));
		    				}
		    				// end bulundu mu bulunduyse returnle
		    				else if(myMap.isEnd(check_x, check_y)) {
		    					path.add(new ArrayList<>(Arrays.asList(check_x,check_y)));
		    					
		    					is_end_found = true;
		    					return path;
		    				}
		    			}
		    			
		    		}
	    		}
	    	}
    	}
    	
    	return path;
    }
    //png dosyası override ediliyor subclasslarda
    public String getSpritePath() {
        return "/assets/enemy_default.png";
    }



	public double getX() {
		return x;
	}


	public int getKillReward() {
		return killReward;
	}


	public void setKillReward(int killReward) {
		this.killReward = killReward;
	}


	public void setX(double x) {
		this.x = x;
	}


	public double getY() {
		return y;
	}


	public void setY(double y) {
		this.y = y;
	}
	//damagable 
	@Override
	public int getHealth() {
	    return health;
	}

	@Override
	public int getMaxHealth() {
	    return maxHealth;
	}

	@Override
	public void setHealth(int h) {
	    health = h;
	}
	

	

	public boolean hasReachedEnd() {
	    return reachedEnd;
	}

	public void update(double dt) {
	    if (dt <= 0) return;
	    if (reachedEnd) return;
	    if (isDead()) return; // Damageable default isDead() varsa

	    if (path == null || path.isEmpty()) {
	        // path yoksa map hatalı vs. -> hareket edemez
	        return;
	    }

	    // path bitti mi?
	    if (pathIndex >= path.size()) {
	        reachedEnd = true;
	        return;
	    }

	    int targetX = path.get(pathIndex).get(0);
	    int targetY = path.get(pathIndex).get(1);

	    double dx = targetX - x;
	    double dy = targetY - y;
	    double dist = Math.sqrt(dx*dx + dy*dy);
	    //dönme yönü
        if (Math.abs(dx) > 1e-9 || Math.abs(dy) > 1e-9) {
            facingRad = Math.atan2(dy, dx);
        }
	    // hedefin üstündeyse bir sonraki node'a geç
	    if (dist < 1e-9) {
	        pathIndex++;
	        if (pathIndex >= path.size()) reachedEnd = true;
	        return;
	    }

	    double step = speed * dt; 

	    if (step >= dist) {
	        // hedef node'a ulaş
	        x = targetX;
	        y = targetY;
	        pathIndex++;
	        if (pathIndex >= path.size()) reachedEnd = true;
	    } else {
	        // hedefe doğru yaklaş
	        x += (dx / dist) * step;
	        y += (dy / dist) * step;
	    }
	}

    
}
