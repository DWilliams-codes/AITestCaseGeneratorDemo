import { safeReturnLocation } from './returnLocation';

describe('safeReturnLocation', () => {
  it('preserves an internal path, query, and fragment', () => {
    expect(safeReturnLocation('/requirements/story?page=2&set=old#history')).toBe(
      '/requirements/story?page=2&set=old#history',
    );
  });

  it.each([
    'https://attacker.example/path',
    '//attacker.example/path',
    '/\\attacker.example',
    '/%2fattacker.example',
    '/path\nnext',
    'javascript:alert(1)',
    ' /projects',
    null,
  ])('rejects unsafe or malformed target %p', (target) => {
    expect(safeReturnLocation(target)).toBe('/');
  });
});
