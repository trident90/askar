import * as koffi from 'koffi';
import * as path from 'path';
import * as fs from 'fs';

// Determine library name based on platform
function getLibraryPath(): string {
  const libName = process.platform === 'win32'
    ? 'aries_askar.dll'
    : process.platform === 'darwin'
    ? 'libaries_askar.dylib'
    : 'libaries_askar.so';

  // Try multiple potential paths
  const possiblePaths = [
    path.join(__dirname, '..', '..', '..', 'target', 'release', libName),
    path.join(__dirname, '..', '..', '..', 'target', 'debug', libName),
    path.join(process.cwd(), 'target', 'release', libName),
    path.join(process.cwd(), 'target', 'debug', libName),
    libName, // System library path
  ];

  // Check for custom path
  if (process.env.ASKAR_LIB_PATH) {
    possiblePaths.unshift(process.env.ASKAR_LIB_PATH);
  }

  for (const p of possiblePaths) {
    try {
      if (fs.existsSync(p)) return p;
    } catch {}
  }
  return libName;
}

// Load the library
const lib = koffi.load(getLibraryPath());

// Basic types
const int8 = 'int8';
const int32 = 'int32';
const int64 = 'int64';
const uint8 = 'uint8';
const uint32 = 'uint32';
const uint64 = 'uint64';
const str = 'str';

// Handle types (opaque pointers)
const StoreHandle = 'size_t';
const SessionHandle = 'size_t';
const ScanHandle = 'size_t';
const LocalKeyHandle = 'size_t';
const EntryListHandle = 'size_t';
const KeyEntryListHandle = 'size_t';
const StringListHandle = 'size_t';
const CallbackId = int64;

// Struct definitions
const ByteBuffer = koffi.struct('ByteBuffer', {
  len: int64,
  data: 'uint8*',
});

const SecretBuffer = koffi.struct('SecretBuffer', {
  len: int64,
  data: 'uint8*',
});

const EncryptedBuffer = koffi.struct('EncryptedBuffer', {
  buffer: SecretBuffer,
  tag_pos: int64,
  nonce_pos: int64,
});

const AeadParams = koffi.struct('AeadParams', {
  nonce_length: int32,
  tag_length: int32,
});

// String buffer returned by the library (char* with custom destructor)
const StrBuffer = koffi.struct('StrBuffer', {
  buffer: 'void*',
});

// Callback function prototypes and pointer types
const StoreCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);
const SimpleCallbackProto = koffi.proto('void', [int64, int64]);
const SessionCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);
// For string callbacks, receive raw pointer (void*) and decode+free in JS handler
const StringCallbackProto = koffi.proto('void', [int64, int64, 'void*']);
const CountCallbackProto = koffi.proto('void', [int64, int64, int64]);
const RemoveCallbackProto = koffi.proto('void', [int64, int64, int8]);
const EntryListCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);
const KeyEntryListCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);
const StringListCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);
const ScanCallbackProto = koffi.proto('void', [int64, int64, 'size_t']);

const StoreCallback = koffi.pointer('StoreCallback', StoreCallbackProto);
const SimpleCallback = koffi.pointer('SimpleCallback', SimpleCallbackProto);
const SessionCallback = koffi.pointer('SessionCallback', SessionCallbackProto);
const StringCallback = koffi.pointer('StringCallback', StringCallbackProto);
const CountCallback = koffi.pointer('CountCallback', CountCallbackProto);
const RemoveCallback = koffi.pointer('RemoveCallback', RemoveCallbackProto);
const EntryListCallback = koffi.pointer('EntryListCallback', EntryListCallbackProto);
const KeyEntryListCallback = koffi.pointer('KeyEntryListCallback', KeyEntryListCallbackProto);
const StringListCallback = koffi.pointer('StringListCallback', StringListCallbackProto);
const ScanCallback = koffi.pointer('ScanCallback', ScanCallbackProto);

// FFI function definitions
export const askarLib = {
  // Version and utility functions
  askar_version: lib.func('askar_version', str, []),
  askar_terminate: lib.func('askar_terminate', 'void', []),
  askar_get_current_error: lib.func('askar_get_current_error', int64, [koffi.out(koffi.pointer(StrBuffer))]),
  // String destructor for heap-allocated strings
  askar_string_free: lib.func('askar_string_free', 'void', ['void*']),

  // Logging functions
  askar_set_default_logger: lib.func('askar_set_default_logger', int64, []),
  askar_set_max_log_level: lib.func('askar_set_max_log_level', int64, [int32]),
  askar_clear_custom_logger: lib.func('askar_clear_custom_logger', 'void', []),

  // Buffer management
  askar_buffer_free: lib.func('askar_buffer_free', 'void', [SecretBuffer]),

  // Store functions
  askar_store_generate_raw_key: lib.func('askar_store_generate_raw_key', int64, [ByteBuffer, koffi.out(koffi.pointer(StrBuffer))]),
  askar_store_provision: lib.func('askar_store_provision', int64, [str, str, str, str, int8, StoreCallback, CallbackId]),
  askar_store_open: lib.func('askar_store_open', int64, [str, str, str, str, StoreCallback, CallbackId]),
  askar_store_close: lib.func('askar_store_close', int64, [StoreHandle, SimpleCallback, CallbackId]),
  askar_store_remove: lib.func('askar_store_remove', int64, [str, RemoveCallback, CallbackId]),
  askar_store_rekey: lib.func('askar_store_rekey', int64, [StoreHandle, str, str, SimpleCallback, CallbackId]),
  askar_store_copy: lib.func('askar_store_copy', int64, [StoreHandle, str, str, str, int8, StoreCallback, CallbackId]),

  // Profile functions
  askar_store_create_profile: lib.func('askar_store_create_profile', int64, [StoreHandle, str, StringCallback, CallbackId]),
  askar_store_get_profile_name: lib.func('askar_store_get_profile_name', int64, [StoreHandle, StringCallback, CallbackId]),
  askar_store_get_default_profile: lib.func('askar_store_get_default_profile', int64, [StoreHandle, StringCallback, CallbackId]),
  askar_store_set_default_profile: lib.func('askar_store_set_default_profile', int64, [StoreHandle, str, SimpleCallback, CallbackId]),
  askar_store_list_profiles: lib.func('askar_store_list_profiles', int64, [StoreHandle, StringListCallback, CallbackId]),
  askar_store_remove_profile: lib.func('askar_store_remove_profile', int64, [StoreHandle, str, RemoveCallback, CallbackId]),

  // Sync store helpers
  askar_store_provision_sync: lib.func('askar_store_provision_sync', int64, [str, str, str, str, int8, 'size_t*']),
  askar_store_open_sync: lib.func('askar_store_open_sync', int64, [str, str, str, str, 'size_t*']),
  askar_store_close_sync: lib.func('askar_store_close_sync', int64, [StoreHandle]),
  askar_store_create_profile_sync: lib.func('askar_store_create_profile_sync', int64, [StoreHandle, str, 'str*']),
  askar_store_create_profile_noptr_sync: lib.func('askar_store_create_profile_noptr_sync', int64, [StoreHandle, str]),
  askar_store_list_profiles_sync: lib.func('askar_store_list_profiles_sync', int64, [StoreHandle, 'size_t*']),
  askar_store_list_profiles_json_sync: lib.func('askar_store_list_profiles_json_sync', int64, [StoreHandle, koffi.out(koffi.pointer(StrBuffer))]),
  askar_store_profile_exists_sync: lib.func('askar_store_profile_exists_sync', int64, [StoreHandle, str, 'int8*']),
  askar_store_remove_profile_sync: lib.func('askar_store_remove_profile_sync', int64, [StoreHandle, str, 'int8*']),

  // Session functions
  askar_session_start: lib.func('askar_session_start', int64, [StoreHandle, str, int8, SessionCallback, CallbackId]),
  askar_session_close: lib.func('askar_session_close', int64, [SessionHandle, int8, SimpleCallback, CallbackId]),
  askar_session_count: lib.func('askar_session_count', int64, [SessionHandle, str, str, CountCallback, CallbackId]),
  askar_session_fetch: lib.func('askar_session_fetch', int64, [SessionHandle, str, str, int8, EntryListCallback, CallbackId]),
  askar_session_fetch_all: lib.func('askar_session_fetch_all', int64, [SessionHandle, str, str, int64, str, int8, int8, EntryListCallback, CallbackId]),
  askar_session_remove_all: lib.func('askar_session_remove_all', int64, [SessionHandle, str, str, CountCallback, CallbackId]),
  askar_session_update: lib.func('askar_session_update', int64, [SessionHandle, int8, str, str, ByteBuffer, str, int64, SimpleCallback, CallbackId]),

  // Sync session helpers
  askar_session_start_sync: lib.func('askar_session_start_sync', int64, [StoreHandle, str, int8, 'size_t*']),
  askar_session_close_sync: lib.func('askar_session_close_sync', int64, [SessionHandle, int8]),
  askar_session_update_sync: lib.func('askar_session_update_sync', int64, [SessionHandle, int8, str, str, ByteBuffer, str, int64]),
  askar_session_fetch_sync: lib.func('askar_session_fetch_sync', int64, [SessionHandle, str, str, int8, 'size_t*']),
  askar_session_count_sync: lib.func('askar_session_count_sync', int64, [SessionHandle, str, str, 'int64*']),
  askar_session_fetch_all_sync: lib.func('askar_session_fetch_all_sync', int64, [SessionHandle, str, str, int64, str, int8, int8, 'size_t*']),

  // Key management in sessions
  askar_session_insert_key: lib.func('askar_session_insert_key', int64, [SessionHandle, LocalKeyHandle, str, str, str, int64, SimpleCallback, CallbackId]),
  askar_session_fetch_key: lib.func('askar_session_fetch_key', int64, [SessionHandle, str, int8, KeyEntryListCallback, CallbackId]),
  askar_session_fetch_all_keys: lib.func('askar_session_fetch_all_keys', int64, [SessionHandle, str, str, str, int64, int8, KeyEntryListCallback, CallbackId]),
  askar_session_update_key: lib.func('askar_session_update_key', int64, [SessionHandle, str, str, str, int64, SimpleCallback, CallbackId]),
  askar_session_remove_key: lib.func('askar_session_remove_key', int64, [SessionHandle, str, SimpleCallback, CallbackId]),

  // Scan functions
  askar_scan_start: lib.func('askar_scan_start', int64, [StoreHandle, str, str, str, int64, int64, str, int8, ScanCallback, CallbackId]),
  askar_scan_next: lib.func('askar_scan_next', int64, [ScanHandle, SimpleCallback, CallbackId]),
  askar_scan_free: lib.func('askar_scan_free', int64, [ScanHandle]),

  // Entry list functions
  askar_entry_list_count: lib.func('askar_entry_list_count', int64, [EntryListHandle, 'int32*']),
  askar_entry_list_free: lib.func('askar_entry_list_free', 'void', [EntryListHandle]),
  askar_entry_list_get_category: lib.func('askar_entry_list_get_category', int64, [EntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_entry_list_get_name: lib.func('askar_entry_list_get_name', int64, [EntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_entry_list_get_value: lib.func('askar_entry_list_get_value', int64, [EntryListHandle, int32, koffi.pointer(SecretBuffer)]),
  askar_entry_list_get_tags: lib.func('askar_entry_list_get_tags', int64, [EntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),

  // Key entry list functions
  askar_key_entry_list_count: lib.func('askar_key_entry_list_count', int64, [KeyEntryListHandle, 'int32*']),
  askar_key_entry_list_free: lib.func('askar_key_entry_list_free', 'void', [KeyEntryListHandle]),
  askar_key_entry_list_get_algorithm: lib.func('askar_key_entry_list_get_algorithm', int64, [KeyEntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_entry_list_get_name: lib.func('askar_key_entry_list_get_name', int64, [KeyEntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_entry_list_get_metadata: lib.func('askar_key_entry_list_get_metadata', int64, [KeyEntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_entry_list_get_tags: lib.func('askar_key_entry_list_get_tags', int64, [KeyEntryListHandle, int32, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_entry_list_load_local: lib.func('askar_key_entry_list_load_local', int64, [KeyEntryListHandle, int32, 'size_t*']),

  // String list functions
  askar_string_list_count: lib.func('askar_string_list_count', int64, [StringListHandle, 'int32*']),
  askar_string_list_free: lib.func('askar_string_list_free', 'void', [StringListHandle]),
  askar_string_list_get_item: lib.func('askar_string_list_get_item', int64, [StringListHandle, int32, 'str*']),

  // Key functions
  askar_key_generate: lib.func('askar_key_generate', int64, [str, str, int8, 'size_t*']),
  askar_key_free: lib.func('askar_key_free', 'void', [LocalKeyHandle]),
  askar_key_from_seed: lib.func('askar_key_from_seed', int64, [str, ByteBuffer, str, 'size_t*']),
  askar_key_from_secret_bytes: lib.func('askar_key_from_secret_bytes', int64, [str, ByteBuffer, 'size_t*']),
  askar_key_from_public_bytes: lib.func('askar_key_from_public_bytes', int64, [str, ByteBuffer, 'size_t*']),
  askar_key_from_jwk: lib.func('askar_key_from_jwk', int64, [ByteBuffer, 'size_t*']),
  askar_key_convert: lib.func('askar_key_convert', int64, [LocalKeyHandle, str, 'size_t*']),
  askar_key_from_key_exchange: lib.func('askar_key_from_key_exchange', int64, [str, LocalKeyHandle, LocalKeyHandle, 'size_t*']),

  // Key property functions
  // Prefer StrBuffer* variant for stability
  askar_key_get_algorithm_buf: lib.func('askar_key_get_algorithm_buf', int64, [LocalKeyHandle, koffi.out(koffi.pointer(StrBuffer))]),
  // Helper returning const char* for environments where StrBuffer out is unstable
  askar_key_get_algorithm_str: lib.func('const char* askar_key_get_algorithm_str(size_t handle)'),
  askar_key_get_ephemeral: lib.func('askar_key_get_ephemeral', int64, [LocalKeyHandle, 'int8*']),
  askar_key_get_jwk_public: lib.func('askar_key_get_jwk_public', int64, [LocalKeyHandle, str, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_get_jwk_secret: lib.func('askar_key_get_jwk_secret', int64, [LocalKeyHandle, koffi.pointer(SecretBuffer)]),
  askar_key_get_jwk_thumbprint: lib.func('askar_key_get_jwk_thumbprint', int64, [LocalKeyHandle, str, koffi.out(koffi.pointer(StrBuffer))]),
  askar_key_get_public_bytes: lib.func('askar_key_get_public_bytes', int64, [LocalKeyHandle, koffi.pointer(SecretBuffer)]),
  askar_key_get_secret_bytes: lib.func('askar_key_get_secret_bytes', int64, [LocalKeyHandle, koffi.pointer(SecretBuffer)]),
  askar_key_get_supported_backends: lib.func('askar_key_get_supported_backends', int64, ['size_t*']),

  // Cryptographic operations
  askar_key_aead_get_params: lib.func('askar_key_aead_get_params', int64, [LocalKeyHandle, koffi.pointer(AeadParams)]),
  askar_key_aead_random_nonce: lib.func('askar_key_aead_random_nonce', int64, [LocalKeyHandle, koffi.pointer(SecretBuffer)]),
  askar_key_aead_encrypt: lib.func('askar_key_aead_encrypt', int64, [LocalKeyHandle, ByteBuffer, ByteBuffer, ByteBuffer, koffi.pointer(EncryptedBuffer)]),
  askar_key_aead_decrypt: lib.func('askar_key_aead_decrypt', int64, [LocalKeyHandle, ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer, koffi.pointer(SecretBuffer)]),
  askar_key_aead_get_padding: lib.func('askar_key_aead_get_padding', int64, [LocalKeyHandle, int64, 'int32*']),

  // Signing and verification
  askar_key_sign_message: lib.func('askar_key_sign_message', int64, [LocalKeyHandle, ByteBuffer, str, koffi.pointer(SecretBuffer)]),
  // Helper: return base64-encoded signature as a JS string
  askar_key_sign_message_b64: lib.func('askar_key_sign_message_b64', 'str', [LocalKeyHandle, ByteBuffer, str]),
  askar_key_verify_signature: lib.func('askar_key_verify_signature', int64, [LocalKeyHandle, ByteBuffer, ByteBuffer, str, 'int8*']),
  askar_key_verify_signature_bool: lib.func('askar_key_verify_signature_bool', 'int8', [LocalKeyHandle, ByteBuffer, ByteBuffer, str]),

  // Key wrapping
  askar_key_wrap_key: lib.func('askar_key_wrap_key', int64, [LocalKeyHandle, LocalKeyHandle, ByteBuffer, koffi.pointer(EncryptedBuffer)]),
  askar_key_unwrap_key: lib.func('askar_key_unwrap_key', int64, [LocalKeyHandle, str, ByteBuffer, ByteBuffer, ByteBuffer, 'size_t*']),

  // Crypto box functions
  askar_key_crypto_box_random_nonce: lib.func('askar_key_crypto_box_random_nonce', int64, [koffi.pointer(SecretBuffer)]),
  askar_key_crypto_box: lib.func('askar_key_crypto_box', int64, [LocalKeyHandle, LocalKeyHandle, ByteBuffer, ByteBuffer, koffi.pointer(SecretBuffer)]),
  askar_key_crypto_box_open: lib.func('askar_key_crypto_box_open', int64, [LocalKeyHandle, LocalKeyHandle, ByteBuffer, ByteBuffer, koffi.pointer(SecretBuffer)]),
  askar_key_crypto_box_seal: lib.func('askar_key_crypto_box_seal', int64, [LocalKeyHandle, ByteBuffer, koffi.pointer(SecretBuffer)]),
  askar_key_crypto_box_seal_open: lib.func('askar_key_crypto_box_seal_open', int64, [LocalKeyHandle, ByteBuffer, koffi.pointer(SecretBuffer)]),

  // ECDH functions
  askar_key_derive_ecdh_es: lib.func('askar_key_derive_ecdh_es', int64, [str, LocalKeyHandle, LocalKeyHandle, ByteBuffer, ByteBuffer, ByteBuffer, int8, 'size_t*']),
  askar_key_derive_ecdh_1pu: lib.func('askar_key_derive_ecdh_1pu', int64, [str, LocalKeyHandle, LocalKeyHandle, LocalKeyHandle, ByteBuffer, ByteBuffer, ByteBuffer, ByteBuffer, int8, 'size_t*']),

  // Migration
  askar_migrate_indy_sdk: lib.func('askar_migrate_indy_sdk', int64, [str, str, str, str, SimpleCallback, CallbackId]),
};

export {
  SecretBuffer,
  ByteBuffer,
  EncryptedBuffer,
  AeadParams,
  StrBuffer,
  StoreCallback,
  SimpleCallback,
  SessionCallback,
  StringCallback,
  CountCallback,
  RemoveCallback,
  EntryListCallback,
  KeyEntryListCallback,
  StringListCallback,
  ScanCallback,
  lib,
  koffi,
};
