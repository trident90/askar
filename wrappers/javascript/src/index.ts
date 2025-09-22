export * from './types';
export * from './utils';
export * from './store';
export * from './session';
export * from './scan';
export * from './key';

import { askarLib } from './ffi';
import { checkResult } from './utils';

export class Askar {
  static setDefaultLogger(): void {
    checkResult(askarLib.askar_set_default_logger());
  }

  static setMaxLogLevel(level: number): void {
    checkResult(askarLib.askar_set_max_log_level(level));
  }

  static clearCustomLogger(): void {
    askarLib.askar_clear_custom_logger();
  }

  static getVersion(): string {
    const version = askarLib.askar_version();
    return version || '';
  }

  static terminate(): void {
    askarLib.askar_terminate();
  }
}

// Re-export main classes for convenience
export { Store } from './store';
export { Session } from './session';
export { Scan } from './scan';
export { LocalKey } from './key';