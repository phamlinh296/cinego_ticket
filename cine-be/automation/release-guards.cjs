'use strict';

function validateEnvironment(environment) {
  const reviewers = environment?.protection_rules?.find(rule => rule.type === 'required_reviewers');
  if (!reviewers?.reviewers?.length) {
    throw new Error('Production environment must already have required reviewers; no deployment was started');
  }
}

async function preflight({github, context, core}, sha, stagingId, productionId) {
  if (context.ref !== 'refs/heads/main' || context.eventName === 'pull_request') {
    throw new Error('Release is restricted to trusted main');
  }
  if (!/^[a-f0-9]{40}$/.test(sha)) throw new Error('A full commit SHA is required');
  if (!/^srv-[a-z0-9]+$/.test(stagingId || '') || !/^srv-[a-z0-9]+$/.test(productionId || '') ||
      stagingId === productionId) throw new Error('Staging and production require distinct Render service IDs');
  const main = await github.rest.repos.getCommit({...context.repo, ref: 'main'});
  if (main.data.sha !== sha) throw new Error('Refusing to release an outdated main commit');
  const environment = await github.rest.repos.getEnvironment({...context.repo, environment_name: 'production'});
  validateEnvironment(environment.data);
  core.setOutput('image', `ghcr.io/${context.repo.owner.toLowerCase()}/cinego-be`);
}

module.exports = {validateEnvironment, preflight};
