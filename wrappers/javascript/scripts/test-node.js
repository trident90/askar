#!/usr/bin/env node
const { execFile } = require('child_process');
const path = require('path');

function getLibName() {
  if (process.platform === 'win32') return 'aries_askar.dll';
  if (process.platform === 'darwin') return 'libaries_askar.dylib';
  return 'libaries_askar.so';
}

function getLibPath() {
  return path.join(__dirname, '..', '..', '..', 'target', 'release', getLibName());
}

function runFixture(relScript, validate) {
  return new Promise((resolve) => {
    const script = path.join(__dirname, '..', 'tests', 'fixtures', relScript);
    const env = { ...process.env, ASKAR_LIB_PATH: getLibPath() };
    execFile(process.execPath, [script], { cwd: path.join(__dirname, '..'), env }, (err, stdout, stderr) => {
      const out = (stdout || '').trim();
      const errOut = (stderr || '').trim();
      let ok = !err;
      let reason = '';
      if (ok && validate) {
        try { ok = !!validate(out); } catch (e) { ok = false; reason = String(e && e.message || e); }
      }
      if (!ok && !reason) reason = err ? (errOut || String(err)) : 'Validation failed';
      resolve({ ok, out, err: reason });
    });
  });
}

async function main() {
  const cases = [
    {
      name: 'version',
      script: 'print_version.js',
      validate: (out) => typeof out === 'string' && out.length > 0,
    },
    {
      name: 'key algorithm',
      script: 'key_algorithm.js',
      validate: (out) => out === 'ed25519',
    },
    {
      name: 'sign/verify',
      script: 'sign_verify.js',
      validate: (out) => out === 'ok',
    },
    {
      name: 'store/session insert+fetch',
      script: 'store_session.js',
      validate: (out) => out === 'ok',
    },
    {
      name: 'profiles create/list/remove',
      script: 'profiles.js',
      validate: (out) => out === 'ok',
    },
    {
      name: 'count/fetchAll',
      script: 'count_fetch_all.js',
      validate: (out) => out === 'ok',
    },
    {
      name: 'AEAD encrypt/decrypt',
      script: 'aead.js',
      validate: (out) => out === 'ok',
    },
    {
      name: 'crypto box',
      script: 'crypto_box.js',
      validate: (out) => out === 'ok',
    },
  ];

  let passed = 0;
  for (const c of cases) {
    process.stdout.write(`- ${c.name}... `);
    // eslint-disable-next-line no-await-in-loop
    const res = await runFixture(c.script, c.validate);
    if (res.ok) {
      passed += 1;
      console.log('ok');
    } else {
      console.log('FAIL');
      if (res.out) console.log(`  stdout: ${res.out}`);
      if (res.err) console.log(`  error: ${res.err}`);
    }
  }

  const total = cases.length;
  console.log(`\nSummary: ${passed}/${total} passed`);
  process.exit(passed === total ? 0 : 1);
}

main().catch((e) => { console.error(e && e.stack || e); process.exit(1); });
