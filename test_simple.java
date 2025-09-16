import org.hyperledger.aries.askar.*;

public class test_simple {
    public static void main(String[] args) {
        try {
            System.out.println("Starting simple test...");
            LibraryLoader.setMaxLogLevel(1); // ERROR level only
            System.out.println("Version: " + LibraryLoader.getVersion());
            System.out.println("Simple test completed successfully!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}