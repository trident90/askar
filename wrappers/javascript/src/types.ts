export enum ErrorCode {
  Success = 0,
  Backend = 1,
  Busy = 2,
  Duplicate = 3,
  Encryption = 4,
  Input = 5,
  NotFound = 6,
  Unexpected = 7,
  Unsupported = 8,
  Custom = 100,
}

export enum EntryOperation {
  Insert = 0,
  Replace = 1,
  Remove = 2,
}

export enum KeyAlg {
  Ed25519 = 'ed25519',
  X25519 = 'x25519',
  Secp256k1 = 'secp256k1',
  P256 = 'p-256',
  P384 = 'p-384',
  P521 = 'p-521',
  Bls12381G1 = 'bls12381g1',
  Bls12381G2 = 'bls12381g2',
  Bls12381G1G2 = 'bls12381g1g2',
  A128Gcm = 'a128gcm',
  A256Gcm = 'a256gcm',
  A128CbcHs256 = 'a128cbc-hs256',
  A256CbcHs512 = 'a256cbc-hs512',
  A128Kw = 'a128kw',
  A192Kw = 'a192kw',
  A256Kw = 'a256kw',
  C20P = 'c20p',
  XC20P = 'xc20p',
}

export enum KeyBackend {
  Software = 'software',
  SecretBox = 'secretbox',
}

export enum SeedMethod {
  BlsKeyGen = 'bls_keygen',
  BlsPopProve = 'bls_pop_prove',
}

export interface ByteBuffer {
  len: number;
  data: Buffer;
}

export interface SecretBuffer {
  len: number;
  data: Buffer;
}

export interface EncryptedBuffer {
  buffer: SecretBuffer;
  tagPos: number;
  noncePos: number;
}

export interface AeadParams {
  nonceLength: number;
  tagLength: number;
}

export interface Entry {
  category: string;
  name: string;
  value: Buffer;
  tags?: Record<string, string>;
}

export interface KeyEntry {
  name: string;
  algorithm: string;
  metadata?: string;
  tags?: Record<string, string>;
}

export type StoreHandle = Buffer;
export type SessionHandle = Buffer;
export type ScanHandle = Buffer;
export type LocalKeyHandle = Buffer;
export type EntryListHandle = Buffer;
export type KeyEntryListHandle = Buffer;
export type StringListHandle = Buffer;
export type CallbackId = number;