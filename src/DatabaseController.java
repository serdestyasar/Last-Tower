import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
public class DatabaseController {

	public static final String FILENAME = "logs.txt";

	
	
    private static String logsPath() {
        return System.getProperty("user.dir") + File.separator + FILENAME;
    }
    
    
    //dosya yoksa oluşturur. 1 döndürür
    public static int ensureLogsFile() throws IOException { 
           File f = new File(logsPath());
           
           if (f.exists()) {
           	return 1;
           }
           f.createNewFile();
           
           return  1;
    }
    
    //Skor kaydeder -> format : S/name/score/
 // Yeni format: S/mapId/name/score/
    public static void insertSCORE(String name, int mapId, int score) throws IOException {
        ensureLogsFile();

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logsPath(), true), StandardCharsets.UTF_8))) {
        	w.newLine();
            w.write("S/" + mapId + "/" + name + "/" + score + "/");
            w.newLine();
        }
    }

    
    
    //Hesap kaydeder -> format : "A/name/password/"
    public static void insertACC(String name, String password) throws IOException {
        ensureLogsFile(); 

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logsPath(), true), StandardCharsets.UTF_8))) {

            w.write("A/" +name + "/" + password +"/");  
            w.newLine();
        }
    }
    public static int getHighestScoreForMap(int mapId) throws IOException {
        ensureLogsFile();
        int best = 0;

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(logsPath()), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("S/")) continue;

                String[] parts = line.split("/", -1);

                // Yeni format: S/mapId/name/score/
                if (parts.length >= 4 && "S".equals(parts[0])) {
                    Integer mid = tryParseInt(parts[1]);
                    Integer sc  = tryParseInt(parts[3]);
                    if (mid != null && sc != null && mid == mapId) {
                        if (sc > best) best = sc;
                    }
                }
                // Eski format: S/name/score/  (map bilinmiyor -> istersen map1 say)
                else if (parts.length >= 3 && "S".equals(parts[0])) {
                    Integer sc = tryParseInt(parts[2]);
                    if (sc != null && mapId == 1) { // eski kayıtları map1'e yaz
                        if (sc > best) best = sc;
                    }
                }
            }
        }
        return best;
    }

    private static Integer tryParseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    
    //tüm hesapların [name][password] matrisi döndürür
    public static String[][] returnACC() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> passwords = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(logsPath()), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("A/")) continue;

                // Format: A/name/password/
                String[] parts = line.split("/", -1); 
                if (parts.length >= 3 && "A".equals(parts[0])) {
                    names.add(parts[1]);
                    passwords.add(parts[2]);
                }
            }
        }

        String[][] acc = new String[2][names.size()];
        for (int i = 0; i < names.size(); i++) {
            acc[0][i] = names.get(i);      // 0. index -> name
            acc[1][i] = passwords.get(i);  // 1. index -> password
        }
        return acc;
    }
    
  //tüm skorların [name][score] matrisi döndürür
    public static String[][] returnSCORE() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> scores = new ArrayList<>();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(logsPath()), StandardCharsets.UTF_8))) {

            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("S/")) continue;

                // Format: A/name/password/
                String[] parts = line.split("/", -1); 
                if (parts.length >= 3 && "S".equals(parts[0])) {
                    names.add(parts[1]);
                    scores.add(parts[2]);
                }
            }
        }

        String[][] acc = new String[2][names.size()];
        for (int i = 0; i < names.size(); i++) {
            acc[0][i] = names.get(i);      // 0. index -> name
            acc[1][i] = scores.get(i);  // 1. index -> password
        }
        return acc;
    }  



    public static String[][] returnSCORES() throws IOException {
    	ArrayList<String> names  = new ArrayList<>();
        ArrayList<String> maps   = new ArrayList<>();
        ArrayList<String> scores = new ArrayList<>();

        File f = new File("logs.txt"); 
        if (!f.exists()) {
            return new String[][]{ new String[0], new String[0], new String[0] };
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // S/1/deniz/280/  sadece score satırları
                if (!line.startsWith("S/")) continue;

                String[] p = line.split("/"); // trailing "/"
                // p = ["S","1","deniz","280"]
                if (p.length < 4) continue;

                String mapId  = p[1];
                String name   = p[2];
                String score  = p[3];

                // güvenlik
                if (mapId.isEmpty() || name.isEmpty() || score.isEmpty()) continue;

                names.add(name);
                maps.add(mapId);
                scores.add(score);
            }
        }

        return new String[][]{
            names.toArray(new String[0]),
            maps.toArray(new String[0]),
            scores.toArray(new String[0])
        };
    }
    	// name,password bir hesaba ait ise 1 , değilse 0 döner;
    public static boolean accExits(String name,String password) throws IOException {
    	String[][] accounts = returnACC();
    	
    	if (accounts==null) return false;
    	
        for (int i = 0; i < accounts[0].length; i++) {
            if (name.equals(accounts[0][i]) && password.equals(accounts[1][i])) {
                return true;
            }
        }
        return false;
    	
    }
    
    public static void main(String[] args) throws Exception {
    
    	System.out.println(1);
    	return;
    }

}


