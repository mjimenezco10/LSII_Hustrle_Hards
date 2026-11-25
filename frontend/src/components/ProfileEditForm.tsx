import { useState, type ChangeEvent, type FormEvent } from 'react';
import { toApiError } from '../auth/authApi';
import { useTranslation } from '../i18n/i18nContext';
import { deleteAvatar, updateProfile, uploadAvatar, type Profile, type ProfileChanges } from '../profile/profileApi';
import { checkAvatarFile, MAX_DESCRIPTION, validateProfile, type ProfileErrors } from '../profile/profileValidation';
import Avatar from './Avatar';

type Props = {
  profile: Profile;
  // Called with the profile returned by the backend after each saved change, so the page shows it at once
  onChange: (profile: Profile) => void;
  onClose: () => void;
};

type Feedback = { kind: 'success' | 'error'; text: string } | null;

const changesOf = (profile: Profile): ProfileChanges => ({
  description: profile.description ?? '',
  location: profile.location ?? '',
  website: profile.website ?? '',
});

/**
 * US-03: the owner edits the description, location, website and avatar of their profile. The avatar is saved as soon
 * as it is chosen or removed; the text fields with "Save profile".
 */
const ProfileEditForm = ({ profile, onChange, onClose }: Props) => {
  const { t, language } = useTranslation();
  const [values, setValues] = useState<ProfileChanges>(() => changesOf(profile));
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [avatarFeedback, setAvatarFeedback] = useState<Feedback>(null);
  const [avatarBusy, setAvatarBusy] = useState(false);

  const update = (field: keyof ProfileChanges) => (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setServerErrors((current) => ({ ...current, [field]: '' }));
  };

  const errorOf = (field: keyof ProfileChanges) => (errors[field] ? t(errors[field]) : serverErrors[field] || '');

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const found = validateProfile(values);
    setErrors(found);
    setFeedback(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      onChange(await updateProfile(values));
      setFeedback({ kind: 'success', text: t('profile.saved') });
    } catch (error) {
      const apiError = toApiError(error, language);
      setServerErrors(apiError.fieldErrors);
      if (Object.keys(apiError.fieldErrors).length === 0) setFeedback({ kind: 'error', text: apiError.message });
    } finally {
      setSaving(false);
    }
  };

  const changeAvatar = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // Lets the same file be chosen again after an error
    event.target.value = '';
    if (!file) return;
    const problem = checkAvatarFile(file);
    if (problem) {
      setAvatarFeedback({ kind: 'error', text: t(problem) });
      return;
    }
    await saveAvatar(() => uploadAvatar(file), 'profile.avatarUpdated');
  };

  const saveAvatar = async (
    call: () => Promise<Profile>,
    successKey: 'profile.avatarUpdated' | 'profile.avatarRemoved'
  ) => {
    setAvatarBusy(true);
    setAvatarFeedback(null);
    try {
      onChange(await call());
      setAvatarFeedback({ kind: 'success', text: t(successKey) });
    } catch (error) {
      const apiError = toApiError(error, language);
      setAvatarFeedback({ kind: 'error', text: apiError.fieldErrors.avatar ?? t('profile.avatarError') });
    } finally {
      setAvatarBusy(false);
    }
  };

  const left = MAX_DESCRIPTION - values.description.length;

  return (
    <section className="profile-edit" aria-labelledby="profile-edit-title">
      <h2 id="profile-edit-title" className="h5 mb-3">
        {t('profile.editTitle')}
      </h2>

      <div className="profile-edit-avatar mb-4">
        <Avatar name={profile.username} avatarUrl={profile.avatarUrl} size="lg" />
        <div>
          <label htmlFor="profile-avatar" className="form-label d-block">
            {t('profile.avatarLabel')}
          </label>
          <div className="d-flex flex-wrap gap-2">
            <input
              id="profile-avatar"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="form-control form-control-sm"
              disabled={avatarBusy}
              aria-describedby="profile-avatar-hint"
              onChange={changeAvatar}
            />
            {profile.avatarUrl && (
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                disabled={avatarBusy}
                onClick={() => saveAvatar(deleteAvatar, 'profile.avatarRemoved')}
              >
                {t('profile.removeAvatar')}
              </button>
            )}
          </div>
          <small id="profile-avatar-hint" className="text-muted">
            {t('profile.avatarHint')}
          </small>
          {avatarFeedback && (
            <p
              role={avatarFeedback.kind === 'error' ? 'alert' : 'status'}
              className={`small mb-0 mt-1 profile-${avatarFeedback.kind}`}
            >
              {avatarFeedback.text}
            </p>
          )}
        </div>
      </div>

      <form onSubmit={submit} noValidate>
        <div className="mb-3">
          <label htmlFor="profile-description" className="form-label">
            {t('profile.descriptionLabel')}
          </label>
          <textarea
            id="profile-description"
            className={`form-control${errorOf('description') ? ' is-invalid' : ''}`}
            rows={4}
            value={values.description}
            aria-describedby="profile-description-hint profile-description-error"
            onChange={update('description')}
          />
          <small id="profile-description-hint" className={left < 0 ? 'text-danger' : 'text-muted'}>
            {t('profile.descriptionHint')} {t('profile.charactersLeft', { count: Math.max(left, 0) })}
          </small>
          <div id="profile-description-error" className="invalid-feedback">
            {errorOf('description')}
          </div>
        </div>

        <div className="row g-3 mb-3">
          <div className="col-md-6">
            <label htmlFor="profile-location" className="form-label">
              {t('profile.locationLabel')}
            </label>
            <input
              id="profile-location"
              className={`form-control${errorOf('location') ? ' is-invalid' : ''}`}
              value={values.location}
              aria-describedby="profile-location-error"
              onChange={update('location')}
            />
            <div id="profile-location-error" className="invalid-feedback">
              {errorOf('location')}
            </div>
          </div>
          <div className="col-md-6">
            <label htmlFor="profile-website" className="form-label">
              {t('profile.websiteLabel')}
            </label>
            <input
              id="profile-website"
              type="url"
              inputMode="url"
              placeholder="https://"
              className={`form-control${errorOf('website') ? ' is-invalid' : ''}`}
              value={values.website}
              aria-describedby="profile-website-hint profile-website-error"
              onChange={update('website')}
            />
            <small id="profile-website-hint" className="text-muted">
              {t('profile.websiteHint')}
            </small>
            <div id="profile-website-error" className="invalid-feedback">
              {errorOf('website')}
            </div>
          </div>
        </div>

        {feedback && (
          <p role={feedback.kind === 'error' ? 'alert' : 'status'} className={`profile-${feedback.kind}`}>
            {feedback.text}
          </p>
        )}

        <div className="d-flex gap-2">
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? t('profile.saving') : t('profile.save')}
          </button>
          <button type="button" className="btn btn-outline-secondary" onClick={onClose}>
            {t('profile.cancel')}
          </button>
        </div>
      </form>
    </section>
  );
};

export default ProfileEditForm;
