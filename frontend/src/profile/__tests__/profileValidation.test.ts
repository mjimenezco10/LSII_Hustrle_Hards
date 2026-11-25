import { checkAvatarFile, validateProfile } from '../profileValidation';

const valid = { description: 'About me', location: 'Mataró', website: 'https://example.com' };

describe('validateProfile', () => {
  it('accepts valid and empty values', () => {
    expect(validateProfile(valid)).toEqual({});
    expect(validateProfile({ description: '', location: '', website: '' })).toEqual({});
  });

  it('rejects texts over their limits', () => {
    expect(
      validateProfile({
        description: 'a'.repeat(501),
        location: 'b'.repeat(101),
        website: `https://${'c'.repeat(200)}`,
      })
    ).toEqual({
      description: 'profile.descriptionTooLong',
      location: 'profile.locationTooLong',
      website: 'profile.websiteTooLong',
    });
  });

  it('asks the website to start with http:// or https://', () => {
    expect(validateProfile({ ...valid, website: 'example.com' })).toEqual({ website: 'profile.websiteInvalid' });
    expect(validateProfile({ ...valid, website: 'http://example.com/me' })).toEqual({});
  });
});

describe('checkAvatarFile', () => {
  it('accepts JPG, PNG and WebP images up to 2 MB', () => {
    expect(checkAvatarFile(new File(['x'], 'a.webp', { type: 'image/webp' }))).toBeNull();
  });

  it('rejects other types and bigger files', () => {
    expect(checkAvatarFile(new File(['x'], 'a.gif', { type: 'image/gif' }))).toBe('profile.avatarType');
    const big = new File(['x'], 'a.png', { type: 'image/png' });
    Object.defineProperty(big, 'size', { value: 2 * 1024 * 1024 + 1 });
    expect(checkAvatarFile(big)).toBe('profile.avatarTooLarge');
  });
});
