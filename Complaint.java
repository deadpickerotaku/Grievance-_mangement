public class Complaint {

    private static int counter = 0;

    private int id;
    private String user;
    private String text;
    private String category;
    private String status;
    private String adminNote;   // ✅ required

    public Complaint(String user, String text, String category) {
        this.id = ++counter;
        this.user = user;
        this.text = text;
        this.category = category;
        this.status = "Pending";
        this.adminNote = "";   // ✅ initialize
    }

    public int getId() { return id; }
    public String getUser() { return user; }
    public String getText() { return text; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
    }

    // ✅ ADD THESE (this is what your error needs)
    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String note) {
        this.adminNote = note;
    }
}
