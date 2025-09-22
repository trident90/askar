import * as koffi from 'koffi';
import { askarNative } from './native';
import { ErrorCode } from './types';
import { Session } from './session';
import { askarLib } from './ffi';
import { checkResult, createPromiseCallback, resolveCallback, rejectCallback, AskarError, allocCString } from './utils';
import { registerCallbackHandler, getStoreCallback, getSimpleCallback, getSessionCallback, getStringCallback, getStringListCallback, getRemoveCallback } from './callbacks';

export class Store {
  private handle: number;

  private constructor(handle: number) {
    this.handle = handle;
  }

  static fromHandle(handle: number): Store {
    return new Store(handle);
  }

  static async provision(
    uri: string,
    keyMethod?: string,
    passKey?: string,
    profile?: string,
    recreate: boolean = false
  ): Promise<Store> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId, errorCode, handle) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, handle);
      }
    });

    const rc = askarLib.askar_store_provision(
      uri,
      keyMethod ?? null,
      passKey ?? null,
      profile ?? null,
      recreate ? 1 : 0,
      getStoreCallback(),
      callbackId
    );
    checkResult(rc);
    const handle = await promise;
    return new Store(handle);
  }

  static async open(
    uri: string,
    keyMethod?: string,
    passKey?: string,
    profile?: string
  ): Promise<Store> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId, errorCode, handle) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, handle);
      }
    });

    const rc = askarLib.askar_store_open(
      uri,
      keyMethod ?? null,
      passKey ?? null,
      profile ?? null,
      getStoreCallback(),
      callbackId
    );
    checkResult(rc);
    const handle = await promise;
    return new Store(handle);
  }

  async close(): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId, errorCode) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, undefined);
      }
    });
    const rc = askarLib.askar_store_close(this.handle, getSimpleCallback(), callbackId);
    checkResult(rc);
    await promise;
  }

  closeSync(): void {
    const rc = askarLib.askar_store_close_sync(this.handle);
    checkResult(rc);
  }

  async startSession(profile?: string, asTransaction: boolean = false): Promise<Session> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId, errorCode, handle) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, handle);
      }
    });

    const rc = askarLib.askar_session_start(
      this.handle,
      allocCString(profile || null),
      asTransaction ? 1 : 0,
      getSessionCallback(),
      callbackId
    );

    checkResult(rc);
    const sessionHandle = await promise;
    return new Session(sessionHandle);
  }

  startSessionSync(profile?: string, asTransaction: boolean = false): Session {
    const out = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_session_start_sync(
      this.handle,
      allocCString(profile || null),
      asTransaction ? 1 : 0,
      out
    );
    checkResult(rc);
    const handle = koffi.decode(out, 'size_t');
    return new Session(handle);
  }

  static getVersion(): string {
    return askarNative.version();
  }

  async rekey(keyMethod?: string, passKey?: string): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId, errorCode) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const rc = askarLib.askar_store_rekey(
      this.handle,
      keyMethod ?? null,
      passKey ?? null,
      getSimpleCallback(),
      callbackId
    );
    checkResult(rc);
    await promise;
  }

  async copy(targetUri: string, keyMethod?: string, passKey?: string, recreate: boolean = false): Promise<Store> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId, errorCode, handle) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, handle);
      }
    });

    const rc = askarLib.askar_store_copy(
      this.handle,
      targetUri,
      keyMethod ?? null,
      passKey ?? null,
      recreate ? 1 : 0,
      getStoreCallback(),
      callbackId
    );
    checkResult(rc);
    const newHandle = await promise;
    return new Store(newHandle);
  }

  static async remove(uri: string): Promise<boolean> {
    const [callbackId, promise] = createPromiseCallback<boolean>();
    registerCallbackHandler(callbackId, (cbId, errorCode, removed) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, !!removed);
      }
    });

    const rc = askarLib.askar_store_remove(
      uri,
      getRemoveCallback(),
      callbackId
    );
    checkResult(rc);
    return await promise;
  }

  async createProfile(profile?: string | null): Promise<string> {
    const [callbackId, promise] = createPromiseCallback<string>();
    registerCallbackHandler(callbackId, (cbId, errorCode, name) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, name || '');
      }
    });

    const rc = askarLib.askar_store_create_profile(
      this.handle,
      profile ?? null,
      getStringCallback(),
      callbackId
    );
    checkResult(rc);
    return await promise;
  }

  async getProfileName(): Promise<string> {
    const [callbackId, promise] = createPromiseCallback<string>();
    registerCallbackHandler(callbackId, (cbId, errorCode, name) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, name || '');
      }
    });

    const rc = askarLib.askar_store_get_profile_name(
      this.handle,
      getStringCallback(),
      callbackId
    );
    checkResult(rc);
    return await promise;
  }

  async getDefaultProfile(): Promise<string> {
    const [callbackId, promise] = createPromiseCallback<string>();
    registerCallbackHandler(callbackId, (cbId, errorCode, name) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, name || '');
      }
    });

    const rc = askarLib.askar_store_get_default_profile(
      this.handle,
      getStringCallback(),
      callbackId
    );
    checkResult(rc);
    return await promise;
  }

  async setDefaultProfile(profile: string): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId, errorCode) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const rc = askarLib.askar_store_set_default_profile(
      this.handle,
      profile,
      getSimpleCallback(),
      callbackId
    );
    checkResult(rc);
    await promise;
  }

  async listProfiles(): Promise<string[]> {
    // Prefer safe JSON sync path when available (single-attempt)
    try {
      if (process.env.ASKAR_LIST_JSON !== '0' && process.env.DISABLE_LIST_JSON !== '1') {
        const sbPtr: any = koffi.alloc(require('./ffi').StrBuffer, 1);
        const rc = askarLib.askar_store_list_profiles_json_sync(this.handle, sbPtr);
        if (rc === 0) {
          const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
          const json = sb && sb.buffer ? (koffi.decode(sb.buffer, 'str') || '[]') : '[]';
          if (sb && sb.buffer) askarLib.askar_string_free(sb.buffer);
          const arr = JSON.parse(json);
          if (Array.isArray(arr)) return arr.map((s) => String(s));
        }
      }
    } catch (_) {
      // fall back to callback-based path
    }

    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId, errorCode, handle) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, handle);
      }
    });

    const rc = askarLib.askar_store_list_profiles(
      this.handle,
      getStringListCallback(),
      callbackId
    );
    checkResult(rc);
    const listHandle = await promise;
    try {
      return this.readStringList(listHandle);
    } finally {
      askarLib.askar_string_list_free(listHandle);
    }
  }

  /**
   * More robust profile listing with retries and JSON-sync preference.
   * - Respects env overrides: set ASKAR_LIST_JSON=0 (or DISABLE_LIST_JSON=1) to skip JSON path.
   * - retries: number of JSON attempts before falling back (default 2)
   * - delayMs: delay between retries (default 30ms)
   */
  async listProfilesStable(retries = 2, delayMs = 30): Promise<string[]> {
    const tryJson = () => (process.env.ASKAR_LIST_JSON !== '0' && process.env.DISABLE_LIST_JSON !== '1');
    const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
    if (tryJson()) {
      for (let i = 0; i <= retries; i++) {
        try {
          const sbPtr: any = koffi.alloc(require('./ffi').StrBuffer, 1);
          const rc = askarLib.askar_store_list_profiles_json_sync(this.handle, sbPtr);
          if (rc === 0) {
            const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
            const json = sb && sb.buffer ? (koffi.decode(sb.buffer, 'str') || '[]') : '[]';
            if (sb && sb.buffer) askarLib.askar_string_free(sb.buffer);
            const arr = JSON.parse(json);
            if (Array.isArray(arr)) return arr.map((s) => String(s));
          }
        } catch (_) {
          // ignore and retry
        }
        if (i < retries) await sleep(delayMs);
      }
    }
    // Fallback to callback-based path
    return await this.listProfiles();
  }

  async removeProfile(profile: string): Promise<boolean> {
    const [callbackId, promise] = createPromiseCallback<boolean>();
    registerCallbackHandler(callbackId, (cbId, errorCode, removed) => {
      if (errorCode !== 0) {
        rejectCallback(cbId, AskarError.fromCode(errorCode as any));
      } else {
        resolveCallback(cbId, !!removed);
      }
    });

    const rc = askarLib.askar_store_remove_profile(
      this.handle,
      profile,
      getRemoveCallback(),
      callbackId
    );
    checkResult(rc);
    return await promise;
  }

  removeProfileSync(profile: string): boolean {
    const out = koffi.alloc('int8', 1);
    const rc = askarLib.askar_store_remove_profile_sync(this.handle, profile, out);
    checkResult(rc);
    return koffi.decode(out, 'int8') !== 0;
  }

  private readStringList(handle: number): string[] {
    const countPtr = koffi.alloc('int32', 1);
    checkResult(askarLib.askar_string_list_count(handle, countPtr));
    const count = koffi.decode(countPtr, 'int32');
    const items: string[] = [];
    for (let i = 0; i < count; i++) {
      const sbPtr = koffi.alloc(require('./ffi').StrBuffer, 1);
      checkResult(askarLib.askar_string_list_get_item(handle, i, sbPtr));
      const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
      const s = sb.buffer ? koffi.decode(sb.buffer, 'str') : '';
      if (sb.buffer) askarLib.askar_string_free(sb.buffer);
      items.push(s || '');
    }
    return items;
  }

  async profileExists(profile: string): Promise<boolean> {
    const out = koffi.alloc('int8', 1);
    const rc = askarLib.askar_store_profile_exists_sync(this.handle, profile, out);
    checkResult(rc);
    return koffi.decode(out, 'int8') !== 0;
  }

  async createProfileNoPtr(profile?: string): Promise<void> {
    const rc = askarLib.askar_store_create_profile_noptr_sync(this.handle, profile ?? null);
    checkResult(rc);
  }
}
