cmake_minimum_required(VERSION 3.10)
project(askar_jni)

# Find required packages
find_package(JNI REQUIRED)

# Set C standard
set(CMAKE_C_STANDARD 99)

# Include directories
include_directories(${JNI_INCLUDE_DIRS})
include_directories(.)

# Add executable for testing
add_library(askar_jni SHARED askar_jni.c)

# Link against JNI and Askar library
target_link_libraries(askar_jni ${JNI_LIBRARIES})
target_link_libraries(askar_jni aries_askar)
target_link_libraries(askar_jni pthread)

# Set library properties
set_target_properties(askar_jni PROPERTIES
    OUTPUT_NAME "askar_jni"
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
)

# Add library search path
link_directories(/usr/local/lib)

# Compiler flags
target_compile_options(askar_jni PRIVATE -fPIC -Wall -Wextra)

# Install rule
install(TARGETS askar_jni 
    LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib
)