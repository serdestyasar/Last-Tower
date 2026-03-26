public class CheapTower extends Tower {

    public CheapTower(int x, int y) {
        super(
            x, y,
            50,   // maxHealth
            100,   // price
            2,    // range (tile)
            20,   // damage
            750   // fireRateMs
        );
    }
    
    
}