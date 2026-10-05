import { execFileSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const OFFICIAL_OPENAI_BASE_URL = 'https://api.openai.com/v1';
export const PINNED_LIVE_MODEL = 'gpt-5.6-sol';
export const LIVE_AUTHORIZATION_FLAG = 'TESTFORGE_LIVE_GENERATION_AUTHORIZED';
export const DOCKER_EXECUTABLE_ENVIRONMENT_KEY = 'TESTFORGE_DOCKER_EXECUTABLE';

/** Returns whether the root Compose environment contains a nonblank provider-key entry. */
export function rootEnvHasProviderKey(rootDirectory) {
  const envPath = path.join(rootDirectory, '.env');
  if (!existsSync(envPath)) return false;
  return readFileSync(envPath, 'utf8')
    .split(/\r?\n/)
    .some((line) => {
      const match = line.match(/^\s*OPENAI_API_KEY\s*=\s*(.*?)\s*(?:#.*)?$/);
      if (!match) return false;
      const value = match[1].replace(/^(?:"([\s\S]*)"|'([\s\S]*)')$/, '$1$2').trim();
      return value.length > 0;
    });
}

/** Returns the one canonical Compose path only when the backend has no override source. */
export function parseExactDefaultComposeSource(configFiles, expectedComposePath) {
  const sources = configFiles
    .split(',')
    .map((source) => source.trim())
    .filter(Boolean);
  if (sources.length !== 1) return false;
  /** Normalizes Compose source paths before the exact-source comparison. */
  const canonical = (candidate) => path.normalize(path.resolve(candidate)).toLowerCase();
  return canonical(sources[0]) === canonical(expectedComposePath);
}

/** Resolves an optional absolute Docker executable without invoking a shell. */
export function resolveDockerExecutable(environment = process.env) {
  const configured = environment[DOCKER_EXECUTABLE_ENVIRONMENT_KEY]?.trim();
  if (!configured) return 'docker';
  if (!path.isAbsolute(configured) || !/^docker(?:\.exe)?$/i.test(path.basename(configured))) {
    throw new Error(
      `${DOCKER_EXECUTABLE_ENVIRONMENT_KEY} must be an absolute docker executable path.`,
    );
  }
  return configured;
}

/** Inspects only active backend labels, then checks the effective runtime settings without printing them. */
export function inspectLiveBackend(runDocker, expectedComposePath) {
  const containers = runDocker([
    'ps',
    '--filter',
    'label=com.docker.compose.service=backend',
    '--format',
    '{{.ID}}',
  ])
    .trim()
    .split(/\r?\n/)
    .filter(Boolean);
  if (containers.length !== 1) {
    throw new Error('Live generation requires exactly one running Compose backend.');
  }
  const [containerId] = containers;
  const project = runDocker([
    'inspect',
    '--format',
    '{{ index .Config.Labels "com.docker.compose.project" }}',
    containerId,
  ]).trim();
  const configFiles = runDocker([
    'inspect',
    '--format',
    '{{ index .Config.Labels "com.docker.compose.project.config_files" }}',
    containerId,
  ]).trim();
  if (
    project !== 'testforge-ai' ||
    !parseExactDefaultComposeSource(configFiles, expectedComposePath)
  ) {
    throw new Error('Live generation refuses a non-default or Responses-stub Compose backend.');
  }

  try {
    runDocker([
      'exec',
      containerId,
      '/bin/sh',
      '-ec',
      `[ "${'${TEST_GENERATION_PROVIDER:-openai}'}" = "openai" ]
[ "${'${OPENAI_BASE_URL:-https://api.openai.com/v1}'}" = "${OFFICIAL_OPENAI_BASE_URL}" ]
[ "${'${OPENAI_MODEL:-gpt-5.6-sol}'}" = "${PINNED_LIVE_MODEL}" ]
[ -n "${'${OPENAI_API_KEY:-}'}" ]`,
    ]);
  } catch {
    throw new Error(
      'Live generation requires the running backend to use OpenAI, the official HTTPS base URL, the pinned model, and a configured key.',
    );
  }
}

/** Fails closed before a paid candidate when authorization, Compose, or runtime binding is unsafe. */
export function validateLiveGenerationPreflight({
  environment = process.env,
  rootDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..'),
  inspectBackend,
} = {}) {
  if (environment[LIVE_AUTHORIZATION_FLAG] !== 'true') {
    throw new Error(`Live generation requires ${LIVE_AUTHORIZATION_FLAG}=true.`);
  }
  if (environment.PLAYWRIGHT_BASE_URL !== 'http://localhost:3000') {
    throw new Error('Live generation requires PLAYWRIGHT_BASE_URL=http://localhost:3000.');
  }
  if (environment.OPENAI_API_KEY?.trim()) {
    throw new Error(
      'Live generation requires the root ignored .env key source, not a process override.',
    );
  }
  const expectedComposePath = path.join(rootDirectory, 'docker-compose.yml');
  const verifyBackend =
    inspectBackend ??
    (() =>
      inspectLiveBackend(
        createDockerRunner(resolveDockerExecutable(environment)),
        expectedComposePath,
      ));
  verifyBackend(expectedComposePath);
  if (!rootEnvHasProviderKey(rootDirectory)) {
    throw new Error('Live generation requires a nonblank OPENAI_API_KEY in the ignored root .env.');
  }
}

/** Creates a narrow Docker runner that retains no output other than selected labels. */
function createDockerRunner(dockerExecutable) {
  return (arguments_) => {
    try {
      return execFileSync(dockerExecutable, arguments_, {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
      });
    } catch {
      throw new Error('Live generation could not verify the running Compose backend.');
    }
  };
}

const invokedDirectly =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invokedDirectly) {
  try {
    validateLiveGenerationPreflight();
    process.env.TESTFORGE_LIVE_GENERATION_MODE = 'true';
    const require = createRequire(import.meta.url);
    execFileSync(
      process.execPath,
      [require.resolve('@playwright/test/cli'), 'test', '--grep', '@live-generation'],
      {
        stdio: 'inherit',
        env: process.env,
      },
    );
  } catch (error) {
    if (error?.message) console.error(error.message);
    process.exitCode = 1;
  }
}
