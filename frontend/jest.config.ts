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
  // NFR-05 asks for 50%; the team enforces more than 90% (README improvement "More than 90% of test coverage")
  coverageThreshold: {
    global: {
      branches: 90,
      functions: 90,
      lines: 90,
      statements: 90,
    },
  },
};
