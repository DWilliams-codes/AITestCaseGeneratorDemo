import { spawnSync } from 'node:child_process';

const acceptedAdvisories = new Map([
  [
    'GHSA-qwww-vcr4-c8h2',
    {
      packageName: 'react-router',
      expiresOn: '2026-08-30',
      rationale:
        'The affected React Server Components action path is not present in this client-only Vite application.',
    },
  ],
]);

const npmExecutable = process.env.npm_execpath ? process.execPath : 'npm';
const npmArguments = process.env.npm_execpath
  ? [process.env.npm_execpath, 'audit', '--json']
  : ['audit', '--json'];
const auditProcess = spawnSync(npmExecutable, npmArguments, {
  encoding: 'utf8',
  shell: false,
});

if (auditProcess.error) {
  console.error(`Unable to start npm audit: ${auditProcess.error.message}`);
  process.exit(1);
}

let report;
try {
  report = JSON.parse(auditProcess.stdout || auditProcess.stderr);
} catch {
  console.error('npm audit did not return valid JSON.');
  console.error(auditProcess.stderr || auditProcess.stdout);
  process.exit(1);
}

const highOrCriticalFindings = [];

for (const [packageName, vulnerability] of Object.entries(report.vulnerabilities ?? {})) {
  for (const advisory of vulnerability.via ?? []) {
    if (typeof advisory === 'string' || !['high', 'critical'].includes(advisory.severity)) {
      continue;
    }

    const advisoryId = advisory.url.split('/').at(-1);
    highOrCriticalFindings.push({
      advisoryId,
      packageName,
      severity: advisory.severity,
      title: advisory.title,
      url: advisory.url,
    });
  }
}

const today = new Date().toISOString().slice(0, 10);
const rejectedFindings = [];

for (const finding of highOrCriticalFindings) {
  const acceptance = acceptedAdvisories.get(finding.advisoryId);
  const acceptanceIsValid =
    acceptance && acceptance.packageName === finding.packageName && today <= acceptance.expiresOn;

  if (!acceptanceIsValid) {
    rejectedFindings.push(finding);
    continue;
  }

  console.warn(
    `Accepted until ${acceptance.expiresOn}: ${finding.advisoryId} in ${finding.packageName}. ${acceptance.rationale}`,
  );
}

if (rejectedFindings.length > 0) {
  console.error('Unaccepted high or critical npm advisories were found:');
  for (const finding of rejectedFindings) {
    console.error(
      `- [${finding.severity}] ${finding.advisoryId} in ${finding.packageName}: ${finding.title} (${finding.url})`,
    );
  }
  process.exit(1);
}

console.log('No unaccepted high or critical npm advisories were found.');
