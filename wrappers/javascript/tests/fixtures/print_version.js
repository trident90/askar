const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { Askar } = require('../../lib');
process.stdout.write(String(Askar.getVersion() || ''));
