'use strict';

// Runs only from a trusted base revision. PR patches are data, never executable code.
const fs = require('node:fs');
const path = require('node:path');
const MARKER = '<!-- cinego-ai-review:v1 -->';

function safePath(value) {
  return typeof value === 'string' && !value.startsWith('/') && !value.includes('\\') &&
    !/[\x00-\x1f\x7f`]/.test(value) &&
    !value.split('/').some(part => part === '..' || part === '.' || !part);
}

function redact(text) {
  return String(text).split('\n').map(line => {
    if (/(?:password|secret|signer[-_]?key|api[-_]?key|authorization)["']?\s*[=:]/i.test(line)) {
      return '[REDACTED sensitive assignment]';
    }
    return line.replace(/\b(?:sk-[A-Za-z0-9_-]{12,}|gh[pousr]_[A-Za-z0-9_]{15,}|github_pat_[A-Za-z0-9_]{15,})\b/g,
      '[REDACTED credential]');
  }).join('\n');
}

function buildInput(files, prefixes, limit = 80000) {
  const allowed = prefixes.split(',').map(p => p.trim()).filter(Boolean);
  if (!allowed.length || allowed.some(p => !safePath(p.replace(/\/$/, '')) || !p.endsWith('/'))) {
    throw new Error('Review paths must be safe relative directory prefixes ending in /');
  }
  const selected = [];
  const omitted = [];
  let used = 0;
  for (const file of files) {
    if (!safePath(file.filename) || !allowed.some(p => file.filename.startsWith(p))) continue;
    if (/(?:^|\/)(?:\.env(?:\..*)?|application-local\.[^/]+)$|\.(?:pem|key|p12|pfx)$/i.test(file.filename)) {
      omitted.push(file.filename);
      continue;
    }
    const patch = typeof file.patch === 'string' ? redact(file.patch) : '';
    if (patch && Number.isInteger(file.changes)) {
      const suppliedChanges = file.patch.split('\n').filter(line => line.startsWith('+') || line.startsWith('-')).length;
      if (suppliedChanges < file.changes) omitted.push(file.filename + ' (partial patch)');
    }
    const item = {path: file.filename, status: file.status, patch};
    const size = JSON.stringify(item).length;
    if (!patch || used + size > limit) { omitted.push(file.filename); continue; }
    used += size;
    selected.push(item);
  }
  return {files: selected, omitted, complete: omitted.length === 0};
}

async function prepare({github, context, core}) {
  const pr = context.payload.pull_request;
  if (!pr) throw new Error('A pull_request event is required');
  if (pr.head.repo.full_name !== context.repo.owner + '/' + context.repo.repo) {
    throw new Error('Fork PRs cannot enter the paid review job');
  }
  const files = await github.paginate(github.rest.pulls.listFiles, {...context.repo, pull_number: pr.number, per_page: 100});
  // GitHub caps listFiles at 3000. Never call a capped diff a complete review.
  if (pr.changed_files > files.length) throw new Error('GitHub returned an incomplete changed-file list');
  const input = buildInput(files, process.env.REVIEW_PATHS || 'cine-be/,shared-model/');
  const directory = path.join(process.env.RUNNER_TEMP, 'cinego-review');
  fs.mkdirSync(directory, {recursive: true});
  fs.writeFileSync(path.join(directory, 'pr-diff.json'), JSON.stringify({
    schema_version: 1, repository: context.repo.owner + '/' + context.repo.repo,
    pull_request: pr.number, base_sha: pr.base.sha, head_sha: pr.head.sha, ...input
  }, null, 2));
  core.setOutput('has_diff', String(input.files.length > 0));
  core.setOutput('head_sha', pr.head.sha);
  core.setOutput('review_directory', directory);
  core.setOutput('coverage', input.complete ? 'complete' : 'partial');
  await core.summary.addRaw(`AI review input: ${input.files.length} files; ${input.omitted.length} omitted.\n`).write();
}

function renderReview(raw, headSha, coverage, allowedPaths = null) {
  if (!/^[a-f0-9]{40}$/.test(headSha) || !['complete', 'partial'].includes(coverage)) {
    throw new Error('Invalid review revision/coverage');
  }
  const result = JSON.parse(raw);
  if (result.schema_version !== 1 || !Array.isArray(result.findings) || result.findings.length > 20 ||
      typeof result.summary !== 'string') throw new Error('Invalid AI review output');
  const escape = text => redact(String(text)).replace(/<!--|-->/g, '')
    .replace(/@/g, '@\u200b').replace(/</g, '&lt;').replace(/>/g, '&gt;').slice(0, 2500);
  const lines = [MARKER, '### AI review — tham khảo, cần người kiểm tra',
    `Commit: \`${headSha}\` · Phạm vi diff: ${coverage}`, '', escape(result.summary)];
  for (const finding of result.findings) {
    if (!['P0', 'P1', 'P2', 'P3'].includes(finding.severity) || !safePath(finding.path) ||
        (allowedPaths && !allowedPaths.has(finding.path)) ||
        !Number.isInteger(finding.line) || finding.line < 1 || typeof finding.title !== 'string' ||
        typeof finding.body !== 'string') throw new Error('Invalid AI finding');
    lines.push('', `- **${finding.severity}** \`${escape(finding.path)}:${finding.line}\` — ${escape(finding.title)}`,
      `  ${escape(finding.body)}`);
  }
  lines.push('', 'AI không thay thế test, security review hoặc phê duyệt của developer.');
  return lines.join('\n').slice(0, 55000);
}

async function post({github, context, core}) {
  const pr = context.payload.pull_request;
  const current = await github.rest.pulls.get({...context.repo, pull_number: pr.number});
  if (current.data.head.sha !== process.env.REVIEW_HEAD_SHA || current.data.state !== 'open') {
    await core.summary.addRaw('Không đăng AI review: PR đã đổi commit hoặc đã đóng.').write();
    return;
  }
  const changed = await github.paginate(github.rest.pulls.listFiles,
    {...context.repo, pull_number: pr.number, per_page: 100});
  const input = buildInput(changed, process.env.REVIEW_PATHS || 'cine-be/,shared-model/');
  const paths = new Set(input.files.map(file => file.path));
  const body = renderReview(process.env.REVIEW_RESULT, process.env.REVIEW_HEAD_SHA, process.env.REVIEW_COVERAGE, paths);
  const comments = await github.paginate(github.rest.issues.listComments,
    {...context.repo, issue_number: pr.number, per_page: 100});
  const existing = comments.find(comment => comment.user?.login === 'github-actions[bot]' &&
    comment.body?.startsWith(MARKER));
  if (existing) await github.rest.issues.updateComment({...context.repo, comment_id: existing.id, body});
  else await github.rest.issues.createComment({...context.repo, issue_number: pr.number, body});
}

module.exports = {safePath, redact, buildInput, prepare, renderReview, post};
