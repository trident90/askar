import * as koffi from 'koffi';
import {
  askarLib,
  EntryListCallback,
  StoreCallback,
  SecretBuffer,
} from './ffi';
import {
  checkResult,
  createPromiseCallback,
  resolveCallback,
  rejectCallback,
  AskarError,
  readSecretBuffer,
} from './utils';
import { ErrorCode, Entry } from './types';

export class Scan {
  private handle: number;

  constructor(handle: number) {
    this.handle = handle;
  }

  async next(): Promise<Entry[]> {
    const [callbackId, promise] = createPromiseCallback<Entry[]>();

    const callback = (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
        return;
      }

      try {
        const entries = this.readEntryList(handle);
        askarLib.askar_entry_list_free(handle);
        resolveCallback(cbId, entries);
      } catch (error) {
        rejectCallback(cbId, error as Error);
      }
    };

    const result = askarLib.askar_scan_next(this.handle, callback, callbackId);
    checkResult(result);
    return promise;
  }

  free(): void {
    checkResult(askarLib.askar_scan_free(this.handle));
  }

  private readEntryList(handle: number): Entry[] {
    const countPtr = koffi.alloc('int32', 1);
    checkResult(askarLib.askar_entry_list_count(handle, countPtr));
    const count = koffi.decode(countPtr, 'int32');

    const entries: Entry[] = [];
    for (let i = 0; i < count; i++) {
      const categorySB = koffi.alloc(require('./ffi').StrBuffer, 1);
      const nameSB = koffi.alloc(require('./ffi').StrBuffer, 1);
      const valuePtr = koffi.alloc(SecretBuffer, 1);
      const tagsSB = koffi.alloc(require('./ffi').StrBuffer, 1);

      checkResult(askarLib.askar_entry_list_get_category(handle, i, categorySB));
      checkResult(askarLib.askar_entry_list_get_name(handle, i, nameSB));
      checkResult(askarLib.askar_entry_list_get_value(handle, i, valuePtr));
      checkResult(askarLib.askar_entry_list_get_tags(handle, i, tagsSB));

      const categoryBuf = koffi.decode(categorySB, require('./ffi').StrBuffer) as any;
      const nameBuf = koffi.decode(nameSB, require('./ffi').StrBuffer) as any;
      const tagsBuf = koffi.decode(tagsSB, require('./ffi').StrBuffer) as any;

      const category = categoryBuf.buffer ? koffi.decode(categoryBuf.buffer, 'str') : '';
      const name = nameBuf.buffer ? koffi.decode(nameBuf.buffer, 'str') : '';
      const value = readSecretBuffer(koffi.decode(valuePtr, SecretBuffer));
      const tagsStr = tagsBuf.buffer ? koffi.decode(tagsBuf.buffer, 'str') : null;

      if (categoryBuf.buffer) askarLib.askar_string_free(categoryBuf.buffer);
      if (nameBuf.buffer) askarLib.askar_string_free(nameBuf.buffer);
      if (tagsBuf.buffer) askarLib.askar_string_free(tagsBuf.buffer);

      let tags: Record<string, string> | undefined;
      if (tagsStr) {
        try {
          tags = JSON.parse(tagsStr);
        } catch {
          // Invalid JSON, ignore tags
        }
      }

      entries.push({ category, name, value, tags });
    }

    return entries;
  }
}
