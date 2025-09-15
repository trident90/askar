# Askar Java Examples

이 디렉토리에는 Askar Java wrapper 사용법을 보여주는 예제들이 포함되어 있습니다.

## 전제 조건

1. **Java 8 이상**
2. **Maven 또는 Gradle**
3. **Askar 네이티브 라이브러리** (libaries_askar.so/.dylib/.dll)

## 빌드 및 실행 방법

### 1. 네이티브 라이브러리 빌드 (필요한 경우)

먼저 Askar 네이티브 라이브러리를 빌드해야 합니다:

```bash
cd ../../
cargo build --release
```

### 2. Java wrapper 빌드

예제를 실행하기 전에 Java wrapper를 빌드해야 합니다:

```bash
cd wrappers/java
mvn clean package -DskipTests
```

### 3. 예제 실행

#### Maven 사용

```bash
cd examples

# BasicStoreExample 실행
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.BasicStoreExample"

# CryptographyExample 실행
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.CryptographyExample"

# 또는 predefined execution 사용
mvn exec:java@basic-store
mvn exec:java@cryptography
```

#### Gradle 사용

```bash
cd examples

# BasicStoreExample 실행 (기본)
./gradlew run

# 또는 특정 태스크 사용
./gradlew runBasicStore
./gradlew runCryptography
```

### 4. 직접 Java 명령어로 실행

```bash
cd examples

# 먼저 컴파일
mvn compile
# 또는
./gradlew compileJava

# Java 명령어로 직접 실행
java -cp "target/classes:../target/aries-askar-0.4.5.jar:~/.m2/repository/net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar:~/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar:~/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar:~/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar:~/.m2/repository/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar" \
-Djava.library.path="../../target/release" \
org.hyperledger.aries.askar.examples.BasicStoreExample
```

## 포함된 예제들

### 1. BasicStoreExample

기본적인 스토어 작업을 보여줍니다:
- 스토어 생성 및 열기
- 데이터 삽입, 조회, 업데이트, 삭제
- 태그를 사용한 메타데이터 관리
- 트랜잭션 사용
- 프로파일 관리
- 키 저장 및 관리

### 2. CryptographyExample

암호화 기능을 보여줍니다:
- 다양한 키 알고리즘 사용
- 디지털 서명 생성 및 검증
- 시드에서 키 유도
- 키 형식 변환 (raw bytes, JWK)
- 키 알고리즘 간 변환

## 문제 해결

### 1. 네이티브 라이브러리를 찾을 수 없는 경우

```
java.lang.UnsatisfiedLinkError: Unable to load library 'aries_askar'
```

**해결방법:**
- 네이티브 라이브러리가 올바른 위치에 있는지 확인
- `java.library.path` 시스템 속성이 올바른지 확인
- 라이브러리 파일명이 플랫폼에 맞는지 확인:
  - Linux: `libaries_askar.so`
  - macOS: `libaries_askar.dylib`
  - Windows: `aries_askar.dll`

### 2. 클래스패스 문제

```
java.lang.ClassNotFoundException
```

**해결방법:**
- 모든 필요한 JAR 파일이 클래스패스에 포함되어 있는지 확인
- Maven/Gradle을 사용하여 의존성을 자동으로 관리

### 3. Java 버전 호환성

```
java.lang.UnsupportedClassVersionError
```

**해결방법:**
- Java 8 이상을 사용하고 있는지 확인
- `JAVA_HOME` 환경변수가 올바른지 확인

## 라이브러리 경로 설정

### Linux/macOS

```bash
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$(pwd)/../../target/release
# 또는
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:$(pwd)/../../target/release  # macOS
```

### Windows

```cmd
set PATH=%PATH%;%CD%\..\..\target\release
```

## 스크립트를 사용한 실행

편의를 위해 실행 스크립트를 만들 수 있습니다:

### run-examples.sh (Linux/macOS)

```bash
#!/bin/bash
cd "$(dirname "$0")"

# 네이티브 라이브러리 경로 설정
export LD_LIBRARY_PATH="$(pwd)/../../target/release:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$(pwd)/../../target/release:$DYLD_LIBRARY_PATH"

# Maven을 사용하여 예제 실행
echo "Running BasicStoreExample..."
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.BasicStoreExample"

echo "Running CryptographyExample..."
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.CryptographyExample"
```

### run-examples.bat (Windows)

```batch
@echo off
cd /d "%~dp0"

set PATH=%PATH%;%CD%\..\..\target\release

echo Running BasicStoreExample...
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.BasicStoreExample"

echo Running CryptographyExample...
mvn exec:java -Dexec.mainClass="org.hyperledger.aries.askar.examples.CryptographyExample"
```

## 추가 정보

- [Askar Java Wrapper README](../README.md)
- [Contributing Guide](../CONTRIBUTING.md)
- [API Documentation](../target/site/apidocs/index.html) (javadoc 생성 후)