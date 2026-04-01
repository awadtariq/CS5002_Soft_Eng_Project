import java.io.*;
import java.util.*;

/**
 * UserManager — Handles all user credential storage and retrieval.
 * Credentials are saved to and loaded from users.txt
 *
 * File format (one user per line):
 *   username:password:role
 *
 * Roles: "admin" or "user"
 *
 * Usage:
 *   UserManager.loadUsers();
 *   boolean ok   = UserManager.validateLogin("alice", "alice123");
 *   boolean saved = UserManager.registerUser("newuser", "pass123", "user");
 *   boolean admin = UserManager.isAdmin("admin");
 */
public class UserManager {

    // Path to the credentials file — saved alongside the project
    private static final String USERS_FILE = "users.txt";

    // In-memory store: username -> { password, role }
    private static final Map<String, String[]> users = new HashMap<>();

    // ─── Load ────────────────────────────────────────────────────────────────────
    /**
     * Loads all users from users.txt into memory.
     * If the file does not exist, seeds default accounts and creates the file.
     */
    public static void loadUsers() {
        File f = new File(USERS_FILE);
        if (!f.exists()) {
            seedDefaults();
            saveUsers();
            return;
        }
        users.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    users.put(parts[0].trim(), new String[]{parts[1].trim(), parts[2].trim()});
                }
            }
            System.out.println("[UserManager] Loaded " + users.size() + " user(s) from " + USERS_FILE);
        } catch (IOException ex) {
            System.err.println("[UserManager] Error reading " + USERS_FILE + ": " + ex.getMessage());
        }
    }

    // ─── Save ────────────────────────────────────────────────────────────────────
    /**
     * Saves all in-memory users back to users.txt.
     * Overwrites the file completely each time.
     */
    public static void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            pw.println("# BookingSystem credentials file");
            pw.println("# Format: username:password:role");
            pw.println("# Roles: admin | user");
            pw.println();
            for (Map.Entry<String, String[]> entry : users.entrySet()) {
                pw.println(entry.getKey() + ":" + entry.getValue()[0] + ":" + entry.getValue()[1]);
            }
            System.out.println("[UserManager] Saved " + users.size() + " user(s) to " + USERS_FILE);
        } catch (IOException ex) {
            System.err.println("[UserManager] Error saving " + USERS_FILE + ": " + ex.getMessage());
        }
    }

    // ─── Validate Login ──────────────────────────────────────────────────────────
    /**
     * Returns true if the username exists and the password matches.
     */
    public static boolean validateLogin(String username, String password) {
        if (username == null || password == null) return false;
        if (!users.containsKey(username)) return false;
        return users.get(username)[0].equals(password);
    }

    // ─── Register ────────────────────────────────────────────────────────────────
    /**
     * Registers a new user and saves to file.
     * Returns false if the username is already taken.
     */
    public static boolean registerUser(String username, String password, String role) {
        if (username == null || username.isEmpty()) return false;
        if (users.containsKey(username)) return false;
        users.put(username, new String[]{password, role});
        saveUsers();
        return true;
    }

    // ─── Delete User ─────────────────────────────────────────────────────────────
    /**
     * Removes a user by username and saves to file.
     * Returns false if the user was not found.
     */
    public static boolean deleteUser(String username) {
        if (!users.containsKey(username)) return false;
        users.remove(username);
        saveUsers();
        return true;
    }

    // ─── Change Password ─────────────────────────────────────────────────────────
    /**
     * Changes the password for an existing user.
     * Returns false if the user does not exist or old password is wrong.
     */
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!validateLogin(username, oldPassword)) return false;
        users.get(username)[0] = newPassword;
        saveUsers();
        return true;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────
    /**
     * Returns true if the given user has the "admin" role.
     */
    public static boolean isAdmin(String username) {
        if (!users.containsKey(username)) return false;
        return "admin".equals(users.get(username)[1]);
    }

    /**
     * Returns true if the username already exists in the system.
     */
    public static boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Returns a list of all usernames currently stored.
     */
    public static List<String> getAllUsernames() {
        return new ArrayList<>(users.keySet());
    }

    /**
     * Returns the role of a user ("admin" or "user"), or null if not found.
     */
    public static String getRole(String username) {
        if (!users.containsKey(username)) return null;
        return users.get(username)[1];
    }

    // ─── Default seed accounts ───────────────────────────────────────────────────
    private static void seedDefaults() {
        users.put("admin", new String[]{"admin123", "admin"});
        users.put("alice", new String[]{"alice123", "user"});
        users.put("bob",   new String[]{"bob123",   "user"});
        System.out.println("[UserManager] Created default users.txt with seed accounts.");
    }
}