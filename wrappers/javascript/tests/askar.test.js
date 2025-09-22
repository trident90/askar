const path = require('path');
const { execFile } = require('child_process');

function getLibPath() {
  const plat = process.platform;
  const name = plat === 'win32' ? 'aries_askar.dll' : plat === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so';
  return path.join(__dirname, '..', '..', '..', 'target', 'release', name);
}

function runNode(script) {
  return new Promise((resolve, reject) => {
    execFile(process.execPath, [script], {
      cwd: path.join(__dirname, '..'),
      env: { ...process.env, ASKAR_LIB_PATH: getLibPath() },
    }, (err, stdout, stderr) => {
      if (err) return reject(err);
      resolve({ stdout: stdout.trim(), stderr: stderr.trim() });
    });
  });
}

describe('Aries Askar Koffi Wrapper (lib)', () => {
  test('version is non-empty', async () => {
    const { stdout } = await runNode(path.join(__dirname, 'fixtures', 'print_version.js'));
    expect(stdout.length).toBeGreaterThan(0);
  });

  test('generate key and get algorithm', async () => {
    const { stdout } = await runNode(path.join(__dirname, 'fixtures', 'key_algorithm.js'));
    expect(stdout).toBe('ed25519');
  });

  test('sign/verify with Ed25519', async () => {
    const { stdout } = await runNode(path.join(__dirname, 'fixtures', 'sign_verify.js'));
    expect(stdout).toBe('ok');
  });
});
