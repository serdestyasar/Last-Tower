import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.GlyphVector;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class MainApp extends JFrame {
	//tower n enemies
	private final ArrayList<Tower> towers = new ArrayList<>();
	private final ArrayList<Enemy> enemies = new ArrayList<>();
	private final ArrayList<Projectile> projectiles = new ArrayList<>();
	
	private enum TowerType {
	    CHEAP(100, "/assets/CheapTower.png", 2),
	    T400 (400, "/assets/Tower400.png", 3),
		T800 (400, "/assets/Tower800.png", 4),
		T1200 (400, "/assets/Tower1200.png", 6);
	    final int cost;
	    final String iconPath;
	    final int rangeTiles;

	    TowerType(int cost, String iconPath, int rangeTiles) {
	        this.cost = cost;
	        this.iconPath = iconPath;
	        this.rangeTiles = rangeTiles;
	    }
	}
	
	private final ArrayList<Plane> planes = new ArrayList<>();
	private final java.util.Random rng = new java.util.Random();

	private static final int SCORCH_MS = 12000;        // iz ne kadar kalsın
	private static final int PLANE_BOMB_DAMAGE = 40;   // tower HP’den düşülecek
	private boolean scoreSaved = false;                // leave / win double save olmasın
	


	private boolean paused = false;
	private JButton btnPause;   // topbarda text değiştirmek için

	private TowerType selectedTowerType = null;
	
	//sellmode 
	private boolean sellMode = false;
	private int final_Score;
	//bardaki money ve health
	private JLabel lblMoney;
	private JLabel lblHealth;
	private JLabel lblScore;
	private int currentMapId = 1;
	private boolean gameEnded = false;

	private int money = 1000;
	private int healthPercentage = 100;

    private boolean logged = false;
    
    private String player_Name;

    // Game state
    private GameMap currentMap;  
    private MapPanel mapPanel;
    private JPanel root; 

    private long lastNs;
    
    public MainApp() {
    	showMainMenu();
    	setVisible(true);

    }
    
    private void showMainMenu() {
    	
        setContentPane(new MenuPanel());
        setSize(900, 960);
        revalidate();
        repaint();
    }
    class MenuPanel extends JPanel {
        private BufferedImage bg;

        MenuPanel() {
            setLayout(new GridBagLayout());
            try {
                bg = ImageIO.read(getClass().getResource("/assets/menu_bg.png"));
            } catch (Exception e) {
                bg = null;
            }

            JPanel box = new JPanel();
            box.setOpaque(false);
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

            JButton bStart  = makeMenuBtn("START",  new Color(0, 150, 255));
            JButton bLogin  = makeMenuBtn("LOGIN",  new Color(0, 150, 255));
            JButton bCred   = makeMenuBtn("CREDITS",new Color(0, 150, 255));
            JButton bHigh   = makeMenuBtn("High Scores", new Color(255, 140, 0));

            bStart.addActionListener(e -> {
                try { start(); } catch (IOException ex) { ex.printStackTrace(); }
            });
            bLogin.addActionListener(e -> {
                try { login(); } catch (IOException ex) { ex.printStackTrace(); }
            });
            bCred.addActionListener(e -> credits());
            bHigh.addActionListener(e -> high_Scores());

            box.add(bStart);  box.add(Box.createVerticalStrut(10));
            box.add(bLogin);  box.add(Box.createVerticalStrut(10));
            box.add(bCred);   box.add(Box.createVerticalStrut(10));
            box.add(bHigh);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.insets = new Insets(120, 0, 0, 0); // title altına indir
            add(box, gbc);
        }

        private JButton makeMenuBtn(String text, Color bgc) {
            JButton b = new JButton(text);
            b.setPreferredSize(new Dimension(200, 45));
            b.setFont(new Font("SansSerif", Font.BOLD, 16));
            b.setForeground(Color.WHITE);
            b.setBackground(bgc);
            b.setFocusPainted(false);
            return b;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            if (bg != null) {
                g2.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
            }

            drawOutlinedTitle(g2, "TOWER DEFENSE");
        }

        private void drawOutlinedTitle(Graphics2D g2, String text) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Font font = new Font("SansSerif", Font.BOLD, 64);
            GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);

            int x = (getWidth() - (int) gv.getVisualBounds().getWidth()) / 2;
            int y = 110;

            Shape shape = gv.getOutline(x, y);

            g2.setStroke(new BasicStroke(10f));
            g2.setColor(new Color(0, 0, 0, 160));
            g2.draw(shape);

            g2.setColor(Color.WHITE);
            g2.fill(shape);
        }
    }


 // ENEMY SPAWN SCHEDULE
    private static class SpawnEvent {
        final int atMs;
        final java.util.function.Supplier<Enemy> factory;
        SpawnEvent(int atMs, java.util.function.Supplier<Enemy> factory) {
            this.atMs = atMs;
            this.factory = factory;
        }
    }

    private static class UiEvent {
        final int atMs;
        final Runnable action;
        UiEvent(int atMs, Runnable action) {
            this.atMs = atMs;
            this.action = action;
        }
    }

    private final ArrayList<UiEvent> uiSchedule = new ArrayList<>();
    private int nextUiIndex = 0;

    private void uiAt(int atMs, Runnable action) {
        uiSchedule.add(new UiEvent(atMs, action));
    }

    private void runDueUiEvents(int elapsedMs) {
        while (nextUiIndex < uiSchedule.size()
                && uiSchedule.get(nextUiIndex).atMs <= elapsedMs) {
            uiSchedule.get(nextUiIndex).action.run();
            nextUiIndex++;
        }
    }

    private void showTextAt(int atMs, String text) {
        uiAt(atMs, () -> {
            if (mapPanel != null) mapPanel.showBanner(text, 3000);
        });
    }
    private void togglePause() {
        if (gameTimer == null) return;

        paused = !paused;

        if (paused) {
            gameTimer.stop();
            if (btnPause != null) btnPause.setText("RESUME");
        } else {
            // dt sıçramasın diye zamanı resetle
            lastNs = System.nanoTime();
            gameStartMs = System.currentTimeMillis() - (int)(System.currentTimeMillis() - gameStartMs);
            gameTimer.start();
            if (btnPause != null) btnPause.setText("PAUSE");
        }

        if (mapPanel != null) mapPanel.repaint();
    }



    private final ArrayList<SpawnEvent> spawnSchedule = new ArrayList<>();
    private int nextSpawnIndex = 0;
    private long gameStartMs = 0;


    public void login() throws IOException {
        if (logged) {
            showPopup("You are already logged");
            return;
        }

        JTextField name = new JTextField(15);
        JTextField password = new JTextField(15);

        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Username:"));
        p.add(name);
        p.add(new JLabel("Password:"));
        p.add(password);

        int res = JOptionPane.showConfirmDialog(
                this, p, "Login",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (res != JOptionPane.OK_OPTION) return;

        String in_name = name.getText().trim();
        String in_password = password.getText();

        if (in_name.contains("/") || in_password.contains("/")) {
            showPopup("Name or Password can not contain '/' .");
            return;
        }

        String[][] accounts = DatabaseController.returnACC();

        boolean found = false;
        for (int i = 0; i < accounts[0].length; i++) {
            if (in_name.equals(accounts[0][i])) {
                found = true;
                if (in_password.equals(accounts[1][i])) {
                    logged = true;
                    player_Name = in_name;
                    showPopup("Login successful");
                } else {
                    showPopup("Wrong password");
                }
                break;
            }
        }

        if (!found) {
            DatabaseController.insertACC(in_name, in_password);
            showPopup("Account created");
        }
    }

    public void start() throws IOException {
        if (!logged) {
            showPopup("Please login first");
            return;
        }
        openGameWindow();
    }

    public void credits() { }
    public void high_Scores() {
        try {
            String[][] all = DatabaseController.returnSCORES();
            int len = all[0].length;

            java.util.List<int[]> idx = new java.util.ArrayList<>();
            for (int i = 0; i < len; i++) {
                int sc = Integer.parseInt(all[2][i]);
                idx.add(new int[]{ sc, i }); // [score, originalIndex]
            }

            idx.sort((a, b) -> Integer.compare(b[0], a[0])); // score desc

            int top = Math.min(10, idx.size());
            Object[][] data = new Object[top][4];

            for (int r = 0; r < top; r++) {
                int original = idx.get(r)[1];
                data[r][0] = (r + 1);          // rank
                data[r][1] = all[0][original]; // name
                data[r][2] = all[1][original]; // map
                data[r][3] = all[2][original]; // score
            }

            String[] cols = {"#", "Name", "Map", "Score"};
            JTable table = new JTable(data, cols);
            table.setEnabled(false);

            JScrollPane sp = new JScrollPane(table);
            sp.setPreferredSize(new Dimension(520, 260));

            JOptionPane.showMessageDialog(this, sp, "Top 10 High Scores", JOptionPane.PLAIN_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            showPopup("High scores could not be loaded.");
        }
    }


    public static void showPopup(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // İlk map seçme ekranı açılır
    private void openGameWindow() throws IOException {
    	
        choose_Map(); 
    }
    
    private void choose_Map() {
        setContentPane(new MapSelectPanel());
        setSize(930, 1030);
        revalidate();
        repaint();
    }
    class MapSelectPanel extends JPanel {
        private BufferedImage bg;

        MapSelectPanel() {
            setLayout(new GridBagLayout());
            try {
                bg = ImageIO.read(getClass().getResource("/assets/menu_bg.png")); 
            } catch (Exception e) { bg = null; }

            JPanel row = new JPanel(new GridLayout(1, 3, 15, 10));
            row.setOpaque(false);

            row.add(makeMapCard(1));
            row.add(makeMapCard(2));
            row.add(makeMapCard(3));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.insets = new Insets(120, 40, 40, 40);
            add(row, gbc);

            JButton back = new JButton("Back");
            back.addActionListener(e -> showMainMenu());
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 20, 0);
            add(back, gbc);
        }

        private JPanel makeMapCard(int mapId) {
            JPanel card = new JPanel();
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("MAP " + mapId, SwingConstants.CENTER);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setForeground(Color.BLACK);
            title.setFont(new Font("SansSerif", Font.BOLD, 18));

            JLabel thumb = new JLabel();
            thumb.setAlignmentX(Component.CENTER_ALIGNMENT);
            try {
                BufferedImage t = ImageIO.read(getClass().getResource("/assets/map" + mapId + "/thumb.png"));
                ImageIcon ic = new ImageIcon(t.getScaledInstance(220, 140, Image.SCALE_SMOOTH));
                thumb.setIcon(ic);
            } catch (Exception ignored) {
                thumb.setText("(no thumb)");
                thumb.setForeground(Color.WHITE);
            }

            int hs = 0;
            try { hs = DatabaseController.getHighestScoreForMap(mapId); }
            catch (Exception ex) { ex.printStackTrace(); }

            JLabel high = new JLabel("High Score: " + hs);
            high.setAlignmentX(Component.CENTER_ALIGNMENT);
            high.setForeground(Color.BLACK);
            high.setFont(new Font("SansSerif", Font.BOLD, 14));

            JButton select = new JButton("Select");
            select.setAlignmentX(Component.CENTER_ALIGNMENT);
            select.addActionListener(e -> {
                try { mapChosen(mapId); }
                catch (IOException ex) { ex.printStackTrace(); }
            });

            card.add(title);
            card.add(Box.createVerticalStrut(8));
            card.add(thumb);
            card.add(Box.createVerticalStrut(8));
            card.add(high);
            card.add(Box.createVerticalStrut(8));
            card.add(select);

            return card;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        }
    }

    
    
    private void mapChosen(int map_number) throws IOException {
        currentMapId = map_number;

        final_Score = 0;
        money = 300;
        healthPercentage = 100;

        setMap(map_number);
        setTopBar();

        setSize(890, 930);
        setLocationRelativeTo(null);

        startGame();
    }
    
    
    private void schedulePlanes(int startMs, int endMs, int intervalMs) {
        if (intervalMs <= 0) return;

        int t = startMs + intervalMs;
        while (t < endMs) {
            int at = t;
            uiAt(at, () -> spawnPlaneRandom());
            t += intervalMs;
        }
    }

    private void spawnPlaneRandom() {
        if (currentMap == null) return;

        int rows = currentMap.getRows();
        int cols = currentMap.getCols();

        int side = rng.nextInt(4); 
        double sx, sy, ex, ey;

        if (side == 0) { 
            sx = -2.0; sy = rng.nextDouble() * rows;
            ex = cols + 2.0; ey = rng.nextDouble() * rows;
        } else if (side == 1) { 
            sx = cols + 2.0; sy = rng.nextDouble() * rows;
            ex = -2.0; ey = rng.nextDouble() * rows;
        } else if (side == 2) {
            sx = rng.nextDouble() * cols; sy = -2.0;
            ex = rng.nextDouble() * cols; ey = rows + 2.0;
        } else {
            sx = rng.nextDouble() * cols; sy = rows + 2.0;
            ex = rng.nextDouble() * cols; ey = -2.0;
        }

        double dx = ex - sx;
        double dy = ey - sy;
        double len = Math.sqrt(dx*dx + dy*dy);
        if (len < 1e-6) return;

        double speed = 6.0; 
        double vx = speed * (dx / len);
        double vy = speed * (dy / len);

        planes.add(new Plane(sx, sy, vx, vy));
    }


    private void updatePlanes(double dt) {
        if (currentMap == null) return;

        int cols = currentMap.getCols();
        int rows = currentMap.getRows();

        for (java.util.Iterator<Plane> it = planes.iterator(); it.hasNext();) {
            Plane p = it.next();
            p.update(dt);

            if (p.shouldDropBomb()) {
                dropBomb(p.tileX(), p.tileY());
            }

            if (p.isOutOfMap(cols, rows)) it.remove();
        }
    }


    private void dropBomb(int tx, int ty) {
        if (currentMap == null || mapPanel == null) return;
        if (!currentMap.inBounds(tx, ty)) return;

        mapPanel.addScorch(tx, ty, SCORCH_MS);
        damageTowerAt(tx, ty, PLANE_BOMB_DAMAGE);
    }

    private void damageTowerAt(int tx, int ty, int dmg) {
        for (java.util.Iterator<Tower> it = towers.iterator(); it.hasNext(); ) {
            Tower t = it.next();
            if (t.getX() == tx && t.getY() == ty) {
                
                t.takeDamage(dmg);
                if (t.isDestroyed()) it.remove();
                return;
            }
        }
    }




    private javax.swing.Timer gameTimer;
    private long lastTickNs;

    
    private void startGame() {
        gameEnded = false;

        enemies.clear();
        projectiles.clear();
        towers.clear();

        buildWaves(currentMap);

        spawnSchedule.sort(java.util.Comparator.comparingInt(s -> s.atMs));
        nextSpawnIndex = 0;

        uiSchedule.sort(java.util.Comparator.comparingInt(u -> u.atMs));
        nextUiIndex = 0;                                                

        gameStartMs = System.currentTimeMillis();


        lastNs = System.nanoTime();

        gameTimer = new Timer(16, e -> {
            long now = System.nanoTime();
            double dt = (now - lastNs) / 1_000_000_000.0;
            lastNs = now;

            int elapsedMs = (int)(System.currentTimeMillis() - gameStartMs);

            spawnDueEnemies(elapsedMs);     
            runDueUiEvents(elapsedMs);      

            for (Enemy en : enemies) en.update(dt);

            long nowMs = System.currentTimeMillis();
            for (Tower tw : towers) tw.tryShoot(enemies, projectiles, nowMs);

            for (int i = projectiles.size()-1; i >= 0; i--) {
                Projectile p = projectiles.get(i);
                p.update(dt);
                if (!p.alive) projectiles.remove(i);
            }
            
            updatePlanes(dt);
            if (mapPanel != null) mapPanel.tickEffects((int)Math.round(dt * 1000));


            cleanupEnemies();
            mapPanel.repaint();
            checkWinAndSave();
        });


        gameTimer.start();
    }

    
    private void checkWinAndSave() {
        if (gameEnded) return;

        boolean allSpawned = (nextSpawnIndex >= spawnSchedule.size());
        boolean noEnemiesAlive = enemies.isEmpty(); // cleanupEnemies zaten ölüleri siliyor

        if (allSpawned && noEnemiesAlive && healthPercentage > 0) {
            gameEnded = true;
            gameTimer.stop();

            try {
                DatabaseController.insertSCORE(player_Name, currentMapId, final_Score);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            showPopup("You Win! Score: " + final_Score);


        }
    }

    
    private void spawnAt(int atMs, java.util.function.Supplier<Enemy> factory) {
        spawnSchedule.add(new SpawnEvent(atMs, factory));
    }
    
    
    //wave mantığı yap 
    private void buildWaves(GameMap map) {
        spawnSchedule.clear();
        if(currentMapId==1) {
        	buildWaveformap1(map);
        };
        if(currentMapId==2) {
        	buildWaveformap2(map);
        };
        if(currentMapId==3) {
        	buildWaveformap3(map);
        };
    }
    private void buildWaveformap3(GameMap map) {
    	//Wave 1
    	showTextAt(0, "WAVE 1");
    	int timer = 3000;
    	int n = 0;
    	int randomizer = 0;
    	for(int i = 0 ;i<17;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 2000) + 1;  // 1..100
    		timer += n;
    	}
    	
    	
    	timer += 10000;
    	//Wave2
    	showTextAt(timer,"WAVE 2");
    	timer+=2000;
    	int w2 = timer;
    	for(int i = 0 ;i<25;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 1400) + 1;  // 1..100
    		timer += n;
    	}
    	spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));

    	timer += 10000;
    	//Wave3
    	showTextAt(timer,"WAVE 3");
    	timer+=2000;
    	int w3 = timer;
    	for(int i = 0 ;i<40;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <15) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <18) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 1000) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave4
    	showTextAt(timer,"WAVE 4");
    	timer+=2000;
    	int w4 = timer;
    	for(int i = 0 ;i<60;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <12) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <18) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 500) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave5
    	showTextAt(timer,"WAVE 5");
    	timer+=2000;
    	int w5 = timer;
    	schedulePlanes(w3, w5, 20000);
    	for(int i = 0 ;i<130;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <9) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <16) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 400) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave6
    	showTextAt(timer,"WAVE 6");
    	timer+=2000;
    	int w6 = timer;
    	schedulePlanes(w5, w6, 12000);
    	for(int i = 0 ;i<250;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <7) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <16) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 200) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave7
    	showTextAt(timer,"WAVE 7");
    	timer+=2000;
    	int w7 = timer;
    	for(int i = 0 ;i<420;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <3) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <15) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 180) + 1;  // 1..100
    		timer += n;
    	}
    	int wend = timer;
    	schedulePlanes(w6, wend, 8000);
    	
    	
    }
    private void buildWaveformap2(GameMap map) {
    	//Wave 1
    	showTextAt(0, "WAVE 1");
    	int timer = 3000;
    	int n = 0;
    	int randomizer = 0;
    	for(int i = 0 ;i<13;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 2000) + 1;  // 1..100
    		timer += n;
    	}
    	
    	
    	timer += 10000;
    	//Wave2
    	showTextAt(timer,"WAVE 2");
    	for(int i = 0 ;i<23;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 1400) + 1;  // 1..100
    		timer += n;
    	}
    	spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));

    	timer += 10000;
    	//Wave3
    	showTextAt(timer,"WAVE 3");
    	for(int i = 0 ;i<40;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <15) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <18) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 700) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave4
    	showTextAt(timer,"WAVE 4");
    	for(int i = 0 ;i<80;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <12) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <18) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 400) + 1;  // 1..100
    		timer += n;
    	}
    	timer += 10000;
    	//Wave5
    	showTextAt(timer,"WAVE 5");
    	for(int i = 0 ;i<130;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <9) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <16) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 400) + 1;  // 1..100
    		timer += n;
    	}
    	
    }
    private void buildWaveformap1(GameMap map) {
    	//Wave 1
    	showTextAt(0, "WAVE 1");
    	int timer = 3000;
    	int n = 0;
    	int randomizer = 0;
    	for(int i = 0 ;i<13;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 2000) + 1;  // 1..100
    		timer += n;
    	}
    	
    	
    	timer += 10000;
    	//Wave2
    	showTextAt(timer,"WAVE 2");
    	for(int i = 0 ;i<23;i++) {
    		spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 1400) + 1;  // 1..100
    		timer += n;
    	}
    	spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));

    	timer += 10000;
    	//Wave3
    	showTextAt(timer,"WAVE 3");
    	for(int i = 0 ;i<40;i++) {
    		randomizer = (int)(Math.random() * 20) + 1;
    		
    		if (randomizer <15) spawnAt(timer,  () -> new DefaultEnemy(map.getSpawn(), map));
    		else if (randomizer <18) spawnAt(timer,  () -> new GhostEnemy(map.getSpawn(), map));
    		else spawnAt(timer,  () -> new CyborgEnemy(map.getSpawn(), map));
    		n = (int)(Math.random() * 1000) + 1;  // 1..100
    		timer += n;
    	}
    }

    private void spawnDueEnemies(int elapsedMs) {
        while (nextSpawnIndex < spawnSchedule.size()
                && spawnSchedule.get(nextSpawnIndex).atMs <= elapsedMs) {

            enemies.add(spawnSchedule.get(nextSpawnIndex).factory.get());
            nextSpawnIndex++;
        }
    }





    
    
    private void drawHealthBar(Graphics2D g2, int x, int y, int w, int h, int hp, int maxHp) {
        if (maxHp <= 0) return;

        hp = Math.max(0, Math.min(hp, maxHp));

        g2.setColor(new Color(180, 0, 0));
        g2.fillRect(x, y, w, h);

        int fillW = (int) Math.round(w * (hp / (double) maxHp));
        g2.setColor(new Color(0, 180, 0));
        g2.fillRect(x, y, fillW, h);

        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, w, h);
    }



    
    private void setMap(int map_number) throws IOException {
        String filename = "map" + map_number + ".txt";
        currentMap = GameMap.loadFromFile(filename);
        mapPanel = new MapPanel(currentMap, map_number);
 // 
    }
    
    private void leaveGame() {
    	paused = false;
    	if (btnPause != null) btnPause.setText("PAUSE");

        int res = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to leave?\nYour score will be saved.",
                "Leave Game",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (res != JOptionPane.YES_OPTION) return;

        if (gameTimer != null) gameTimer.stop();

        gameEnded = true;

        // skoru kaydet (bitmemiş olsa bile)
        try {
            DatabaseController.insertSCORE(player_Name, currentMapId, final_Score);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // menüye dön
        showMainMenu();
    }

    //money health , towerlar , leave
    private void setTopBar() {

        root = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(40, 120, 60));
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // SOL: money + health  (ÖNCE TANIMLA)
        JPanel leftInfo = new JPanel();
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setOpaque(false);

        lblMoney = new JLabel();
        lblHealth = new JLabel();
        lblScore = new JLabel();
        lblMoney.setForeground(Color.WHITE);
        lblHealth.setForeground(Color.WHITE);
        lblScore.setForeground(Color.WHITE);

        lblMoney.setFont(lblMoney.getFont().deriveFont(Font.BOLD, 16f));
        lblHealth.setFont(lblHealth.getFont().deriveFont(Font.BOLD, 16f));
        lblScore.setFont(lblHealth.getFont().deriveFont(Font.BOLD, 16f));
        
        leftInfo.add(lblMoney);
        leftInfo.add(lblHealth);
        leftInfo.add(lblScore);
     // SAĞ: Leave
     // SAĞ: Leave + Pause
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        btnPause = new JButton("PAUSE");
        btnPause.setFocusPainted(false);
        btnPause.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnPause.addActionListener(e -> togglePause());

        JButton btnLeave = new JButton("LEAVE");
        btnLeave.setFocusPainted(false);
        btnLeave.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLeave.addActionListener(e -> leaveGame());

        rightPanel.add(btnPause);
        rightPanel.add(btnLeave);

        topBar.add(rightPanel, BorderLayout.EAST);


        
        
        refreshHud();   //ilk yazdırma


        // ORTA: kuleler
        JButton btnCheap = makeTowerButton("$100", "/assets/CheapTower.png");
        JButton btn400   = makeTowerButton("$400", "/assets/Tower400.png");
        JButton btn800   = makeTowerButton("$800", "/assets/Tower800.png");
        JButton btn1200   = makeTowerButton("$1200", "/assets/Tower1200.png");
        JButton btnSell  = makeTowerButton("Sell", "/assets/btn_sell.png");
        
        //action listenerlar
        btnCheap.addActionListener(e -> { selectedTowerType = TowerType.CHEAP; sellMode = false; mapPanel.repaint(); });
        btn400.addActionListener(e -> { selectedTowerType = TowerType.T400;  sellMode = false; mapPanel.repaint(); });
        btn800.addActionListener(e -> { selectedTowerType = TowerType.T800;  sellMode = false; mapPanel.repaint(); });
        btn1200.addActionListener(e -> { selectedTowerType = TowerType.T1200;  sellMode = false; mapPanel.repaint(); });
        
        btnSell.addActionListener(e -> { selectedTowerType = null; sellMode = true; mapPanel.repaint(); });

        
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(btnCheap);
        centerPanel.add(btn400);
        centerPanel.add(btn800);
        centerPanel.add(btn1200);
        centerPanel.add(btnSell);


        topBar.add(leftInfo, BorderLayout.WEST);
        topBar.add(centerPanel, BorderLayout.CENTER);

        root.add(topBar, BorderLayout.NORTH);
        root.add(mapPanel, BorderLayout.CENTER);

        setContentPane(root);
        revalidate();
        repaint();
    }

    //money ve healt i yeniler her money veya health değişince çağır 
    private void refreshHud() {
        if (lblMoney != null)  lblMoney.setText("Money: $" + money);
        if (lblHealth != null) lblHealth.setText("Health: " + healthPercentage + "%");
        if (lblScore != null) lblScore.setText("Score: " + final_Score );
    }
    

    private static final int LEAK_DAMAGE = 10; // 1 enemy kaç % götürsün
    
    //ölüler çizilmez , end e reachleyenler kalkar can düşer
    private void cleanupEnemies() {
        boolean hudChanged = false;

        for (java.util.Iterator<Enemy> it = enemies.iterator(); it.hasNext(); ) {
            Enemy e = it.next();

            if (e.isDead()) {
                it.remove();
                money += e.getKillReward();
                final_Score += 20;
                hudChanged = true;
            } 
            else if (e.hasReachedEnd()) {
                it.remove();                 
                healthPercentage -= LEAK_DAMAGE; 
                if (healthPercentage < 0) healthPercentage = 0;
                hudChanged = true;
            }
        }

        if (hudChanged) {
            refreshHud();
            if (healthPercentage <= 0) {
                gameTimer.stop();
                showPopup("Game Over");
            }
        }
    }



    
    

    private JButton makeTowerButton(String priceText, String iconPath) { //örn 100, "/assets/1_1.png"
        ImageIcon icon = new ImageIcon(getClass().getResource(iconPath));

        Image scaled = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        icon = new ImageIcon(scaled);

        JButton b = new JButton(priceText, icon);
        b.setPreferredSize(new Dimension(120, 55));

        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);

        return b;
    }


    
    class MapPanel extends JPanel {

    	//enemyler için 
    	private final java.util.HashMap<String, BufferedImage> spriteCache = new java.util.HashMap<>();

    	private BufferedImage loadSprite(String path) {
    	        if (spriteCache.containsKey(path)) return spriteCache.get(path);
    	        try {
    	            BufferedImage img = ImageIO.read(getClass().getResource(path));
    	            spriteCache.put(path, img);
    	            return img;
    	        } catch (Exception e) {
    	            e.printStackTrace();
    	            spriteCache.put(path, null);
    	            return null;
    	        }
    	    }
    	
    	private int[][] scorchMs; // kalan ms uçaklar

    	private int[][] groundVariant;
    	
    	private int hoverTx = -1, hoverTy = -1;
    	private boolean hoverInBounds = false;
    	
    	private BufferedImage img0, img1, img2, img3, img4;
    	private BufferedImage img0a, img0b; // 1_0_1 ve 1_0_2

    	private BufferedImage cheapTowerImg, tower400Img,tower800Img,tower1200Img;
    	private BufferedImage planeImg;
    	private BufferedImage scorchImg;

        private final GameMap map;
        private final int tileSize = 80;
        
        
        private String bannerText = null;
        private javax.swing.Timer bannerTimer = null;
        
        public void showBanner(String text, int ms) {
            bannerText = text;

            if (bannerTimer != null && bannerTimer.isRunning()) {
                bannerTimer.stop();
            }

            bannerTimer = new javax.swing.Timer(ms, e -> {
                bannerText = null;
                repaint();
            });
            bannerTimer.setRepeats(false);
            bannerTimer.start();

            repaint();
        }
        private void drawBanner(Graphics2D g2) {
            if (bannerText == null || bannerText.isEmpty()) return;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Font font = new Font("SansSerif", Font.BOLD, 48);
            GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), bannerText);
            Rectangle vb = gv.getVisualBounds().getBounds();

            int padX = 40, padY = 22;
            int rectW = vb.width + padX * 2;
            int rectH = vb.height + padY * 2;

            int x = (getWidth() - rectW) / 2;
            int y = (getHeight() - rectH) / 2;

            // mavi kutu
            g2.setColor(new Color(0, 150, 255, 200));
            g2.fillRoundRect(x, y, rectW, rectH, 18, 18);

            g2.setStroke(new BasicStroke(4f));
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawRoundRect(x, y, rectW, rectH, 18, 18);


            int textX = x + padX - vb.x;
            int textY = y + padY - vb.y;

            Shape outline = gv.getOutline(textX, textY);

            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0, 0, 0, 200));
            g2.draw(outline);

            g2.setColor(Color.WHITE);
            g2.fill(outline);
        }


        
        public MapPanel(GameMap map,int mapId) {
        	
        	scorchMs = new int[map.getRows()][map.getCols()];

            this.map = map;
            setPreferredSize(new Dimension(map.getCols() * tileSize, map.getRows() * tileSize));
            
            //assetler için 
            try {
            	//tile imageleri
            	String base = "/assets/map" + mapId + "/";

            	img0  = ImageIO.read(getClass().getResource(base + "1_0_0.png"));
            	img0a = ImageIO.read(getClass().getResource(base + "1_0_1.png"));
            	img0b = ImageIO.read(getClass().getResource(base + "1_0_2.png"));

            	img1 = ImageIO.read(getClass().getResource(base + "1_1.png"));
            	img2 = ImageIO.read(getClass().getResource(base + "1_2.png"));
            	img3 = ImageIO.read(getClass().getResource(base + "1_1.png")); // varsa
            	img4 = ImageIO.read(getClass().getResource(base + "1_1.png")); // varsa

                
                //tower imageleri
                cheapTowerImg = ImageIO.read(getClass().getResource("/assets/CheapTower.png"));
                tower400Img   = ImageIO.read(getClass().getResource("/assets/Tower400.png"));
                tower800Img   = ImageIO.read(getClass().getResource("/assets/Tower800.png"));
                tower1200Img   = ImageIO.read(getClass().getResource("/assets/Tower1200.png"));
                planeImg = ImageIO.read(getClass().getResource("/assets/Plane.png"));
                scorchImg = ImageIO.read(getClass().getResource("/assets/BombDamage.png"));

            } catch (Exception e) {
                e.printStackTrace();
            }
            
            groundVariant = new int[map.getRows()][map.getCols()];
            java.util.Random rnd = new java.util.Random(12345); // sabit seed = aynı map aynı görünür

            for (int y = 0; y < map.getRows(); y++) {
                for (int x = 0; x < map.getCols(); x++) {
                    if (map.getTile(x, y) == 0) {
                        int n = rnd.nextInt(100) + 1;
                        groundVariant[y][x] = (n > 80) ? 1 : (n > 70) ? 2 : 0;
                    }
                }
            }
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverTx = e.getX() / tileSize;
                    hoverTy = e.getY() / tileSize;
                    hoverInBounds = map.inBounds(hoverTx, hoverTy);
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    hoverInBounds = false;
                    repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    int tx = e.getX() / tileSize;
                    int ty = e.getY() / tileSize;
                    if (!map.inBounds(tx, ty)) return;

                    // SELL
                    if (sellMode) {
                        Tower t = getTowerAt(tx, ty);
                        if (t != null) {
                            towers.remove(t);
                            money += t.getPrice()/2; //%50 refund
                            refreshHud();
                            repaint();
                        }
                        return;
                    }

                 // PLACE
                    if (selectedTowerType == null) return;
                    if (!canPlaceTower(tx, ty)) return;

                    Tower newT = null;
                    if (selectedTowerType == TowerType.CHEAP) newT = new CheapTower(tx, ty);
                    else if (selectedTowerType == TowerType.T400) newT = new Tower400(tx, ty);
                    else if (selectedTowerType == TowerType.T800) newT = new Tower800(tx, ty);
                    else if (selectedTowerType == TowerType.T1200) newT = new Tower1200(tx, ty);

                    if (newT == null) return;

                    if (money < newT.getPrice()) { showPopup("Not enough money"); return; }

                    towers.add(newT);
                    money -= newT.getPrice();
                    final_Score += newT.getPrice();
                    refreshHud();
                    repaint();
                    selectedTowerType=null;



                }
            });

        }
        

        public void tickEffects(int dtMs) {
            for (int y = 0; y < map.getRows(); y++) {
                for (int x = 0; x < map.getCols(); x++) {
                    if (scorchMs[y][x] > 0) scorchMs[y][x] = Math.max(0, scorchMs[y][x] - dtMs);
                }
            }
        }

        private void drawScorch(Graphics2D g2) {
            for (int y = 0; y < map.getRows(); y++) {
                for (int x = 0; x < map.getCols(); x++) {
                    int ms = scorchMs[y][x];
                    if (ms <= 0) continue;

                    int px = x * tileSize;
                    int py = y * tileSize;

                
                    int alpha = Math.min(160, 40 + ms / 40);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillOval(px + 10, py + 10, tileSize - 20, tileSize - 20);
                }
            }
        }

        private void drawPlanes(Graphics2D g2) {
            for (Plane p : planes) {
                int px = (int)Math.round(p.xTile * tileSize);
                int py = (int)Math.round(p.yTile * tileSize);

                int w = (int)(tileSize * 1.2);
                int h = (int)(tileSize * 0.7);

                if (planeImg != null) {
                    var old = g2.getTransform();

                    double cx = px + w / 2.0;
                    double cy = py + h / 2.0;

                    g2.translate(cx, cy);
                    g2.rotate(p.angleRad()); 
                    g2.drawImage(planeImg, -w/2, -h/2, w, h, null);

                    g2.setTransform(old);
                } else {
                    g2.setColor(new Color(230,230,230,220));
                    g2.fillRoundRect(px, py, w, h, 12, 12);
                }
            }
        }



        
        private void drawRotated(Graphics2D g2, BufferedImage img, int px, int py, int size, double angleRad) {
            if (img == null) return;

            var old = g2.getTransform();

            double cx = px + size / 2.0;
            double cy = py + size / 2.0;

            g2.translate(cx, cy);
            g2.rotate(angleRad);
            g2.drawImage(img, -size/2, -size/2, size, size, null);

            g2.setTransform(old);
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            //health bar için
            Graphics2D g2 = (Graphics2D) g;


            for (int y = 0; y < map.getRows(); y++) {
                for (int x = 0; x < map.getCols(); x++) {
                    int t = map.getTile(x, y);
                    
                    BufferedImage img = null;
                    switch (t) {
                    //boş alan için randomluk ekle
	                    case 0: {
	                        int v = groundVariant[y][x];          // 0/1/2
	                        img = (v == 1) ? img0a : (v == 2) ? img0b : img0;
	                        break;
	                    }

                        
                        
                        
                        case 1: img = img1; break;
                        case 2: img = img2; break;
                        case 3: img = img3; break;
                        case 4: img = img4; break;
                    }

                    int px = x * tileSize;
                    int py = y * tileSize;

                    if (img != null) {
                        g.drawImage(img, px, py, tileSize, tileSize, null);
                    } else {
                        // fallback: resim yoksa gri bas
                        g.setColor(Color.LIGHT_GRAY);
                        g.fillRect(px, py, tileSize, tileSize);
                    }
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(px, py, tileSize, tileSize);
                    
            }
        }
            drawScorches(g2);  
            drawPlanes(g2);

            for (Tower t : towers) {
                int pxT = t.getX() * tileSize;
                int pyT = t.getY() * tileSize;

                BufferedImage ti = null;
                if (t instanceof CheapTower) ti = cheapTowerImg;
                else if (t instanceof Tower400) ti = tower400Img;
                else if (t instanceof Tower800) ti = tower800Img;
                else if (t instanceof Tower1200) ti = tower1200Img;

                if (ti != null) {
                    // tower sprite "yukarı bakıyor" -> atan2 0=sağ olduğundan +90° offset
                    double ang = t.getFacingRad() + Math.PI / 2.0;
                    drawRotated(g2, ti, pxT, pyT, tileSize, ang);
                    	// TOWER HEALTH BAR (HP max değilse)
                    int thp = t.getHealth();       
                    int tmax = t.getMaxHealth();   

                    if (tmax > 0 && thp > 0 && thp < tmax) {
                        int barW = (int)(tileSize * 0.80);
                        int barH = 6;
                        int barX = pxT + (tileSize - barW) / 2;
                        int barY = pyT - 10; // üstünde dursun

                        drawHealthBar(g2, barX, barY, barW, barH, thp, tmax);
                    }

                }
            }
            for (Enemy e : enemies) {
                BufferedImage img = loadSprite(e.getSpritePath());
                if (img != null) {
                    int px = (int) Math.round(e.getX() * tileSize);
                    int py = (int) Math.round(e.getY() * tileSize);

                    drawRotated(g2, img, px, py, tileSize, e.getFacingRad());

                    // HEALTH BAR
                    if (e.showHealth() && !e.isDead()) {
                        int barW = (int) (tileSize * 0.80);
                        int barH = 6;
                        int barX = px + (tileSize - barW) / 2;
                        int barY = py + tileSize + 2;

                        drawHealthBar(g2, barX, barY, barW, barH, e.getHealth(), e.getMaxHealth());
                    }
                }
            }

            g.setColor(Color.YELLOW);
            for (Projectile p : projectiles) {
                int px = (int)(p.x * tileSize);
                int py = (int)(p.y * tileSize);
                g.fillOval(px - 3, py - 3, 6, 6);
            }
            drawPlacementPreview(g2);
            drawBanner(g2);


            



    }

        private Tower getTowerAt(int x, int y) {
            for (Tower t : towers) {
                if (t.getX() == x && t.getY() == y) return t;
            }
            return null;
        }
        private BufferedImage getGhostImageForSelectedTower() {
            if (selectedTowerType == null) return null;

            switch (selectedTowerType) {
                case CHEAP:  return cheapTowerImg;
                case T400:   return tower400Img;
                case T800:   return tower800Img;
                case T1200:  return tower1200Img;
                default:     return null;
            }
        }

        private boolean canPlacePreview(int tx, int ty) {
            if (!map.inBounds(tx, ty)) return false;
            if (selectedTowerType == null) return false;
            if (money < selectedTowerType.cost) return false;
            return canPlaceTower(tx, ty); // buildable + boş mu 
        }

        private void drawPlacementPreview(Graphics2D g2) {
            if (sellMode) return;                 
            if (selectedTowerType == null) return;
            if (!hoverInBounds) return;

            int tx = hoverTx, ty = hoverTy;
            int px = tx * tileSize;
            int py = ty * tileSize;

            boolean ok = canPlacePreview(tx, ty);


            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // RANGE CIRCLE
            int cx = px + tileSize / 2;
            int cy = py + tileSize / 2;
            int r  = selectedTowerType.rangeTiles * tileSize;

            Color fill   = ok ? new Color(0, 255, 0, 55) : new Color(255, 0, 0, 55);
            Color stroke = ok ? new Color(0, 255, 0, 160) : new Color(255, 0, 0, 160);

            g2.setColor(fill);
            g2.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            g2.setColor(stroke);
            g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);

            BufferedImage ghostImg = getGhostImageForSelectedTower();
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));

            if (ghostImg != null) {
                g2.drawImage(ghostImg, px, py, tileSize, tileSize, null);
            } else {
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillRect(px, py, tileSize, tileSize);
            }
            g2.setComposite(oldComp);

            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(ok ? new Color(0, 255, 0, 220) : new Color(255, 0, 0, 220));
            g2.drawRect(px + 2, py + 2, tileSize - 4, tileSize - 4);
            g2.setStroke(oldStroke);
        }

        

        private boolean canPlaceTower(int x, int y) {
            return map.isBuildable(x, y) && getTowerAt(x, y) == null;
        }

    
    private static class Scorch {
        final int tx, ty;
        final long untilMs;
        final float alpha;

        Scorch(int tx, int ty, long untilMs, float alpha) {
            this.tx = tx; this.ty = ty; this.untilMs = untilMs;
            this.alpha = alpha;
        }
    }

    private final ArrayList<Scorch> scorches = new ArrayList<>();

    public void addScorch(int txt, int tyt, int mss) {
        float a = 0.65f + (float)Math.random() * 0.25f; 
        scorches.add(new Scorch(txt, tyt, System.currentTimeMillis() + mss, a));
    }

    private void drawScorches(Graphics2D g2) {
        long now = System.currentTimeMillis();

        for (java.util.Iterator<Scorch> it = scorches.iterator(); it.hasNext();) {
            Scorch s = it.next();
            if (now > s.untilMs) { it.remove(); continue; }
            int tileSize = 80;
            int px = s.tx * tileSize;
            int py = s.ty * tileSize;

            int w = (int)(tileSize * 1.10);
            int h = (int)(tileSize * 1.10);

            Composite oldC = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s.alpha));


            int dx = px + (tileSize - w) / 2;
            int dy = py + (tileSize - h) / 2;
            g2.drawImage(scorchImg, dx, dy, w, h, null);


            g2.setComposite(oldC);
        }
    }

    }
    
}


   