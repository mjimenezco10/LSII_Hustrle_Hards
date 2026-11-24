import { createDefaultPreset } from 'ts-jest';

const tsJestTransformCfg = createDefaultPreset().transform;

/** @type {import("jest").Config} **/
export default {
  preset: 'ts-jest',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testEnvironment: 'jest-environment-jsdom',
  transform: {
    ...tsJestTransformCfg,
  },
  moduleNameMapper: {
    '\\.css$': '<rootDir>/src/test/styleMock.ts',
  },
  collectCoverage: true,
  // lcov is read by SonarQube (sonar-project.properties)
  // lcov paths relative to the repo root (frontend/src/...), as SonarQube reads them from sonar-project.properties
  coverageReporters: ['json', 'html', ['lcov', { projectRoot: '..' }]],
  coverageThreshold: {
    global: {
      branches: 75,
      functions: 75,
      lines: 75,
      statements: 75,
    },
  },
};
