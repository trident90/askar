#include <stdio.h>
#include <stdint.h>

// Rust SecretBuffer equivalent in C
struct SecretBuffer {
    int64_t len;
    uint8_t* data;
};

// Test structure that matches Java RawBuffer  
struct JavaRawBuffer {
    int64_t len;    // Java long -> 64-bit
    void* data;     // JNA Pointer -> void* (64-bit)
};

int main() {
    printf("=== Structure Size Compatibility Test ===\n");
    printf("Rust SecretBuffer size: %zu bytes\n", sizeof(struct SecretBuffer));
    printf("Java RawBuffer equivalent size: %zu bytes\n", sizeof(struct JavaRawBuffer));
    
    printf("\n=== Field Offsets ===\n");
    printf("SecretBuffer len offset: %zu\n", __builtin_offsetof(struct SecretBuffer, len));
    printf("SecretBuffer data offset: %zu\n", __builtin_offsetof(struct SecretBuffer, data));
    
    printf("JavaRawBuffer len offset: %zu\n", __builtin_offsetof(struct JavaRawBuffer, len));  
    printf("JavaRawBuffer data offset: %zu\n", __builtin_offsetof(struct JavaRawBuffer, data));
    
    printf("\n=== Alignment Analysis ===\n");
    printf("int64_t alignment: %zu\n", _Alignof(int64_t));
    printf("void* alignment: %zu\n", _Alignof(void*));
    printf("uint8_t* alignment: %zu\n", _Alignof(uint8_t*));
    
    if (sizeof(struct SecretBuffer) == sizeof(struct JavaRawBuffer)) {
        printf("\n✅ Structure sizes match!\n");
    } else {
        printf("\n❌ Structure size mismatch!\n");
    }
    
    return 0;
}