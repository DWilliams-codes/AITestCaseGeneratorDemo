import assert from 'node:assert/strict';
import test from 'node:test';
import { evaluateAuditProcess } from './audit-policy.mjs';

/** Builds a complete synthetic npm-audit process result with focused overrides. */
const report = (overrides = {}) => ({
  status: 0,
  signal: null,
  error: null,
  stderr: '',
  stdout: JSON.stringify({
    auditReportVersion: 2,
    vulnerabilities: {},
    metadata: {
      vulnerabilities: { info: 0, low: 0, moderate: 0, high: 0, critical: 0, total: 0 },
    },
    ...overrides,
  }),
});

test('approves a complete report without high or critical findings', () => {
  assert.equal(evaluateAuditProcess(report()).approved, true);
});

test('blocks high findings without an advisory allowlist', () => {
  const result = evaluateAuditProcess({
    ...report({
      vulnerabilities: {
        router: {
          severity: 'high',
          via: [
            {
              severity: 'high',
              title: 'Unsafe route',
              url: 'https://github.com/advisories/GHSA-test',
            },
          ],
        },
      },
      metadata: {
        vulnerabilities: { info: 0, low: 0, moderate: 0, high: 1, critical: 0, total: 1 },
      },
    }),
    status: 1,
  });
  assert.equal(result.approved, false);
  assert.equal(result.findings[0].advisoryId, 'GHSA-test');
});

test('blocks a top-level high finding even when via contains only a dependency name', () => {
  const result = evaluateAuditProcess({
    ...report({
      vulnerabilities: { router: { severity: 'high', via: ['transitive-package'] } },
      metadata: {
        vulnerabilities: { info: 0, low: 0, moderate: 0, high: 1, critical: 0, total: 1 },
      },
    }),
    status: 1,
  });
  assert.equal(result.approved, false);
  assert.equal(result.findings[0].severity, 'high');
});

for (const severity of ['high', 'critical']) {
  test(`blocks nested ${severity} advisories hidden by a moderate package severity`, () => {
    const result = evaluateAuditProcess({
      ...report({
        vulnerabilities: {
          router: {
            severity: 'moderate',
            via: [
              {
                severity,
                title: `Nested ${severity} advisory`,
                url: `https://github.com/advisories/GHSA-nested-${severity}`,
              },
            ],
          },
        },
        metadata: {
          vulnerabilities: { info: 0, low: 0, moderate: 1, high: 0, critical: 0, total: 1 },
        },
      }),
      status: 1,
    });
    assert.equal(result.approved, false);
    assert.equal(result.findings[0].severity, severity);
  });
}

test('blocks object advisories that omit severity evidence', () => {
  const result = evaluateAuditProcess({
    ...report({
      vulnerabilities: {
        router: {
          severity: 'moderate',
          via: [{ title: 'Severity omitted', url: 'https://github.com/advisories/GHSA-omitted' }],
        },
      },
      metadata: {
        vulnerabilities: { info: 0, low: 0, moderate: 1, high: 0, critical: 0, total: 1 },
      },
    }),
    status: 1,
  });
  assert.equal(result.approved, false);
  assert.match(result.messages[0], /invalid advisory severity/);
});

test('blocks package findings that omit all via evidence', () => {
  const result = evaluateAuditProcess({
    ...report({
      vulnerabilities: { router: { severity: 'moderate', via: [] } },
      metadata: {
        vulnerabilities: { info: 0, low: 0, moderate: 1, high: 0, critical: 0, total: 1 },
      },
    }),
    status: 1,
  });
  assert.equal(result.approved, false);
  assert.match(result.messages[0], /omitted advisory evidence/);
});

test('blocks mismatched severity metadata and status-one empty reports', () => {
  assert.equal(
    evaluateAuditProcess(
      report({
        metadata: {
          vulnerabilities: { info: 0, low: 0, moderate: 0, high: 1, critical: 0, total: 1 },
        },
      }),
    ).approved,
    false,
  );
  assert.equal(evaluateAuditProcess({ ...report(), status: 1 }).approved, false);
});

for (const [name, value] of [
  ['spawn errors', { ...report(), error: new Error('spawn failed') }],
  ['signals', { ...report(), signal: 'SIGTERM' }],
  ['unexpected exits', { ...report(), status: 2 }],
  ['invalid JSON', { ...report(), stdout: '{' }],
  ['reported errors', report({ error: { summary: 'registry unavailable' } })],
  ['unsupported versions', report({ auditReportVersion: 3 })],
  ['missing metadata', report({ metadata: undefined })],
  ['missing vulnerability data', report({ vulnerabilities: undefined })],
]) {
  test(`blocks ${name}`, () => {
    assert.equal(evaluateAuditProcess(value).approved, false);
  });
}
