// Starts the backend for the E2E suite on a fresh environment: the `dev` profile against a new, empty PostgreSQL
// container (NFR-01: no H2, needs Docker) and a new video store with only the fixture videos, so every run starts
// from the same data.
import { spawn, spawnSync } from 'node:child_process';
import { cpSync, rmSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const e2eDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const storeDir = path.join(e2eDir, '.store');

// Throwaway database: its own container and port, so it never touches the developer's database (5432)
const DB_CONTAINER = 'protube_e2e_db';
const DB_PORT = process.env.E2E_DB_PORT ?? '5433';
const DB = { name: 'protube_e2e', user: 'protube', password: 'protube' };

const docker = (...args) => spawnSync('docker', args, { stdio: 'pipe', encoding: 'utf8' });

function stopDatabase() {
  docker('rm', '-f', DB_CONTAINER);
}

function startDatabase() {
  // A container left by an interrupted run would keep the old data
  stopDatabase();
  const started = docker(
    'run', '-d', '--rm', '--name', DB_CONTAINER,
    '-e', `POSTGRES_DB=${DB.name}`, '-e', `POSTGRES_USER=${DB.user}`, '-e', `POSTGRES_PASSWORD=${DB.password}`,
    '-p', `${DB_PORT}:5432`, 'postgres:16'
  );
  if (started.status !== 0) {
    console.error(`Could not start PostgreSQL for the E2E tests (is Docker running?)\n${started.stderr}`);
    process.exit(1);
  }
  // Wait until it answers real queries over TCP (the image restarts its server once after initialising)
  for (let attempt = 0; attempt < 60; attempt++) {
    const ready = docker('exec', DB_CONTAINER, 'psql', '-h', '127.0.0.1', '-U', DB.user, '-d', DB.name, '-c', 'select 1');
    if (ready.status === 0) return;
    spawnSync(process.execPath, ['-e', 'setTimeout(() => {}, 1000)']);
  }
  console.error('PostgreSQL for the E2E tests did not become ready');
  stopDatabase();
  process.exit(1);
}

// Uploads made by the tests are written to the store, so it is recreated from the fixtures on each run
rmSync(storeDir, { recursive: true, force: true });
cpSync(path.join(e2eDir, 'fixtures', 'store'), storeDir, { recursive: true });

startDatabase();

const maven = process.env.E2E_MAVEN ?? 'mvn';
const backend = spawn(maven, ['-q', 'spring-boot:run'], {
  cwd: path.join(e2eDir, '..', 'backend'),
  stdio: 'inherit',
  // mvn is a .cmd script on Windows
  shell: process.platform === 'win32',
  env: {
    ...process.env,
    // Replaces the developer's own ENV_PROTUBE_STORE_DIR, so the suite never reads or writes their videos.
    // Forward slashes work on every OS (Java accepts them on Windows too) and give a valid file: URL for MvcConfig;
    // it must end with a separator because MvcConfig serves /media/** from file:<store dir>
    ENV_PROTUBE_STORE_DIR: `${storeDir.split(path.sep).join('/')}/`,
    // Same for the database: always the throwaway one started above, never the developer's
    ENV_PROTUBE_DB_HOST: 'localhost',
    ENV_PROTUBE_DB_PORT: DB_PORT,
    ENV_PROTUBE_DB: DB.name,
    ENV_PROTUBE_DB_USER: DB.user,
    ENV_PROTUBE_DB_PASS: DB.password,
    // Hides the "Sign in with Google" button (the frontend reads the client id from the backend): it needs a real
    // Google account
    ENV_PROTUBE_GOOGLE_CLIENT_ID: '',
    SERVER_PORT: process.env.E2E_BACKEND_PORT ?? '8081',
  },
});

// Stop Spring Boot when Playwright stops this script (gracefulShutdown in playwright.config.ts), then drop the database
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => backend.kill(signal));
}
backend.on('exit', (code) => {
  stopDatabase();
  process.exit(code ?? 0);
});
