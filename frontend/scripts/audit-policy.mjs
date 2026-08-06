/** Converts an npm-audit process result into a fail-closed policy decision. */
export function evaluateAuditProcess(result) {
  if (result.error) {
    return blocked(`Unable to start npm audit: ${result.error.message}`);
  }
  if (result.signal) {
    return blocked(`npm audit terminated by signal ${result.signal}.`);
  }
  if (result.status !== 0 && result.status !== 1) {
    return blocked(`npm audit returned unexpected exit status ${String(result.status)}.`);
  }

  let report;
  try {
    report = JSON.parse(result.stdout || result.stderr);
  } catch {
    return blocked('npm audit did not return valid JSON.');
  }
  if (!report || typeof report !== 'object' || Array.isArray(report)) {
    return blocked('npm audit returned an unsupported report shape.');
  }
  if (report.error) {
    return blocked('npm audit reported an internal or registry error.');
  }
  if (report.auditReportVersion !== 2) {
    return blocked(`Unsupported npm audit report version ${String(report.auditReportVersion)}.`);
  }
  if (!report.metadata || typeof report.metadata !== 'object' || Array.isArray(report.metadata)) {
    return blocked('npm audit report metadata is missing.');
  }
  if (
    !report.vulnerabilities ||
    typeof report.vulnerabilities !== 'object' ||
    Array.isArray(report.vulnerabilities)
  ) {
    return blocked('npm audit vulnerability data is missing.');
  }

  const severities = ['info', 'low', 'moderate', 'high', 'critical'];
  const reportedCounts = report.metadata.vulnerabilities;
  if (!reportedCounts || typeof reportedCounts !== 'object' || Array.isArray(reportedCounts)) {
    return blocked('npm audit vulnerability counts are missing.');
  }
  for (const severity of severities) {
    if (!Number.isInteger(reportedCounts[severity]) || reportedCounts[severity] < 0) {
      return blocked(`npm audit returned an invalid ${severity} vulnerability count.`);
    }
  }
  if (!Number.isInteger(reportedCounts.total) || reportedCounts.total < 0) {
    return blocked('npm audit returned an invalid total vulnerability count.');
  }

  const findings = [];
  const observedCounts = Object.fromEntries(severities.map((severity) => [severity, 0]));
  for (const [packageName, vulnerability] of Object.entries(report.vulnerabilities)) {
    if (!vulnerability || typeof vulnerability !== 'object' || Array.isArray(vulnerability)) {
      return blocked(`npm audit returned invalid vulnerability data for ${packageName}.`);
    }
    if (!Array.isArray(vulnerability.via)) {
      return blocked(`npm audit omitted advisory evidence for ${packageName}.`);
    }
    if (vulnerability.via.length === 0) {
      return blocked(`npm audit omitted advisory evidence for ${packageName}.`);
    }
    if (!severities.includes(vulnerability.severity)) {
      return blocked(`npm audit omitted a supported severity for ${packageName}.`);
    }
    observedCounts[vulnerability.severity] += 1;
    let highestSeverity = vulnerability.severity;
    let highestAdvisory;
    for (const advisory of vulnerability.via) {
      if (typeof advisory === 'string') continue;
      if (!advisory || typeof advisory !== 'object' || Array.isArray(advisory)) {
        return blocked(`npm audit returned invalid advisory evidence for ${packageName}.`);
      }
      if (typeof advisory.severity !== 'string' || !severities.includes(advisory.severity)) {
        return blocked(`npm audit returned invalid advisory severity for ${packageName}.`);
      }
      if (severities.indexOf(advisory.severity) > severities.indexOf(highestSeverity)) {
        highestSeverity = advisory.severity;
        highestAdvisory = advisory;
      } else if (!highestAdvisory) {
        highestAdvisory = advisory;
      }
    }
    if (highestSeverity !== vulnerability.severity) {
      return blocked(
        `npm audit reported an incoherent package/advisory severity for ${packageName}.`,
        [advisoryFinding(packageName, highestSeverity, highestAdvisory)],
      );
    }
    if (highestSeverity === 'high' || highestSeverity === 'critical') {
      findings.push(advisoryFinding(packageName, highestSeverity, highestAdvisory));
    }
  }
  const observedTotal = Object.values(observedCounts).reduce((sum, count) => sum + count, 0);
  if (
    observedTotal !== reportedCounts.total ||
    severities.some((severity) => observedCounts[severity] !== reportedCounts[severity])
  ) {
    return blocked('npm audit vulnerability counts do not match the reported findings.');
  }
  if ((result.status === 0) !== (reportedCounts.total === 0)) {
    return blocked('npm audit exit status does not match the reported vulnerability count.');
  }
  return findings.length ? blocked('High or critical npm advisories were found.', findings) : ok();
}

/** Converts one validated package/advisory maximum into bounded review evidence. */
function advisoryFinding(packageName, severity, advisory) {
  return {
    advisoryId:
      typeof advisory?.url === 'string' ? advisory.url.split('/').at(-1) : 'transitive-or-unknown',
    packageName,
    severity,
    title:
      typeof advisory?.title === 'string' ? advisory.title : 'High-severity dependency finding',
    url: typeof advisory?.url === 'string' ? advisory.url : '',
  };
}

/** Creates the stable successful policy result. */
function ok() {
  return {
    approved: true,
    messages: ['No high or critical npm advisories were found.'],
    findings: [],
  };
}

/** Creates a stable blocked policy result with optional advisory evidence. */
function blocked(message, findings = []) {
  return { approved: false, messages: [message], findings };
}
