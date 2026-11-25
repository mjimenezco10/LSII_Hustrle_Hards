import type { MessageKey } from '../i18n/messages';
import type { ProfileChanges } from './profileApi';

// Same limits as UpdateProfileRequest in the backend
export const MAX_DESCRIPTION = 500;
export const MAX_LOCATION = 100;
export const MAX_WEBSITE = 200;
export const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
export const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export type ProfileErrors = Partial<Record<keyof ProfileChanges, MessageKey>>;

/**
 * Checks the profile form before sending it, with the same rules as the backend. Returns the message key of each
 * wrong field; empty when everything is valid.
 */
export function validateProfile(changes: ProfileChanges): ProfileErrors {
  const errors: ProfileErrors = {};
  if (changes.description.trim().length > MAX_DESCRIPTION) errors.description = 'profile.descriptionTooLong';
  if (changes.location.trim().length > MAX_LOCATION) errors.location = 'profile.locationTooLong';
  const website = changes.website.trim();
  if (website.length > MAX_WEBSITE) errors.website = 'profile.websiteTooLong';
  else if (website && !/^https?:\/\/\S+$/i.test(website)) errors.website = 'profile.websiteInvalid';
  return errors;
}

/**
 * Message key when the file cannot be an avatar (wrong type or too big), null when it can.
 */
export function checkAvatarFile(file: File): MessageKey | null {
  if (!AVATAR_TYPES.includes(file.type)) return 'profile.avatarType';
  if (file.size > MAX_AVATAR_BYTES) return 'profile.avatarTooLarge';
  return null;
}
