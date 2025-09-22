import * as koffi from 'koffi';
import { askarLib, SecretBuffer } from './ffi';
import { checkResult, createPromiseCallback, resolveCallback, rejectCallback, AskarError, allocCString, readCString, createByteBuffer, readSecretBuffer } from './utils';
import { registerCallbackHandler, getEntryListCallback, getSimpleCallback, getCountCallback, getSessionCallback, getRemoveCallback, getKeyEntryListCallback } from './callbacks';
import { ErrorCode, Entry, KeyEntry, EntryOperation } from './types';
import { LocalKey } from './key';

export class Session {
  private handle: number;

  constructor(handle: number) {
    this.handle = handle;
  }

  async close(commit: boolean = true): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId, errorCode) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const result = askarLib.askar_session_close(
      this.handle,
      commit ? 1 : 0,
      getSimpleCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  closeSync(commit: boolean = true): void {
    const rc = askarLib.askar_session_close_sync(this.handle, commit ? 1 : 0);
    checkResult(rc);
  }

  async count(category?: string, tagFilter?: string | Record<string, string>): Promise<number> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, count: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, count);
      }
    });

    const filterStr = typeof tagFilter === 'string' ? tagFilter :
                     tagFilter ? JSON.stringify(tagFilter) : null;

    const result = askarLib.askar_session_count(
      this.handle,
      allocCString(category || null),
      allocCString(filterStr),
      getCountCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  countSync(category?: string, tagFilter?: string | Record<string, string>): number {
    const out = koffi.alloc('int64', 1);
    const filterStr = typeof tagFilter === 'string' ? tagFilter : tagFilter ? JSON.stringify(tagFilter) : null;
    const rc = askarLib.askar_session_count_sync(
      this.handle,
      allocCString(category || null),
      allocCString(filterStr),
      out
    );
    checkResult(rc);
    return Number(koffi.decode(out, 'int64'));
  }

  async fetch(category: string, name: string, forUpdate: boolean = false): Promise<Entry | null> {
    const [callbackId, promise] = createPromiseCallback<Entry | null>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
        return;
      }

      try {
        const entries = this.readEntryList(handle);
        askarLib.askar_entry_list_free(handle);
        resolveCallback(cbId, entries.length > 0 ? entries[0] : null);
      } catch (error) {
        rejectCallback(cbId, error as Error);
      }
    });

    const result = askarLib.askar_session_fetch(
      this.handle,
      allocCString(category),
      allocCString(name),
      forUpdate ? 1 : 0,
      getEntryListCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  fetchSync(category: string, name: string, forUpdate: boolean = false): Entry | null {
    const out = koffi.alloc('size_t', 1);
    const rc = askarLib.askar_session_fetch_sync(
      this.handle,
      allocCString(category),
      allocCString(name),
      forUpdate ? 1 : 0,
      out
    );
    checkResult(rc);
    const list = koffi.decode(out, 'size_t');
    const entries = this.readEntryList(list);
    askarLib.askar_entry_list_free(list);
    return entries.length > 0 ? entries[0] : null;
  }

  async fetchAll(
    category?: string,
    tagFilter?: string | Record<string, string>,
    limit?: number,
    orderBy?: string,
    descending: boolean = false,
    forUpdate: boolean = false
  ): Promise<Entry[]> {
    const [callbackId, promise] = createPromiseCallback<Entry[]>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, handle: number) => {
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
    });

    const filterStr = typeof tagFilter === 'string' ? tagFilter :
                     tagFilter ? JSON.stringify(tagFilter) : null;

    const result = askarLib.askar_session_fetch_all(
      this.handle,
      allocCString(category || null),
      allocCString(filterStr),
      limit || -1,
      allocCString(orderBy || null),
      descending ? 1 : 0,
      forUpdate ? 1 : 0,
      getEntryListCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  fetchAllSync(
    category?: string,
    tagFilter?: string | Record<string, string>,
    limit?: number,
    orderBy?: string,
    descending: boolean = false,
    forUpdate: boolean = false
  ): Entry[] {
    const out = koffi.alloc('size_t', 1);
    const filterStr = typeof tagFilter === 'string' ? tagFilter : tagFilter ? JSON.stringify(tagFilter) : null;
    const rc = askarLib.askar_session_fetch_all_sync(
      this.handle,
      allocCString(category || null),
      allocCString(filterStr),
      limit || -1,
      allocCString(orderBy || null),
      descending ? 1 : 0,
      forUpdate ? 1 : 0,
      out
    );
    checkResult(rc);
    const list = koffi.decode(out, 'size_t');
    const entries = this.readEntryList(list);
    askarLib.askar_entry_list_free(list);
    return entries;
  }

  async removeAll(
    category?: string,
    tagFilter?: string | Record<string, string>
  ): Promise<number> {
    const [callbackId, promise] = createPromiseCallback<number>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, removed: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, removed);
      }
    });

    const filterStr = typeof tagFilter === 'string' ? tagFilter :
                     tagFilter ? JSON.stringify(tagFilter) : null;

    const result = askarLib.askar_session_remove_all(
      this.handle,
      allocCString(category || null),
      allocCString(filterStr),
      getCountCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async update(
    operation: EntryOperation,
    category: string,
    name: string,
    value?: Buffer | string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const valueBuffer = value ? (typeof value === 'string' ? Buffer.from(value, 'utf8') : value) : null;
    const valueBuf = createByteBuffer(valueBuffer);
    const tagsStr = tags ? JSON.stringify(tags) : null;

    const result = askarLib.askar_session_update(
      this.handle,
      operation,
      allocCString(category),
      allocCString(name),
      valueBuf,
      allocCString(tagsStr),
      expiryMs || -1,
      getSimpleCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async insert(
    category: string,
    name: string,
    value: Buffer | string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): Promise<void> {
    return this.update(EntryOperation.Insert, category, name, value, tags, expiryMs);
  }

  insertSync(
    category: string,
    name: string,
    value: Buffer | string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): void {
    const tagsStr = tags ? JSON.stringify(tags) : null;
    const rc = askarLib.askar_session_update_sync(
      this.handle,
      0, // Insert
      allocCString(category),
      allocCString(name),
      createByteBuffer(value),
      allocCString(tagsStr),
      expiryMs ?? -1
    );
    checkResult(rc);
  }

  async replace(
    category: string,
    name: string,
    value: Buffer | string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): Promise<void> {
    return this.update(EntryOperation.Replace, category, name, value, tags, expiryMs);
  }

  replaceSync(
    category: string,
    name: string,
    value: Buffer | string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): void {
    const tagsStr = tags ? JSON.stringify(tags) : null;
    const rc = askarLib.askar_session_update_sync(
      this.handle,
      1, // Replace
      allocCString(category),
      allocCString(name),
      createByteBuffer(value),
      allocCString(tagsStr),
      expiryMs ?? -1
    );
    checkResult(rc);
  }

  async remove(
    category: string,
    name: string
  ): Promise<void> {
    return this.update(EntryOperation.Remove, category, name);
  }

  removeSync(category: string, name: string): void {
    const rc = askarLib.askar_session_update_sync(
      this.handle,
      2, // Remove
      allocCString(category),
      allocCString(name),
      createByteBuffer(null),
      allocCString(null),
      -1
    );
    checkResult(rc);
  }

  async insertKey(
    key: LocalKey,
    name: string,
    metadata?: string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const tagsStr = tags ? JSON.stringify(tags) : null;

    const result = askarLib.askar_session_insert_key(
      this.handle,
      key.handle,
      allocCString(name),
      allocCString(metadata || null),
      allocCString(tagsStr),
      expiryMs || -1,
      getSimpleCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async fetchKey(name: string, forUpdate: boolean = false): Promise<KeyEntry | null> {
    const [callbackId, promise] = createPromiseCallback<KeyEntry | null>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
        return;
      }

      try {
        const entries = this.readKeyEntryList(handle);
        askarLib.askar_key_entry_list_free(handle);
        resolveCallback(cbId, entries.length > 0 ? entries[0] : null);
      } catch (error) {
        rejectCallback(cbId, error as Error);
      }
    });

    const result = askarLib.askar_session_fetch_key(
      this.handle,
      allocCString(name),
      forUpdate ? 1 : 0,
      getKeyEntryListCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async fetchAllKeys(
    algorithm?: string,
    thumbprint?: string,
    tagFilter?: string | Record<string, string>,
    limit?: number,
    forUpdate: boolean = false
  ): Promise<KeyEntry[]> {
    const [callbackId, promise] = createPromiseCallback<KeyEntry[]>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number, handle: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
        return;
      }

      try {
        const entries = this.readKeyEntryList(handle);
        askarLib.askar_key_entry_list_free(handle);
        resolveCallback(cbId, entries);
      } catch (error) {
        rejectCallback(cbId, error as Error);
      }
    });

    const filterStr = typeof tagFilter === 'string' ? tagFilter :
                     tagFilter ? JSON.stringify(tagFilter) : null;

    const result = askarLib.askar_session_fetch_all_keys(
      this.handle,
      allocCString(algorithm || null),
      allocCString(thumbprint || null),
      allocCString(filterStr),
      limit || -1,
      forUpdate ? 1 : 0,
      getKeyEntryListCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async updateKey(
    name: string,
    metadata?: string,
    tags?: Record<string, string>,
    expiryMs?: number
  ): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const tagsStr = tags ? JSON.stringify(tags) : null;

    const result = askarLib.askar_session_update_key(
      this.handle,
      allocCString(name),
      allocCString(metadata || null),
      allocCString(tagsStr),
      expiryMs || -1,
      getSimpleCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  async removeKey(name: string): Promise<void> {
    const [callbackId, promise] = createPromiseCallback<void>();
    registerCallbackHandler(callbackId, (cbId: number, errorCode: number) => {
      if (errorCode !== ErrorCode.Success) {
        rejectCallback(cbId, AskarError.fromCode(errorCode));
      } else {
        resolveCallback(cbId, undefined);
      }
    });

    const result = askarLib.askar_session_remove_key(
      this.handle,
      allocCString(name),
      getSimpleCallback(),
      callbackId
    );

    checkResult(result);
    return promise;
  }

  private readEntryList(handle: number): Entry[] {
    const countPtr = koffi.alloc('int32', 1);
    checkResult(askarLib.askar_entry_list_count(handle, countPtr));
    const count = koffi.decode(countPtr, 'int32');

    const entries: Entry[] = [];
    for (let i = 0; i < count; i++) {
      const categorySB = koffi.alloc(require('./ffi').StrBuffer, 1);
      const nameSB = koffi.alloc(require('./ffi').StrBuffer, 1);
      const valueBuf = koffi.alloc(SecretBuffer, 1);
      const tagsSB = koffi.alloc(require('./ffi').StrBuffer, 1);

      checkResult(askarLib.askar_entry_list_get_category(handle, i, categorySB));
      checkResult(askarLib.askar_entry_list_get_name(handle, i, nameSB));
      checkResult(askarLib.askar_entry_list_get_value(handle, i, valueBuf));
      checkResult(askarLib.askar_entry_list_get_tags(handle, i, tagsSB));

      const categoryBuf = koffi.decode(categorySB, require('./ffi').StrBuffer) as any;
      const nameBuf = koffi.decode(nameSB, require('./ffi').StrBuffer) as any;
      const tagsBuf = koffi.decode(tagsSB, require('./ffi').StrBuffer) as any;

      const category = categoryBuf.buffer ? koffi.decode(categoryBuf.buffer, 'str') : '';
      const name = nameBuf.buffer ? koffi.decode(nameBuf.buffer, 'str') : '';
      const value = readSecretBuffer(koffi.decode(valueBuf, SecretBuffer));
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

  private readKeyEntryList(handle: number): KeyEntry[] {
    const countPtr = koffi.alloc('int32', 1);
    checkResult(askarLib.askar_key_entry_list_count(handle, countPtr));
    const count = koffi.decode(countPtr, 'int32');

    const entries: KeyEntry[] = [];
    for (let i = 0; i < count; i++) {
      const nameSB2 = koffi.alloc(require('./ffi').StrBuffer, 1);
      const algSB2 = koffi.alloc(require('./ffi').StrBuffer, 1);
      const metaSB2 = koffi.alloc(require('./ffi').StrBuffer, 1);
      const tagsSB2 = koffi.alloc(require('./ffi').StrBuffer, 1);

      checkResult(askarLib.askar_key_entry_list_get_name(handle, i, nameSB2));
      checkResult(askarLib.askar_key_entry_list_get_algorithm(handle, i, algSB2));
      checkResult(askarLib.askar_key_entry_list_get_metadata(handle, i, metaSB2));
      checkResult(askarLib.askar_key_entry_list_get_tags(handle, i, tagsSB2));

      const nameBuf2 = koffi.decode(nameSB2, require('./ffi').StrBuffer) as any;
      const algBuf2 = koffi.decode(algSB2, require('./ffi').StrBuffer) as any;
      const metaBuf2 = koffi.decode(metaSB2, require('./ffi').StrBuffer) as any;
      const tagsBuf2 = koffi.decode(tagsSB2, require('./ffi').StrBuffer) as any;

      const name = nameBuf2.buffer ? koffi.decode(nameBuf2.buffer, 'str') : '';
      const algorithm = algBuf2.buffer ? koffi.decode(algBuf2.buffer, 'str') : '';
      const metadata = metaBuf2.buffer ? koffi.decode(metaBuf2.buffer, 'str') : null;
      const tagsStr = tagsBuf2.buffer ? koffi.decode(tagsBuf2.buffer, 'str') : null;

      if (nameBuf2.buffer) askarLib.askar_string_free(nameBuf2.buffer);
      if (algBuf2.buffer) askarLib.askar_string_free(algBuf2.buffer);
      if (metaBuf2.buffer) askarLib.askar_string_free(metaBuf2.buffer);
      if (tagsBuf2.buffer) askarLib.askar_string_free(tagsBuf2.buffer);

      let tags: Record<string, string> | undefined;
      if (tagsStr) {
        try {
          tags = JSON.parse(tagsStr);
        } catch {
          // Invalid JSON, ignore tags
        }
      }

      entries.push({ name, algorithm, metadata, tags });
    }

    return entries;
  }
}
