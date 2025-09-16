import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;

public class test_jna_alignment {
    
    public interface TestLibrary extends Library {
        TestLibrary INSTANCE = Native.load("aries_askar", TestLibrary.class);
        
        Pointer askar_version();
        void askar_string_free(Pointer str);
    }
    
    // Test structure alignment
    public static class TestRawBuffer extends Structure {
        public long len;     // Java long = 64 bits
        public Pointer data; // JNA Pointer = 64 bits
        
        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("len", "data");
        }
        
        public TestRawBuffer() {
            super();
        }
        
        public TestRawBuffer(String str) {
            super();
            if (str != null) {
                byte[] bytes = str.getBytes();
                this.len = bytes.length;
                this.data = new Memory(bytes.length);
                this.data.write(0, bytes, 0, bytes.length);
            } else {
                this.len = 0;
                this.data = Pointer.NULL;
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("=== JNA-Rust FFI Alignment Test ===");
            
            // Test basic function call
            System.out.println("Testing askar_version()...");
            Pointer versionPtr = TestLibrary.INSTANCE.askar_version();
            if (versionPtr != null && versionPtr != Pointer.NULL) {
                String version = versionPtr.getString(0);
                System.out.println("Version: " + version);
                TestLibrary.INSTANCE.askar_string_free(versionPtr);
                System.out.println("✅ Basic FFI call successful");
            } else {
                System.out.println("❌ Version pointer is null");
            }
            
            // Test structure size and alignment
            System.out.println("\n=== Structure Analysis ===");
            TestRawBuffer buffer = new TestRawBuffer();
            System.out.println("Java TestRawBuffer size: " + buffer.size() + " bytes");
            System.out.println("Expected Rust SecretBuffer size: 16 bytes (i64 + *mut u8)");
            
            // Test with actual data
            TestRawBuffer testBuffer = new TestRawBuffer("test");
            System.out.println("Test buffer len: " + testBuffer.len);
            System.out.println("Test buffer data: " + testBuffer.data);
            System.out.println("Test buffer size: " + testBuffer.size());
            
            // Check field offsets
            System.out.println("\n=== Field Offsets ===");
            System.out.println("len field offset: " + buffer.fieldOffset("len"));
            System.out.println("data field offset: " + buffer.fieldOffset("data"));
            
            System.out.println("\n✅ JNA alignment test completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error during JNA alignment test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}