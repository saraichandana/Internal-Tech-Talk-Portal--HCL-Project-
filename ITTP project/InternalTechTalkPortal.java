import com.mongodb.client.*;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

import java.time.LocalDate;
import java.util.*;

public class InternalTechTalkPortal {

    // MongoDB connection details
    private static final String URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "ittpdb";
    private static final String COLLECTION_NAME = "techtalks";
    private static MongoCollection<Document> collection;

    // In-memory storage
    private static List<TechTalk> techTalkList = new ArrayList<>();

    // Inner class for TechTalk structure
    public static class TechTalk {
        private String title;
        private String description;
        private String postedBy;
        private String date;
        private List<String> tags;

        public TechTalk(String title, String description, String postedBy, String date, List<String> tags) {
            this.title = title;
            this.description = description;
            this.postedBy = postedBy;
            this.date = date;
            this.tags = tags != null ? tags : new ArrayList<>();
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getPostedBy() { return postedBy; }
        public String getDate() { return date; }
        public List<String> getTags() { return tags; }

        public void setDescription(String description) { this.description = description; }
        public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
        public void setDate(String date) { this.date = date; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create(URI);
             Scanner sc = new Scanner(System.in)) {

            MongoDatabase database = mongoClient.getDatabase(DB_NAME);
            collection = database.getCollection(COLLECTION_NAME);
            System.out.println("Connected to MongoDB successfully!");
            loadDataFromDB();
            runMenu(sc);

        } catch (Exception e) {
            System.out.println("MongoDB connection failed: " + e.getMessage());
        }
    }

    private static void loadDataFromDB() {
        techTalkList.clear();
        for (Document doc : collection.find()) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) doc.get("tags", List.class);
            TechTalk talk = new TechTalk(
                    doc.getString("title"),
                    doc.getString("description"),
                    doc.getString("postedBy"),
                    doc.getString("date"),
                    tags
            );
            techTalkList.add(talk);
        }
        System.out.println("Loaded " + techTalkList.size() + " tech talks into memory.");
    }

    private static void runMenu(Scanner sc) {
        while (true) {
            System.out.println("\n===== INTERNAL TECH TALK PORTAL =====");
            System.out.println("1. Add Tech Talk");
            System.out.println("2. View All Tech Talks");
            System.out.println("3. Search by Title");
            System.out.println("4. Search by Tag");
            System.out.println("5. Search by Posted By");
            System.out.println("6. Update Tech Talk");
            System.out.println("7. Delete Tech Talk");
            System.out.println("8. Sort Tech Talks by Date");
            System.out.println("9. Exit");
            System.out.print("Enter your choice: ");

            String input = sc.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter number 1-9.");
                continue;
            }

            switch (choice) {
                case 1 -> addTechTalk(sc);
                case 2 -> viewAll();
                case 3 -> searchByTitle(sc);
                case 4 -> searchByTag(sc);
                case 5 -> searchByPostedBy(sc);
                case 6 -> updateTechTalk(sc);
                case 7 -> deleteByTitle(sc);
                case 8 -> sortByDate(sc);
                case 9 -> {
                    System.out.println("Exiting portal...");
                    return;
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    private static void addTechTalk(Scanner sc) {
        System.out.print("Enter title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("Title required!");
            return;
        }
        if (collection.find(eq("title", title)).first() != null) {
            System.out.println("Tech Talk with this title already exists!");
            return;
        }

        System.out.print("Enter description: ");
        String desc = sc.nextLine().trim();
        System.out.print("Enter posted by: ");
        String postedBy = sc.nextLine().trim();
        System.out.print("Enter tags (comma separated): ");
        String[] tagsArray = sc.nextLine().split(",");
        List<String> tags = new ArrayList<>();
        for (String t : tagsArray) if (!t.trim().isEmpty()) tags.add(t.trim());

        String today = LocalDate.now().toString();
        TechTalk talk = new TechTalk(title, desc, postedBy, today, tags);
        techTalkList.add(talk);

        Document doc = new Document("title", title)
                .append("description", desc)
                .append("postedBy", postedBy)
                .append("tags", tags)
                .append("date", today);

        collection.insertOne(doc);
        System.out.println("Tech Talk added successfully!");
    }

    private static void viewAll() {
        if (techTalkList.isEmpty()) {
            System.out.println("No tech talks available!");
            return;
        }
        System.out.println("\nAll Tech Talks:");
        techTalkList.forEach(InternalTechTalkPortal::printTalk);
    }

    private static void searchByTitle(Scanner sc) {
        System.out.print("Enter title to search: ");
        String title = sc.nextLine().trim();
        techTalkList.stream()
                .filter(t -> t.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .ifPresentOrElse(
                        InternalTechTalkPortal::printTalk,
                        () -> System.out.println("Not found in portal.")
                );
    }

    private static void searchByTag(Scanner sc) {
        System.out.print("Enter tag to search: ");
        String tag = sc.nextLine().trim().toLowerCase();
        boolean found = false;
        for (TechTalk talk : techTalkList) {
            for (String t : talk.getTags()) {
                if (t.equalsIgnoreCase(tag)) {
                    printTalk(talk);
                    found = true;
                    break;
                }
            }
        }
        if (!found) System.out.println("No talks found with this tag.");
    }

    private static void searchByPostedBy(Scanner sc) {
        System.out.print("Enter author name: ");
        String postedBy = sc.nextLine().trim();
        boolean found = false;
        for (TechTalk talk : techTalkList) {
            if (talk.getPostedBy().equalsIgnoreCase(postedBy)) {
                printTalk(talk);
                found = true;
            }
        }
        if (!found) System.out.println("No talks found by this author.");
    }

    private static void updateTechTalk(Scanner sc) {
        System.out.print("Enter title to update: ");
        String title = sc.nextLine().trim();
        boolean updated = false;

        for (TechTalk talk : techTalkList) {
            if (talk.getTitle().equalsIgnoreCase(title)) {
                System.out.print("New description (leave blank to skip): ");
                String desc = sc.nextLine().trim();
                if (!desc.isEmpty()) talk.setDescription(desc);

                System.out.print("New posted by (leave blank to skip): ");
                String postedBy = sc.nextLine().trim();
                if (!postedBy.isEmpty()) talk.setPostedBy(postedBy);

                System.out.print("New tags (comma separated, leave blank to skip): ");
                String tagsInput = sc.nextLine().trim();
                if (!tagsInput.isEmpty()) {
                    List<String> tags = new ArrayList<>();
                    for (String t : tagsInput.split(",")) if (!t.trim().isEmpty()) tags.add(t.trim());
                    talk.setTags(tags);
                }

                String today = LocalDate.now().toString();
                talk.setDate(today);

                Document updatedDoc = new Document("description", talk.getDescription())
                        .append("postedBy", talk.getPostedBy())
                        .append("tags", talk.getTags())
                        .append("date", talk.getDate());

                collection.updateOne(eq("title", title), new Document("$set", updatedDoc));
                System.out.println("Tech Talk updated successfully!");
                updated = true;
                break;
            }
        }

        if (!updated) System.out.println("Tech Talk not found.");
    }

    private static void deleteByTitle(Scanner sc) {
        System.out.print("Enter title to delete: ");
        String title = sc.nextLine().trim();

        boolean removed = techTalkList.removeIf(t -> t.getTitle().equalsIgnoreCase(title));
        collection.deleteOne(eq("title", title));

        System.out.println(removed ? "Tech Talk deleted." : "Tech Talk not found.");
    }

    private static void sortByDate(Scanner sc) {
        System.out.println("1. Newest to Oldest");
        System.out.println("2. Oldest to Newest");
        System.out.print("Choose option: ");
        String choice = sc.nextLine();

        Comparator<TechTalk> comparator = Comparator.comparing(t -> LocalDate.parse(t.getDate()));
        if (choice.equals("1")) techTalkList.sort(comparator.reversed());
        else if (choice.equals("2")) techTalkList.sort(comparator);
        else {
            System.out.println("Invalid choice!");
            return;
        }

        System.out.println("\nSorted Tech Talks:");
        techTalkList.forEach(InternalTechTalkPortal::printTalk);
    }

    private static void printTalk(TechTalk talk) {
        System.out.println("---------------------------------------------------");
        System.out.println("Title: " + talk.getTitle());
        System.out.println("Description: " + talk.getDescription());
        System.out.println("Posted By: " + talk.getPostedBy());
        System.out.println("Date: " + talk.getDate());
        System.out.println("Tags: " + talk.getTags());
    }
}
