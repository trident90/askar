import * as koffi from 'koffi';
import { resolveCallback, rejectCallback, AskarError } from './utils';
import { ErrorCode } from './types';

// Define callback prototypes
const StoreCallbackProto = koffi.proto('void', ['int64', 'int64', 'size_t']);
const SimpleCallbackProto = koffi.proto('void', ['int64', 'int64']);
const SessionCallbackProto = koffi.proto('void', ['int64', 'int64', 'size_t']);
const StringCallbackProto = koffi.proto('void', ['int64', 'int64', 'str']);
const CountCallbackProto = koffi.proto('void', ['int64', 'int64', 'int64']);
const RemoveCallbackProto = koffi.proto('void', ['int64', 'int64', 'int8']);
const EntryListCallbackProto = koffi.proto('void', ['int64', 'int64', 'size_t']);
const StringListCallbackProto = koffi.proto('void', ['int64', 'int64', 'size_t']);
const ScanCallbackProto = koffi.proto('void', ['int64', 'int64', 'size_t']);

// Callback handlers using actual functions
function storeCallbackHandler(cbId: number, errorCode: number, handle: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, handle);
  }
}

function simpleCallbackHandler(cbId: number, errorCode: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, undefined);
  }
}

function sessionCallbackHandler(cbId: number, errorCode: number, handle: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, handle);
  }
}

function stringCallbackHandler(cbId: number, errorCode: number, result: string): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, result || '');
  }
}

function countCallbackHandler(cbId: number, errorCode: number, count: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, count);
  }
}

function removeCallbackHandler(cbId: number, errorCode: number, removed: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, removed !== 0);
  }
}

function entryListCallbackHandler(cbId: number, errorCode: number, handle: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, handle);
  }
}

function stringListCallbackHandler(cbId: number, errorCode: number, handle: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, handle);
  }
}

function scanCallbackHandler(cbId: number, errorCode: number, handle: number): void {
  if (errorCode !== ErrorCode.Success) {
    rejectCallback(cbId, AskarError.fromCode(errorCode));
  } else {
    resolveCallback(cbId, handle);
  }
}

// Just export the callback functions directly - koffi will handle registration when used
export const registeredStoreCallback = storeCallbackHandler;
export const registeredSimpleCallback = simpleCallbackHandler;
export const registeredSessionCallback = sessionCallbackHandler;
export const registeredStringCallback = stringCallbackHandler;
export const registeredCountCallback = countCallbackHandler;
export const registeredRemoveCallback = removeCallbackHandler;
export const registeredEntryListCallback = entryListCallbackHandler;
export const registeredStringListCallback = stringListCallbackHandler;
export const registeredScanCallback = scanCallbackHandler;

// Public API
export function getStoreCallback() {
  return registeredStoreCallback;
}

export function getSimpleCallback() {
  return registeredSimpleCallback;
}

export function getSessionCallback() {
  return registeredSessionCallback;
}

export function getStringCallback() {
  return registeredStringCallback;
}

export function getCountCallback() {
  return registeredCountCallback;
}

export function getRemoveCallback() {
  return registeredRemoveCallback;
}

export function getEntryListCallback() {
  return registeredEntryListCallback;
}

export function getStringListCallback() {
  return registeredStringListCallback;
}

export function getScanCallback() {
  return registeredScanCallback;
}