module.exports = {
  testEnvironment: 'node',
  roots: ['<rootDir>/tests'],
  testMatch: ['**/?(*.)+(spec|test).js'],
  collectCoverageFrom: [
    'lib/**/*.js',
    '!lib/**/*.d.ts',
    '!lib/**/*.map',
  ],
};
