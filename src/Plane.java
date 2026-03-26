public class Plane {
    public double xTile, yTile;   // tile coord (double)
    public double vx, vy;         // tile/sec
    private int lastTx = Integer.MIN_VALUE;
    private int lastTy = Integer.MIN_VALUE;

    public Plane(double xTile, double yTile, double vx, double vy) {
        this.xTile = xTile;
        this.yTile = yTile;
        this.vx = vx;
        this.vy = vy;
    }

    public void update(double dt) {
        xTile += vx * dt;
        yTile += vy * dt;
    }

    public int tileX() { return (int)Math.floor(xTile); }
    public int tileY() { return (int)Math.floor(yTile); }

    // sprite normalde sağa bakıyor -> angle = atan2(vy, vx)
    public double angleRad() {
        return Math.atan2(vy, vx);
    }

    // yeni bir tile'a ilk kez girdiyse true
    public boolean shouldDropBomb() {
        int tx = tileX(), ty = tileY();
        if (tx == lastTx && ty == lastTy) return false;
        lastTx = tx; lastTy = ty;
        return true;
    }

    public boolean isOutOfMap(int cols, int rows) {
        return (xTile < -3 || xTile > cols + 3 || yTile < -3 || yTile > rows + 3);
    }
}
