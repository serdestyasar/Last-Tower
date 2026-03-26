public class Projectile {
    public double x, y;          // tile coord
    public double vx, vy;        // tile/sec yön (normalize)
    public double speed;         // tile/sec
    public int damage;

    public Enemy target;         
    public boolean alive = true;

    public Projectile(double startX, double startY, Enemy target, int damage, double speed) {
        this.x = startX;
        this.y = startY;
        this.target = target;
        this.damage = damage;
        this.speed = speed;

        // ilk yönü hesapla
        recalcDirection();
    }

    private void recalcDirection() {
        if (target == null) { vx = 0; vy = 0; return; }
        double tx = target.getX();
        double ty = target.getY();

        double dx = tx - x;
        double dy = ty - y;
        double d = Math.sqrt(dx*dx + dy*dy);
        if (d < 1e-9) { vx = 0; vy = 0; }
        else { vx = dx / d; vy = dy / d; }
    }

    public void update(double dt) {
        if (!alive) return;

        if (target != null && target.isDead()) { alive = false; return; }

        recalcDirection();

        x += vx * speed * dt;
        y += vy * speed * dt;

        // hit testi (tile cinsinden)
        if (target != null) {
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            double dist = Math.sqrt(dx*dx + dy*dy);

            double hitRadius = 0.15; // tile
            if (dist <= hitRadius) {
                target.takeDamage(damage);
                alive = false;
            }
        }
    }
}
