import * as koffi from 'koffi';
import {
  StoreCallback,
  SimpleCallback,
  SessionCallback,
  StringCallback,
  CountCallback,
  RemoveCallback,
  EntryListCallback,
  KeyEntryListCallback,
  StringListCallback,
  askarLib,
} from './ffi';

// Global registered callbacks
let registeredStoreCallback: any = null;
let registeredSimpleCallback: any = null;
let registeredSessionCallback: any = null;
let registeredStringCallback: any = null;
let registeredCountCallback: any = null;
let registeredRemoveCallback: any = null;
let registeredEntryListCallback: any = null;
let registeredKeyEntryListCallback: any = null;
let registeredStringListCallback: any = null;

// Initialize callbacks once
export function initializeCallbacks() {
  if (!registeredStoreCallback) {
    registeredStoreCallback = koffi.register(storeCallbackHandler, StoreCallback);
  }
  if (!registeredSimpleCallback) {
    registeredSimpleCallback = koffi.register(simpleCallbackHandler, SimpleCallback);
  }
  if (!registeredSessionCallback) {
    registeredSessionCallback = koffi.register(sessionCallbackHandler, SessionCallback);
  }
  if (!registeredStringCallback) {
    registeredStringCallback = koffi.register(stringCallbackHandler, StringCallback);
  }
  if (!registeredCountCallback) {
    registeredCountCallback = koffi.register(countCallbackHandler, CountCallback);
  }
  if (!registeredRemoveCallback) {
    registeredRemoveCallback = koffi.register(removeCallbackHandler, RemoveCallback);
  }
  if (!registeredEntryListCallback) {
    registeredEntryListCallback = koffi.register(entryListCallbackHandler, EntryListCallback);
  }
  if (!registeredKeyEntryListCallback) {
    registeredKeyEntryListCallback = koffi.register(entryListCallbackHandler, KeyEntryListCallback);
  }
  if (!registeredStringListCallback) {
    registeredStringListCallback = koffi.register(stringListCallbackHandler, StringListCallback);
  }
}

// Map to store callback handlers by ID
const callbackHandlers = new Map<number, (cbId: number, errorCode: number, ...args: any[]) => void>();

// Callback handlers that delegate to the registered handlers
function storeCallbackHandler(cbId: number, errorCode: number, handle: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, handle);
  }
}

function simpleCallbackHandler(cbId: number, errorCode: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode);
  }
}

function sessionCallbackHandler(cbId: number, errorCode: number, handle: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, handle);
  }
}

function stringCallbackHandler(cbId: number, errorCode: number, resultPtr: any) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    let result: string | null = null;
    if (resultPtr) {
      try {
        result = require('koffi').decode(resultPtr, 'str') || '';
      } finally {
        askarLib.askar_string_free(resultPtr);
      }
    }
    handler(cbId, errorCode, result || '');
  }
}

function countCallbackHandler(cbId: number, errorCode: number, count: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, count);
  }
}

function removeCallbackHandler(cbId: number, errorCode: number, removed: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, removed);
  }
}

function entryListCallbackHandler(cbId: number, errorCode: number, handle: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, handle);
  }
}

function stringListCallbackHandler(cbId: number, errorCode: number, handle: number) {
  const handler = callbackHandlers.get(cbId);
  if (handler) {
    handler(cbId, errorCode, handle);
  }
}

// Public API
export function registerCallbackHandler(
  callbackId: number,
  handler: (cbId: number, errorCode: number, ...args: any[]) => void
) {
  callbackHandlers.set(callbackId, handler);
}

export function unregisterCallbackHandler(callbackId: number) {
  callbackHandlers.delete(callbackId);
}

export function getStoreCallback() {
  initializeCallbacks();
  return registeredStoreCallback;
}

export function getSimpleCallback() {
  initializeCallbacks();
  return registeredSimpleCallback;
}

export function getSessionCallback() {
  initializeCallbacks();
  return registeredSessionCallback;
}

export function getStringCallback() {
  initializeCallbacks();
  return registeredStringCallback;
}

export function getCountCallback() {
  initializeCallbacks();
  return registeredCountCallback;
}

export function getRemoveCallback() {
  initializeCallbacks();
  return registeredRemoveCallback;
}

export function getEntryListCallback() {
  initializeCallbacks();
  return registeredEntryListCallback;
}

export function getKeyEntryListCallback() {
  initializeCallbacks();
  return registeredKeyEntryListCallback;
}

export function getStringListCallback() {
  initializeCallbacks();
  return registeredStringListCallback;
}
