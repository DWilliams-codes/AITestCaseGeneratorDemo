const INTERNAL_ORIGIN = 'https://testforge.local';
const UNSAFE_ENCODED_PATH = /%(?:0[0-9a-f]|1[0-9a-f]|2f|5c|7f)/i;

/** Returns a normalized same-origin route or the conservative application root. */
export function safeReturnLocation(value: unknown, fallback = '/') {
  if (
    typeof value !== 'string' ||
    value.length === 0 ||
    value !== value.trim() ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\') ||
    [...value].some((character) => {
      const code = character.codePointAt(0) ?? 0;
      return code <= 31 || code === 127;
    }) ||
    UNSAFE_ENCODED_PATH.test(value)
  ) {
    return fallback;
  }
  try {
    const target = new URL(value, INTERNAL_ORIGIN);
    if (target.origin !== INTERNAL_ORIGIN || target.username || target.password) return fallback;
    return `${target.pathname}${target.search}${target.hash}`;
  } catch {
    return fallback;
  }
}
