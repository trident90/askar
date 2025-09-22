import * as koffi from 'koffi';
import { ErrorCode } from './types';
import { askarLib, SecretBuffer, ByteBuffer } from './ffi';

export class AskarError extends Error {
  constructor(
    public code: ErrorCode,
    message: string
  ) {
    super(`Askar error ${code}: ${message}`);
    this.name = 'AskarError';
  }

  static fromCode(code: ErrorCode): AskarError {
    const sbPtr: any = koffi.alloc(require('./ffi').StrBuffer, 1);
    const result = askarLib.askar_get_current_error(sbPtr);

    let message = `Error code ${code}`;
    if (result === ErrorCode.Success) {
      const sb = koffi.decode(sbPtr, require('./ffi').StrBuffer) as any;
      const str = sb && sb.buffer ? koffi.decode(sb.buffer, 'str') : null;
      if (sb && sb.buffer) askarLib.askar_string_free(sb.buffer);
      if (str) {
        try {
          const errorInfo = JSON.parse(str);
          message = errorInfo.message || message;
        } catch {
          message = str;
        }
      }
    }

    return new AskarError(code, message);
  }
}

export function checkResult(code: number): void {
  if (code !== ErrorCode.Success) {
    throw AskarError.fromCode(code);
  }
}

export function createByteBuffer(data: Buffer | string | null): any {
  if (data === null || data === undefined) {
    return {
      len: 0,
      data: null,
    };
  }

  const buffer = Buffer.isBuffer(data) ? data : Buffer.from(data, 'utf8');

  return {
    len: buffer.length,
    data: buffer,
  };
}

export function readSecretBuffer(secretBuf: any): Buffer {
  if (!secretBuf || secretBuf.len === 0 || !secretBuf.data) {
    return Buffer.alloc(0);
  }

  return Buffer.from(koffi.decode(secretBuf.data, koffi.array('uint8', secretBuf.len)));
}

export function readByteBuffer(byteBuf: any): Buffer {
  if (!byteBuf || byteBuf.len === 0 || !byteBuf.data) {
    return Buffer.alloc(0);
  }

  return Buffer.from(koffi.decode(byteBuf.data, koffi.array('uint8', byteBuf.len)));
}

export function freeSecretBuffer(secretBuf: any): void {
  if (secretBuf && secretBuf.data) {
    askarLib.askar_buffer_free(secretBuf);
  }
}

export function readCString(ptr: any): string | null {
  if (!ptr) {
    return null;
  }
  return koffi.decode(ptr, 'str');
}

export function allocCString(str: string | null): any {
  if (str === null || str === undefined) {
    return null;
  }
  return str; // Koffi handles string conversion automatically
}

let callbackIdCounter = 1;

export function generateCallbackId(): number {
  return callbackIdCounter++;
}

export interface PromiseCallbacks<T = void> {
  resolve: (value: T) => void;
  reject: (reason: any) => void;
}

export const pendingCallbacks = new Map<number, PromiseCallbacks<any>>();

export function createPromiseCallback<T = void>(): [number, Promise<T>] {
  const callbackId = generateCallbackId();

  const promise = new Promise<T>((resolve, reject) => {
    pendingCallbacks.set(callbackId, { resolve, reject });
  });

  return [callbackId, promise];
}

export function resolveCallback<T>(callbackId: number, value: T): void {
  const callbacks = pendingCallbacks.get(callbackId);
  if (callbacks) {
    pendingCallbacks.delete(callbackId);
    callbacks.resolve(value);
  }
}

export function rejectCallback(callbackId: number, error: Error): void {
  const callbacks = pendingCallbacks.get(callbackId);
  if (callbacks) {
    pendingCallbacks.delete(callbackId);
    callbacks.reject(error);
  }
}

export function cleanup(): void {
  askarLib.askar_terminate();
}

// Cleanup on process exit
process.on('exit', cleanup);
process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);
