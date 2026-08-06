import { spawnSync } from 'node:child_process';
import { evaluateAuditProcess } from './audit-policy.mjs';

const npmExecutable = process.env.npm_execpath ? process.execPath : 'npm';
const npmArguments = process.env.npm_execpath
  ? [process.env.npm_execpath, 'audit', '--json']
  : ['audit', '--json'];
const result = evaluateAuditProcess(
  spawnSync(npmExecutable, npmArguments, { encoding: 'utf8', shell: false }),
);

for (const message of result.messages) {
  (result.approved ? console.log : console.error)(message);
}
for (const finding of result.findings) {
  console.error(
    `- [${finding.severity}] ${finding.advisoryId} in ${finding.packageName}: ${finding.title} (${finding.url})`,
  );
}
process.exit(result.approved ? 0 : 1);
