# JNA → JNI 전환 분석

## 🎯 **권장사항: JNI로 전환하는 것이 좋습니다**

### ✅ **JNI 전환의 장점:**

#### **1. 메모리 관리 정확성**
- **직접 제어**: C 포인터를 직접 다루어 구조체 매핑 오류 방지
- **명시적 해제**: `GetPrimitiveArrayCritical`/`ReleasePrimitiveArrayCritical`로 안전한 메모리 관리
- **GC 연동**: Java GC와 네이티브 메모리 생명주기 명확히 분리

#### **2. 성능 향상**
- **오버헤드 감소**: JNA의 reflection 기반 호출 vs JNI의 직접 호출
- **구조체 복사 최소화**: 메모리 직접 접근으로 불필요한 복사 제거
- **함수 호출 최적화**: 컴파일 타임 바인딩으로 런타임 오버헤드 감소

#### **3. 타입 안전성**
- **컴파일 타임 검증**: 함수 시그니처 불일치를 빌드 시점에 발견
- **구조체 정렬 보장**: C 헤더파일과 완벽한 ABI 호환성
- **포인터 안전성**: JNA의 Pointer 래핑 오버헤드 제거

### ❌ **현재 JNA 문제점들:**

#### **1. 복잡한 구조체 매핑**
```java
// JNA - 복잡하고 오류 가능성 높음
class RawBuffer extends Structure {
    public long len;
    public Pointer data;
    // 필드 순서, 패딩, 정렬 문제...
}
```

#### **2. 메모리 해석 오류**
```java
// JNA - 포인터 간접참조 오류로 137TB 메모리 할당 시도
PointerByReference valueRef = new PointerByReference();
OutputBuffer buffer = new OutputBuffer(valueRef.getValue()); // 🚨 오류 원인
```

#### **3. 런타임 타입 검증**
- 함수 시그니처 불일치를 런타임에만 발견
- Structure 매핑 오류가 메모리 크래시로 이어짐

## 🔧 **JNI 구현 접근법**

### **1. Rust 코드 수정 불필요**
```rust
// 이미 C ABI 완벽 지원
#[no_mangle]
pub extern "C" fn askar_store_provision(...) -> ErrorCode { ... }
```

### **2. JNI 래퍼 작성**
```c
// askar_jni.c
#include <jni.h>
#include "aries_askar.h"

JNIEXPORT jstring JNICALL Java_AskarNative_getVersion(JNIEnv *env, jobject obj) {
    char* version = askar_version();
    jstring result = (*env)->NewStringUTF(env, version);
    askar_string_free(version);
    return result;
}

JNIEXPORT jlong JNICALL Java_AskarNative_storeProvision(
    JNIEnv *env, jobject obj, jstring uri, jstring keyMethod, 
    jstring passKey, jstring profile, jboolean recreate) {
    
    const char* c_uri = (*env)->GetStringUTFChars(env, uri, NULL);
    const char* c_key_method = (*env)->GetStringUTFChars(env, keyMethod, NULL);
    const char* c_pass_key = (*env)->GetStringUTFChars(env, passKey, NULL);
    const char* c_profile = (*env)->GetStringUTFChars(env, profile, NULL);
    
    // 직접 Rust 함수 호출 - 구조체 매핑 문제 없음
    StoreHandle handle;
    int result = askar_store_provision(c_uri, c_key_method, c_pass_key, 
                                     c_profile, recreate, callback, callback_id);
    
    // 문자열 해제
    (*env)->ReleaseStringUTFChars(env, uri, c_uri);
    (*env)->ReleaseStringUTFChars(env, keyMethod, c_key_method);
    (*env)->ReleaseStringUTFChars(env, passKey, c_pass_key);
    (*env)->ReleaseStringUTFChars(env, profile, c_profile);
    
    return handle;
}
```

### **3. 구조체 처리**
```c
// SecretBuffer 직접 처리
JNIEXPORT jbyteArray JNICALL Java_AskarNative_entryGetValue(
    JNIEnv *env, jobject obj, jlong entryList, jint index) {
    
    SecretBuffer buffer = {0}; // 구조체 직접 선언
    int result = askar_entry_list_get_value(entryList, index, &buffer);
    
    if (result == 0 && buffer.data != NULL) {
        // Java byte[]로 복사
        jbyteArray array = (*env)->NewByteArray(env, buffer.len);
        (*env)->SetByteArrayRegion(env, array, 0, buffer.len, buffer.data);
        
        // Rust 메모리 해제
        askar_buffer_free(buffer);
        return array;
    }
    return NULL;
}
```

### **4. Java 인터페이스 단순화**
```java
public class AskarNative {
    static {
        System.loadLibrary("askar_jni");
    }
    
    // 간단하고 명확한 시그니처
    public static native String getVersion();
    public static native long storeProvision(String uri, String keyMethod, 
                                           String passKey, String profile, boolean recreate);
    public static native byte[] entryGetValue(long entryList, int index);
}
```

## 📊 **구현 복잡도 비교**

| 측면 | JNA (현재) | JNI (제안) |
|------|-----------|------------|
| **구조체 매핑** | 복잡, 오류 가능성 높음 | C 헤더로 정확성 보장 |
| **메모리 관리** | 포인터 래핑, 간접 참조 | 직접 제어, 명확한 생명주기 |
| **성능** | reflection 오버헤드 | 네이티브 속도 |
| **타입 안전성** | 런타임 검증 | 컴파일 타임 검증 |
| **구현 시간** | 구조체 디버깅 오래 걸림 | 초기 작업 후 안정적 |
| **유지보수** | 구조체 변경 시 복잡 | C 헤더 동기화만 필요 |

## 🚀 **권장 전환 단계**

### **Phase 1: 핵심 함수 JNI 포팅**
1. `askar_version`, `askar_store_provision` 등 기본 함수
2. 단순한 문자열/숫자 매개변수부터 시작

### **Phase 2: 구조체 처리**
1. `SecretBuffer` 직접 처리
2. Entry list 읽기 함수들

### **Phase 3: 전체 전환**
1. 모든 FFI 함수 포팅
2. 기존 JNA 코드 제거

## 💡 **결론**

**JNI 전환을 강력히 권장합니다.** 현재 JNA에서 겪고 있는 구조체 매핑 오류, 메모리 관리 문제, 성능 이슈들이 JNI로 해결될 수 있으며, Rust 코드 수정 없이 더 안정적이고 빠른 Java 바인딩을 구현할 수 있습니다.