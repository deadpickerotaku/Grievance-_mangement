import java.io.*;
import java.util.*;

public class FileDatabase {

    private static final String FILE_NAME = "complaints.txt";

    public static void saveAll(List<Complaint> list) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME, false))) {
            for (Complaint c : list) {
                bw.write(c.getId() + "|" +
                         c.getUser() + "|" +
                         c.getCategory() + "|" +
                         c.getText() + "|" +
                         c.getStatus());
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Complaint> loadAll() {
        List<Complaint> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {

                String[] parts = line.split("\\|");
                if (parts.length < 5) continue;

                Complaint c = new Complaint(parts[1], parts[3], parts[2]);
                c.setStatus(parts[4]);

                list.add(c);
            }

        } catch (Exception e) {
            // ignore
        }

        return list;
    }
}
