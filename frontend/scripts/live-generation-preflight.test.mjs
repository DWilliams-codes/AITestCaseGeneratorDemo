import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import {
  inspectLiveBackend,
  parseExactDefaultComposeSource,
  rootEnvHasProviderKey,
  resolveDockerExecutable,
  validateLiveGenerationPreflight,
} from './live-generation-preflight.mjs';

/** Runs an isolated fixture with a temporary synthetic root environment file. */

/** Runs an isolated fixture with a temporary synthetic root environment file. */
function withRootEnv(contents, callback) {
  const directory = mkdtempSync(path.join(os.tmpdir(), 'testforge-live-preflight-'));
  try {
    if (contents !== null) writeFileSync(path.join(directory, '.env'), contents);
    callback(directory);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

/** Builds explicit test-only authorization flags without reading real credentials. */

/** Builds explicit test-only authorization flags without reading real credentials. */
function authorizedEnvironment() {
  return {
    TESTFORGE_LIVE_GENERATION_AUTHORIZED: 'true',
    PLAYWRIGHT_BASE_URL: 'http://localhost:3000',
  };
}

test('requires an explicit authorization flag, localhost Compose frontend, and root provider key', () => {
  withRootEnv('OPENAI_API_KEY=present-only\n', (rootDirectory) => {
    assert.throws(
      () =>
        validateLiveGenerationPreflight({
          environment: {},
          rootDirectory,
          /** Supplies unused synthetic inspection for this rejected request. */ inspectBackend() {},
        }),
      /TESTFORGE_LIVE_GENERATION_AUTHORIZED=true/,
    );
    assert.throws(
      () =>
        validateLiveGenerationPreflight({
          environment: { TESTFORGE_LIVE_GENERATION_AUTHORIZED: 'true' },
          rootDirectory,
          /** Supplies synthetic container metadata to the preflight under test. */
          inspectBackend() {},
        }),
      /PLAYWRIGHT_BASE_URL=http:\/\/localhost:3000/,
    );
    assert.throws(
      () =>
        validateLiveGenerationPreflight({
          environment: { ...authorizedEnvironment(), OPENAI_API_KEY: 'process-only' },
          rootDirectory,
          /** Supplies synthetic container metadata to the preflight under test. */
          inspectBackend() {},
        }),
      /root ignored .env key source/,
    );
  });
  withRootEnv(null, (rootDirectory) => {
    assert.throws(
      () =>
        validateLiveGenerationPreflight({
          environment: authorizedEnvironment(),
          rootDirectory,
          /** Supplies synthetic container metadata to the preflight under test. */
          inspectBackend() {},
        }),
      /ignored root .env/,
    );
  });
  withRootEnv('OPENAI_API_KEY=present-only\n', (rootDirectory) => {
    assert.doesNotThrow(() =>
      validateLiveGenerationPreflight({
        environment: authorizedEnvironment(),
        rootDirectory,
        /** Supplies synthetic container metadata to the preflight under test. */
        inspectBackend() {},
      }),
    );
  });
});

test('accepts exactly the canonical default Compose source and rejects lookalikes or overrides', () => {
  const expected = path.resolve('docker-compose.yml');
  assert.equal(parseExactDefaultComposeSource(expected, expected), true);
  assert.equal(
    parseExactDefaultComposeSource(
      `${expected},${path.resolve('docker-compose.e2e.yml')}`,
      expected,
    ),
    false,
  );
  assert.equal(parseExactDefaultComposeSource('/attacker/not-docker-compose.yml', expected), false);
  assert.equal(parseExactDefaultComposeSource(`${expected}.bak`, expected), false);
  assert.equal(
    parseExactDefaultComposeSource(
      path.join(path.dirname(expected), '..', 'other', 'docker-compose.yml'),
      expected,
    ),
    false,
  );
});

test('uses an explicit safe Docker executable path or the portable PATH fallback', () => {
  assert.equal(resolveDockerExecutable({}), 'docker');
  assert.equal(
    resolveDockerExecutable({ TESTFORGE_DOCKER_EXECUTABLE: 'C:\\tools\\docker.exe' }),
    'C:\\tools\\docker.exe',
  );
  assert.throws(
    () => resolveDockerExecutable({ TESTFORGE_DOCKER_EXECUTABLE: 'docker.exe' }),
    /absolute docker executable path/,
  );
  assert.throws(
    () => resolveDockerExecutable({ TESTFORGE_DOCKER_EXECUTABLE: 'C:\\tools\\not-docker.exe' }),
    /absolute docker executable path/,
  );
});

test('refuses the active Responses-stub backend before a browser can generate', () => {
  const calls = [];
  /** Models a Docker command response without calling a live container runtime. */
  const runDocker = (arguments_) => {
    calls.push(arguments_);
    if (arguments_[0] === 'ps') return 'backend-id\n';
    if (arguments_[2] === '{{ index .Config.Labels "com.docker.compose.project" }}') {
      return 'testforge-ai\n';
    }
    if (arguments_[2] === '{{ index .Config.Labels "com.docker.compose.project.config_files" }}') {
      return 'C:/repo/docker-compose.yml,C:/repo/docker-compose.e2e.yml\n';
    }
    throw new Error('Runtime settings must not be inspected after a stub-source refusal.');
  };
  assert.throws(
    () => inspectLiveBackend(runDocker, 'C:/repo/docker-compose.yml'),
    /Responses-stub Compose backend/,
  );
  assert.equal(
    calls.some((arguments_) => arguments_[0] === 'exec'),
    false,
  );
});

test('inspects only labels and boolean runtime checks for a default backend', () => {
  const calls = [];
  /** Models a Docker command response without calling a live container runtime. */
  const runDocker = (arguments_) => {
    calls.push(arguments_);
    if (arguments_[0] === 'ps') return 'backend-id\n';
    if (arguments_[2] === '{{ index .Config.Labels "com.docker.compose.project" }}') {
      return 'testforge-ai\n';
    }
    if (arguments_[2] === '{{ index .Config.Labels "com.docker.compose.project.config_files" }}') {
      return 'C:/repo/docker-compose.yml\n';
    }
    if (arguments_[0] === 'exec') return '';
    throw new Error('Unexpected Docker inspection.');
  };
  assert.doesNotThrow(() => inspectLiveBackend(runDocker, 'C:/repo/docker-compose.yml'));
  assert.deepEqual(
    calls.map((arguments_) => arguments_[0]),
    ['ps', 'inspect', 'inspect', 'exec'],
  );
  assert.match(calls.at(-1).at(-1), /OPENAI_BASE_URL/);
  assert.doesNotMatch(calls.at(-1).at(-1), /present-only/);
});

test('binds live mode to zero retries and disables all retained Playwright artifacts', () => {
  const config = readFileSync(path.resolve('playwright.config.ts'), 'utf8');
  assert.match(config, /retries: liveGenerationMode \? 0/);
  assert.match(config, /trace: liveGenerationMode \? 'off'/);
  assert.match(config, /screenshot: liveGenerationMode \? 'off'/);
  assert.match(config, /video: liveGenerationMode \? 'off'/);
  const liveSpec = readFileSync(path.resolve('e2e/stage-one-workflow.spec.ts'), 'utf8');
  assert.match(liveSpec, /TESTFORGE_LIVE_GENERATION_MODE !== 'true'/);
  assert.match(liveSpec, /must be launched through npm run e2e:live/);
});
