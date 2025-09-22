import * as koffi from 'koffi';
import { askarLib, StrBuffer } from './ffi';
import {
  checkResult,
  createPromiseCallback,
  resolveCallback,
  rejectCallback,
  AskarError,
} from './utils';
import { ErrorCode } from './types';

export interface AskarNative {
  version(): string;
  storeProvision(
    uri: string,
    keyMethod?: string,
    passKey?: string,
    profile?: string,
    recreate?: boolean
  ): Promise<number>;
  storeOpen(uri: string, keyMethod?: string, passKey?: string, profile?: string): Promise<number>;
  storeClose(handle: number): Promise<void>;
  keyGenerate(algorithm: string, backend?: string, ephemeral?: boolean): Promise<number>;
  keyGetAlgorithm(handle: number): Promise<string>;
  keyFree(handle: number): void;
}

class KoffiAskarNative implements AskarNative {
  version(): string {
    const v = askarLib.askar_version();
    return v || '';
  }

  async storeProvision(
    uri: string,
    keyMethod?: string,
    passKey?: string,
    profile?: string,
    recreate: boolean = false
  ): Promise<number> {
    const [callbackId, promise] = createPromiseCallback<number>();

    const callback = (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, handle);
      }
    };

    const rc = askarLib.askar_store_provision(
      uri,
      keyMethod ?? null,
      passKey ?? null,
      profile ?? null,
      recreate ? 1 : 0,
      callback,
      callbackId
    );
    checkResult(rc);
    return promise;
  }

  async storeOpen(uri: string, keyMethod?: string, passKey?: string, profile?: string): Promise<number> {
    const [callbackId, promise] = createPromiseCallback<number>();

    const callback = (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, handle);
      }
    };

    const rc = askarLib.askar_store_open(
      uri,
      keyMethod ?? null,
      passKey ?? null,
      profile ?? null,
      callback,
      callbackId
    );
    checkResult(rc);
    return promise;
  }

  async storeClose(handle: number): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();

    const callback = (cbId: number, errorCode: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    };

    const rc = askarLib.askar_store_close(handle, callback, callbackId);
    checkResult(rc);
    return promise;
  }

  async keyGenerate(algorithm: string, backend?: string, ephemeral: boolean = false): Promise<number> {
    const handlePtr = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_key_generate(algorithm, backend ?? null, ephemeral ? 1 : 0, handlePtr);
    checkResult(rc);
    const handle = koffi.decode(handlePtr, 'size_t');
    return handle;
  }

  async keyGetAlgorithm(handle: number): Promise<string> {
    const s = askarLib.askar_key_get_algorithm_str(handle) || '';
    return s;
  }

  keyFree(handle: number): void {
    askarLib.askar_key_free(handle);
  }
}

export const askarNative: AskarNative = new KoffiAskarNative();
