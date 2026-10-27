import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('..', import.meta.url));
const config = readFileSync(path.join(root, 'shoppingmall-backend/src/main/resources/application.properties'), 'utf8');
const value = key => config.split(/\r?\n/).find(line => line.startsWith(key + '='))?.slice(key.length + 1);
for (const [key, variable] of Object.entries({
    'spring.datasource.url': 'DB_URL', 'spring.datasource.username': 'DB_USERNAME',
    'spring.datasource.password': 'DB_PASSWORD', 'jwt.secret': 'JWT_SECRET',
})) assert.equal(value(key), '${' + variable + '}', 'Configuration must reference environment key: ' + key);
assert.ok(value('spring.config.import')?.includes('.env[.properties]'), 'Spring must import .env');
const tracked = execFileSync('git', ['ls-files', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean);
assert.ok(!tracked.some(file => /^\.env(?:\..*)?$/.test(path.basename(file)) && !file.endsWith('.example')), 'Local .env must not be tracked');
assert.equal(spawnSync('git', ['check-ignore', '-q', '.env'], { cwd: root }).status, 0, '.env must be ignored');
const example = readFileSync(path.join(root, '.env.example'), 'utf8');
assert.match(example, /^JWT_SECRET=$/m, 'Example must not contain a JWT secret');
assert.match(example, /^DB_PASSWORD=$/m, 'Example must not contain a DB password');
assert.ok(existsSync(path.join(root, 'shoppingmall-frontend/vite.config.ts')), 'Frontend TypeScript Vite config must exist');
assert.ok(!existsSync(path.join(root, 'shoppingmall-frontend/vite.config.js')), 'Legacy Vite config must not shadow the API proxy');
assert.ok(readFileSync(path.join(root, 'shoppingmall-frontend/index.html'), 'utf8').includes('/src/main.tsx'), 'Frontend must use the integrated TypeScript entry point');

// Check for high-confidence credential patterns without printing their values.
const credential = /(?:gh[pousr]_[a-zA-Z0-9]{30,}|github_pat_[a-zA-Z0-9_]{40,}|AKIA[A-Z0-9]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)/;
const localEnv = path.join(root, '.env');
const localJwt = existsSync(localEnv)
    ? readFileSync(localEnv, 'utf8').split(/\r?\n/).find(line => line.startsWith('JWT_SECRET='))?.slice('JWT_SECRET='.length)
    : undefined;
for (const file of tracked) {
    if (!existsSync(path.join(root, file)) || /\.(?:png|jpg|jar|svg)$/.test(file)) continue;
    // The verifier itself contains pattern descriptions, not actual keys.
    if (file === 'tools/verify-env.mjs') continue;
    const content = readFileSync(path.join(root, file), 'utf8');
    assert.ok(!credential.test(content), 'Potential credential in ' + file);
    if (localJwt && localJwt.length >= 16) assert.ok(!content.includes(localJwt), 'Runtime JWT key copied into ' + file);
}
console.log('Environment references, ignore policy and credential-pattern checks passed. No values printed.');
