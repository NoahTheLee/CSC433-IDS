
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class IDS {

    public static void main(String[] args) throws IOException {
        List<String> paths = new ArrayList<>(); // Generate empty list to hold files
        String dirPath = "Example Dir"; //Set up basic directory path
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) { // Make sure it exists
            throw new FileNotFoundException("Directory not found: " + dirPath);
        }
        recurseAddDirs(paths, dirPath); // Add all files and/or directories recursively

        String[] specificFilePaths = {
            "Second Example Dir/misc_file.txt"};
        addSpecificFiles(paths, specificFilePaths);

        // Print all collected paths
        for (String path : paths) {
            System.out.println("File Path: " + path);
        }

        // Create checksum file and populate it with paths and SHA256 checksums
        makeChecksum(paths);

        // Start iterating over the files
        while (true) {
            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            File checksumFile = new File("checksum_file.txt");
            if (!checksumFile.exists() || !checksumFile.isFile()) {
                System.err.println("Checksum file not found.");
                continue;
            }
            String checksumContent = readFileContents("checksum_file.txt");
            String[] entries = checksumContent.split("\n\n"); // Split entries by double newline

            boolean discrepancyFound = false;

            for (String entry : entries) {
                String[] lines = entry.split("\n");
                if (lines.length < 2) {
                    continue; // Skip invalid entries
                }
                String filePath = lines[0].trim();
                String storedHash = lines[1].trim();

                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    System.out.println("File missing: " + filePath);
                    discrepancyFound = true;
                    continue;
                }

                String currentHash = fileToHex(filePath);
                if (!currentHash.equals(storedHash)) {
                    System.out.println("File modified: " + filePath);
                    discrepancyFound = true;
                }
            }

            if (!discrepancyFound) {
                System.out.println("No discrepancies found.");
            }
        }
        // If a discrepancy is found, alert
    }

    // Recursive method to add directories and files
    public static void recurseAddDirs(List<String> paths, String dirPath) {
        File dir = new File(dirPath); // Create a File object for the directory
        File[] files = dir.listFiles(); // List all files and directories in the given directory
        if (files != null) {
            for (File file : files) { // Iterate through each file or directory
                if (file.isDirectory()) { // If the file is a directory, recurse
                    System.out.println(file.getName() + " (Directory)");
                    recurseAddDirs(paths, file.getAbsolutePath());
                } else if (file.isFile()) { // If the file is a file, add its path to the list
                    System.out.println(file.getName() + " (File)");
                    paths.add(file.getAbsolutePath());
                } else { // Default case for some other invalid file type
                    System.out.println(file.getName() + " (Unknown Type)");
                }
            }
        }

    }

    // Method to add specific files to the list
    public static void addSpecificFiles(List<String> paths, String[] specificFilePaths) throws FileNotFoundException {
        for (String filePath : specificFilePaths) { // Iterate through list
            File file = new File(filePath);
            if (!file.exists()) { // If the file does not exist, throw error
                throw new FileNotFoundException("File not found: " + filePath);
            } else if (!file.isFile()) { // If it's not a file, throw error
                throw new IllegalArgumentException("Expected a file but found a directory: " + filePath);
            } else { //Otherwise, assume it's valid
                paths.add(file.getAbsolutePath());
            }
        }
    }

    // Method to make make, auth, then write to checksum file
    public static File makeChecksum(List<String> paths) throws IOException {
        File checksumFile = new File("checksum_file.txt");

        authChecksum(checksumFile); // Ensure the checksum file exists and is empty

        FileWriter checksumWriter = new FileWriter(checksumFile, true);
        for (String path : paths) {
            checksumWriter.write(path + "\n");
            checksumWriter.write(fileToHex(path) + "\n\n");
            checksumWriter.flush();
        }
        checksumWriter.close();

        return new File("checksum_file.txt");
    }

    // Simple function to confirm checksum file exists
    public static void authChecksum(File checksumFile) {
        if (!checksumFile.exists()) { // If the file does not exist...
            try { // Create a new one
                checksumFile.createNewFile();
            } catch (Exception e) {
                System.err.println("Failed to create checksum file: " + e.getMessage());
            }
        } else { // otherwise...
            try (java.io.PrintWriter writer = new java.io.PrintWriter(checksumFile)) { // Purge it
                writer.print("");
            } catch (Exception e) {
                System.err.println("Failed to purge checksum file: " + e.getMessage());
            }
        }
    }

    // Method to convert a file to a SHA256 hex string
    public static String fileToHex(String path) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found: ", e);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                digest.update(line.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + path, e);
        }
        byte[] hash = digest.digest();
        return bytesToHex(hash);
    }

    // Simple method to read a file's contents
    public static String readFileContents(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // Helper method to convert byte array to hex string for human readability
    // copied  from Baeldung
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // In other homeworks, I've used a hexToBytes method
    // This is unnecessary here, since we never need to convert back
}
