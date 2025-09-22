import * as koffi from 'koffi';
import { askarNative } from './native';
import { askarLib, SecretBuffer, ByteBuffer, AeadParams, EncryptedBuffer } from './ffi';
import { KeyAlg, KeyBackend, SeedMethod } from './types';
import { AskarError, createByteBuffer, checkResult, readSecretBuffer, freeSecretBuffer } from './utils';

export class LocalKey {
  public handle: number;

  constructor(handle: number) {
    this.handle = handle;
  }

  static async generate(
    algorithm: KeyAlg | string,
    backend?: KeyBackend | string,
    ephemeral: boolean = false
  ): Promise<LocalKey> {
    const algStr = typeof algorithm === 'string' ? algorithm : algorithm;
    const backendStr = backend ? (typeof backend === 'string' ? backend : backend) : undefined;
    const handle = await askarNative.keyGenerate(algStr, backendStr, ephemeral);
    return new LocalKey(handle);
  }

  async getAlgorithm(): Promise<string> {
    return await askarNative.keyGetAlgorithm(this.handle);
  }

  free(): void {
    askarNative.keyFree(this.handle);
  }

  // Full implementations using Koffi FFI
  static fromSeed(algorithm: KeyAlg | string, seed: Buffer | string, method?: SeedMethod | string): LocalKey {
    const alg = typeof algorithm === 'string' ? algorithm : algorithm;
    const buf = createByteBuffer(seed);
    const meth = method ? (typeof method === 'string' ? method : method) : null;
    const handlePtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_from_seed(alg, buf, meth, handlePtr);
    checkResult(rc);
    const handle = koffi.decode(handlePtr, 'size_t');
    return new LocalKey(handle);
  }

  static fromSecretBytes(algorithm: KeyAlg | string, secret: Buffer | string): LocalKey {
    const alg = typeof algorithm === 'string' ? algorithm : algorithm;
    const buf = createByteBuffer(secret);
    const handlePtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_from_secret_bytes(alg, buf, handlePtr);
    checkResult(rc);
    const handle = koffi.decode(handlePtr, 'size_t');
    return new LocalKey(handle);
  }

  static fromPublicBytes(algorithm: KeyAlg | string, publicBytes: Buffer | string): LocalKey {
    const alg = typeof algorithm === 'string' ? algorithm : algorithm;
    const buf = createByteBuffer(publicBytes);
    const handlePtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_from_public_bytes(alg, buf, handlePtr);
    checkResult(rc);
    const handle = koffi.decode(handlePtr, 'size_t');
    return new LocalKey(handle);
  }

  static fromJwk(jwk: object | string): LocalKey {
    const data = typeof jwk === 'string' ? Buffer.from(jwk, 'utf8') : Buffer.from(JSON.stringify(jwk), 'utf8');
    const buf = createByteBuffer(data);
    const handlePtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_from_jwk(buf, handlePtr);
    checkResult(rc);
    const handle = koffi.decode(handlePtr, 'size_t');
    return new LocalKey(handle);
  }

  isEphemeral(): boolean {
    const out: any[] = [0];
    const rc = askarLib.askar_key_get_ephemeral(this.handle, out);
    checkResult(rc);
    return !!out[0];
  }

  getJwkPublic(algorithm?: KeyAlg | string): string {
    const alg = algorithm ? (typeof algorithm === 'string' ? algorithm : algorithm) : null;
    const sbPtr = koffi.alloc(require('./ffi').StrBuffer, 1);
    const rc = askarLib.askar_key_get_jwk_public(this.handle, alg, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
    const s = sb.buffer ? koffi.decode(sb.buffer, 'str') : '';
    if (sb.buffer) askarLib.askar_string_free(sb.buffer);
    return s || '';
  }

  getJwkSecret(): Buffer {
    const sbPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_get_jwk_secret(this.handle, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, SecretBuffer);
    try {
      return readSecretBuffer(sb);
    } finally {
      freeSecretBuffer(sb);
    }
  }

  getJwkThumbprint(algorithm?: KeyAlg | string): string {
    const alg = algorithm ? (typeof algorithm === 'string' ? algorithm : algorithm) : null;
    const sbPtr = koffi.alloc(require('./ffi').StrBuffer, 1);
    const rc = askarLib.askar_key_get_jwk_thumbprint(this.handle, alg, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
    const s = sb.buffer ? koffi.decode(sb.buffer, 'str') : '';
    if (sb.buffer) askarLib.askar_string_free(sb.buffer);
    return s || '';
  }

  getPublicBytes(): Buffer {
    const sbPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_get_public_bytes(this.handle, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, SecretBuffer);
    try {
      return readSecretBuffer(sb);
    } finally {
      freeSecretBuffer(sb);
    }
  }

  getSecretBytes(): Buffer {
    const sbPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_get_secret_bytes(this.handle, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, SecretBuffer);
    try {
      return readSecretBuffer(sb);
    } finally {
      freeSecretBuffer(sb);
    }
  }

  convert(algorithm: KeyAlg | string): LocalKey {
    const alg = typeof algorithm === 'string' ? algorithm : algorithm;
    const outPtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_convert(this.handle, alg, outPtr);
    checkResult(rc);
    const handle = koffi.decode(outPtr, 'size_t');
    return new LocalKey(handle);
  }

  // Signing
  signMessage(message: Buffer | string, sigType: string | null = null): Buffer {
    const msg = createByteBuffer(message);
    const b64 = askarLib.askar_key_sign_message_b64(this.handle, msg, sigType ?? null);
    const s = b64 || '';
    return Buffer.from(s, 'base64');
  }

  verifySignature(message: Buffer | string, signature: Buffer | string, sigType: string | null = null): boolean {
    const msg = createByteBuffer(message);
    const sig = createByteBuffer(signature);
    const res = askarLib.askar_key_verify_signature_bool(this.handle, msg, sig, sigType ?? null);
    return !!res;
  }

  // AEAD operations
  aeadGetParams(): { nonceLength: number; tagLength: number } {
    const ptr = koffi.alloc(AeadParams, 1);
    const rc = askarLib.askar_key_aead_get_params(this.handle, ptr);
    checkResult(rc);
    const p = koffi.decode(ptr, AeadParams);
    return { nonceLength: p.nonce_length, tagLength: p.tag_length };
  }

  aeadRandomNonce(): Buffer {
    const sbPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_aead_random_nonce(this.handle, sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  aeadEncrypt(message: Buffer | string, nonce: Buffer, aad: Buffer | string): { ciphertext: Buffer; tagPos: number; noncePos: number } {
    const msg = createByteBuffer(message);
    const n = createByteBuffer(nonce);
    const a = createByteBuffer(aad);
    const outPtr = koffi.alloc(EncryptedBuffer, 1);
    const rc = askarLib.askar_key_aead_encrypt(this.handle, msg, n, a, outPtr);
    checkResult(rc);
    const enc = koffi.decode(outPtr, EncryptedBuffer);
    try {
      const data = readSecretBuffer(enc.buffer);
      return { ciphertext: data, tagPos: Number(enc.tag_pos), noncePos: Number(enc.nonce_pos) };
    } finally {
      freeSecretBuffer(enc.buffer);
    }
  }

  aeadDecrypt(ciphertext: Buffer | string, nonce: Buffer, tag: Buffer | string, aad: Buffer | string): Buffer {
    const ct = createByteBuffer(ciphertext);
    const n = createByteBuffer(nonce);
    const t = createByteBuffer(tag);
    const a = createByteBuffer(aad);
    const outPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_aead_decrypt(this.handle, ct, n, t, a, outPtr);
    checkResult(rc);
    const sb = koffi.decode(outPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  // Key wrap/unwrap
  wrapKey(other: LocalKey, nonce: Buffer | string): { ciphertext: Buffer; tagPos: number; noncePos: number } {
    const n = createByteBuffer(nonce);
    const outPtr = koffi.alloc(EncryptedBuffer, 1);
    const rc = askarLib.askar_key_wrap_key(this.handle, other.handle, n, outPtr);
    checkResult(rc);
    const enc = koffi.decode(outPtr, EncryptedBuffer);
    try {
      const data = readSecretBuffer(enc.buffer);
      return { ciphertext: data, tagPos: Number(enc.tag_pos), noncePos: Number(enc.nonce_pos) };
    } finally { freeSecretBuffer(enc.buffer); }
  }

  unwrapKey(algorithm: KeyAlg | string, ciphertext: Buffer | string, nonce: Buffer | string, tag: Buffer | string): LocalKey {
    const alg = typeof algorithm === 'string' ? algorithm : algorithm;
    const ct = createByteBuffer(ciphertext);
    const n = createByteBuffer(nonce);
    const t = createByteBuffer(tag);
    const outPtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_unwrap_key(this.handle, alg, ct, n, t, outPtr);
    checkResult(rc);
    const handle = koffi.decode(outPtr, 'size_t');
    return new LocalKey(handle);
  }

  // Crypto box
  static cryptoBoxRandomNonce(): Buffer {
    const sbPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_crypto_box_random_nonce(sbPtr);
    checkResult(rc);
    const sb = koffi.decode(sbPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  cryptoBox(peer: LocalKey, message: Buffer | string, nonce: Buffer): Buffer {
    const msg = createByteBuffer(message);
    const n = createByteBuffer(nonce);
    const outPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_crypto_box(this.handle, peer.handle, msg, n, outPtr);
    checkResult(rc);
    const sb = koffi.decode(outPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  cryptoBoxOpen(peer: LocalKey, ciphertext: Buffer | string, nonce: Buffer): Buffer {
    const ct = createByteBuffer(ciphertext);
    const n = createByteBuffer(nonce);
    const outPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_crypto_box_open(this.handle, peer.handle, ct, n, outPtr);
    checkResult(rc);
    const sb = koffi.decode(outPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  cryptoBoxSeal(message: Buffer | string): Buffer {
    const msg = createByteBuffer(message);
    const outPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_crypto_box_seal(this.handle, msg, outPtr);
    checkResult(rc);
    const sb = koffi.decode(outPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  cryptoBoxSealOpen(ciphertext: Buffer | string): Buffer {
    const ct = createByteBuffer(ciphertext);
    const outPtr = koffi.alloc(SecretBuffer, 1);
    const rc = askarLib.askar_key_crypto_box_seal_open(this.handle, ct, outPtr);
    checkResult(rc);
    const sb = koffi.decode(outPtr, SecretBuffer);
    try { return readSecretBuffer(sb); } finally { freeSecretBuffer(sb); }
  }

  // ECDH
  static deriveEcdhEs(algorithm: string, senderPriv: LocalKey, recipientPub: LocalKey, apu: Buffer | string, apv: Buffer | string, info: Buffer | string, ephemeral: boolean): LocalKey {
    const apuBuf = createByteBuffer(apu);
    const apvBuf = createByteBuffer(apv);
    const infoBuf = createByteBuffer(info);
    const outPtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_derive_ecdh_es(algorithm, senderPriv.handle, recipientPub.handle, apuBuf, apvBuf, infoBuf, ephemeral ? 1 : 0, outPtr);
    checkResult(rc);
    const handle = koffi.decode(outPtr, 'size_t');
    return new LocalKey(handle);
  }

  static deriveEcdh1pu(algorithm: string, senderPriv: LocalKey, recipientPub: LocalKey, senderPub: LocalKey, apu: Buffer | string, apv: Buffer | string, info: Buffer | string, tag: Buffer | string, ephemeral: boolean): LocalKey {
    const apuBuf = createByteBuffer(apu);
    const apvBuf = createByteBuffer(apv);
    const infoBuf = createByteBuffer(info);
    const tagBuf = createByteBuffer(tag);
    const outPtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_derive_ecdh_1pu(algorithm, senderPriv.handle, recipientPub.handle, senderPub.handle, apuBuf, apvBuf, infoBuf, tagBuf, ephemeral ? 1 : 0, outPtr);
    checkResult(rc);
    const handle = koffi.decode(outPtr, 'size_t');
    return new LocalKey(handle);
  }
}
