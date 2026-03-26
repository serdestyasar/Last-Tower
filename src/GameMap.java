import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class GameMap {
    private final int[][] grid;
    private final int rows;
    private final int cols;
    private int[] spawn = new int[] {-1, -1};
    private int[] end = new int[] {-1, -1};;
    
    // 1 1 1
    // 1 0 0
    // 1 0 2  - 0 empty , 1 path , 2 buildable , 3 spawn , 4 end 
    public GameMap(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            throw new IllegalArgumentException("Matrix boş olamaz.");
        }

        this.rows = matrix.length;
        this.cols = matrix[0].length;

        // kare map zorunlu
        if (cols != rows) {
            throw new IllegalArgumentException("Matrix kare olmalı.");
        }

        this.grid = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                this.grid[r][c] = matrix[r][c]; // grid[row][col]

                if (this.grid[r][c] == 3) { // spawn
                    this.spawn[0] = c; // x = col
                    this.spawn[1] = r; // y = row
                }

                if (this.grid[r][c] == 4) { // end
                    this.end[0] = c; // x
                    this.end[1] = r; // y
                }
            }
        }
    }

   

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}
    
    
    public int[] getSpawn() {
		return spawn;
	}


	public void setSpawn(int[] spawn) {
		this.spawn = spawn;
	}


	public int getTile(int x, int y) {
	    return grid[y][x];   //
	}

    
    public boolean isBuildable(int x,int y) {
    	return (getTile(x,y)==2);
    }
    
    public boolean isEnd(int x, int y) {
    	return (getTile(x,y)==4);
    }
    
    public boolean isPath(int x, int y) {
    	return (getTile(x,y)==1);
    }
    public boolean inBounds(int x, int y) {
        return (x >= 0 && x < cols && y >= 0 && y < rows);
    }
    
    public static GameMap loadFromFile(String filename) throws IOException {
        ArrayList<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        }

        if (lines.isEmpty()) throw new IllegalArgumentException("Map dosyası boş.");

        int rows = lines.size();
        int cols = lines.get(0).length();

        for (String s : lines) {
            if (s.length() != cols) throw new IllegalArgumentException("Satır uzunlukları eşit değil.");
        }

        int[][] matrix = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            String s = lines.get(y);
            for (int x = 0; x < cols; x++) {
                matrix[y][x] = s.charAt(x) - '0';
            }
        }

        return new GameMap(matrix);
    }

    
}