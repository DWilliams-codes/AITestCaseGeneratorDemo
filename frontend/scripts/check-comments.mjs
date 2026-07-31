import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import ts from 'typescript';

const roots = ['src', 'e2e', 'scripts'];
const supportedExtensions = new Set(['.ts', '.tsx', '.js', '.jsx', '.mjs', '.cjs']);
const missing = [];

/** Returns maintained JavaScript and TypeScript source paths in deterministic order. */
function collectFiles(directory) {
  return fs
    .readdirSync(directory, { withFileTypes: true })
    .flatMap((entry) => {
      const entryPath = path.join(directory, entry.name);
      if (entry.isDirectory()) return collectFiles(entryPath);
      if (!entry.isFile() || !supportedExtensions.has(path.extname(entry.name))) return [];
      if (entry.name.endsWith('.d.ts')) return [];
      return [entryPath];
    })
    .sort();
}

/** Reports whether a declaration is immediately preceded by an attachable JSDoc comment. */
function hasDocComment(source, declarationStart) {
  const prefix = source.slice(0, declarationStart).trimEnd();
  if (!prefix.endsWith('*/')) return false;
  const open = prefix.lastIndexOf('/**');
  const close = prefix.lastIndexOf('*/');
  return open >= 0 && close > open && prefix.slice(close + 2).trim() === '';
}

/** Returns the stable name and documentation anchor for a declared function-like node. */
function functionCandidate(node) {
  if (ts.isFunctionDeclaration(node) && node.name) {
    return { name: node.name.text, anchor: node };
  }
  if (
    (ts.isMethodDeclaration(node) ||
      ts.isGetAccessorDeclaration(node) ||
      ts.isSetAccessorDeclaration(node)) &&
    node.name
  ) {
    return { name: node.name.getText(), anchor: node };
  }
  if (ts.isConstructorDeclaration(node)) {
    return { name: 'constructor', anchor: node };
  }
  if (
    ts.isVariableDeclaration(node) &&
    ts.isIdentifier(node.name) &&
    node.initializer &&
    (ts.isArrowFunction(node.initializer) || ts.isFunctionExpression(node.initializer))
  ) {
    let anchor = node;
    while (anchor.parent && !ts.isVariableStatement(anchor)) anchor = anchor.parent;
    return { name: node.name.text, anchor };
  }
  return null;
}

/** Recursively checks documentable declarations while leaving inline framework callbacks contextual. */
function inspectNode(node, sourceFile, source) {
  const candidate = functionCandidate(node);
  if (candidate && !hasDocComment(source, candidate.anchor.getStart(sourceFile, false))) {
    const position = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile, false));
    missing.push(`${sourceFile.fileName}:${position.line + 1} ${candidate.name}`);
  }
  ts.forEachChild(node, (child) => inspectNode(child, sourceFile, source));
}

for (const sourcePath of roots.flatMap(collectFiles)) {
  const source = fs.readFileSync(sourcePath, 'utf8');
  const kind = sourcePath.endsWith('x') ? ts.ScriptKind.TSX : ts.ScriptKind.TS;
  const sourceFile = ts.createSourceFile(sourcePath, source, ts.ScriptTarget.Latest, true, kind);
  inspectNode(sourceFile, sourceFile, source);
}

if (missing.length > 0) {
  console.error('Declared functions missing intent-level comments:');
  for (const item of missing) console.error(`- ${item}`);
  process.exitCode = 1;
} else {
  console.log('All declared frontend functions have intent-level comments.');
}
