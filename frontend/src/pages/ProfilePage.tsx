import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/authContext';
import Avatar from '../components/Avatar';
import ProfileEditForm from '../components/ProfileEditForm';
import SubscribeButton from '../components/SubscribeButton';
import PagedListFooter from '../components/PagedListFooter';
import VideoCardList from '../components/VideoCardList';
import { useTranslation } from '../i18n/i18nContext';
import { useProfile, useProfileVideos } from '../profile/useProfile';
import type { Profile } from '../profile/profileApi';
import './ProfilePage.css';

/**
 * US-03: profile of a user or channel at /profile/:name, with its videos. Anyone sees the public information; the
 * owner also gets "Edit profile", and the changes they save are shown at once.
 */
const ProfilePage = () => {
  const { name = '' } = useParams();
  const { t } = useTranslation();
  const { profile, status, setProfile, retry } = useProfile(name);

  if (status === 'loading') {
    return (
      <p role="status" className="text-muted">
        {t('profile.loading')}
      </p>
    );
  }
  if (status === 'notFound') {
    return (
      <section className="text-center py-5">
        <h1 className="h3">{t('profile.notFoundTitle')}</h1>
        <p className="text-muted">{t('profile.notFoundText', { name })}</p>
        <Link to="/" className="btn btn-primary">
          {t('profile.backHome')}
        </Link>
      </section>
    );
  }
  if (status === 'error' || !profile) {
    return (
      <div role="alert" className="alert alert-danger d-flex align-items-center gap-3">
        <span>{t('profile.loadError')}</span>
        <button type="button" className="btn btn-outline-danger btn-sm" onClick={retry}>
          {t('profile.retry')}
        </button>
      </div>
    );
  }
  // key: a different profile starts with the edit form closed
  return <ProfileView key={profile.username} profile={profile} onChange={setProfile} />;
};

const ProfileView = ({ profile, onChange }: { profile: Profile; onChange: (profile: Profile) => void }) => {
  const { user } = useAuth();
  const { t, language } = useTranslation();
  const [editing, setEditing] = useState(false);
  const isOwner = user?.username === profile.username;
  const joined =
    profile.joinedAt &&
    new Intl.DateTimeFormat(language === 'en' ? 'en-GB' : language, { dateStyle: 'long' }).format(
      new Date(profile.joinedAt)
    );

  return (
    <div className="profile-page text-start">
      <header className="profile-header">
        <Avatar
          name={profile.username}
          avatarUrl={profile.avatarUrl}
          size="lg"
          alt={t('profile.avatarAlt', { name: profile.username })}
        />
        <div className="profile-info">
          <h1 className="profile-name">{profile.username}</h1>
          <p className="profile-facts text-muted mb-2">
            <span>{t('profile.videoCount', { count: profile.videoCount })}</span>
            {joined && (
              <>
                <span aria-hidden="true"> · </span>
                <time dateTime={profile.joinedAt ?? undefined}>{t('profile.joined', { date: joined })}</time>
              </>
            )}
            {profile.location && (
              <>
                <span aria-hidden="true"> · </span>
                <span>{profile.location}</span>
              </>
            )}
          </p>
          {!profile.registered && <p className="text-muted small mb-2">{t('profile.channelOnly')}</p>}
          {profile.registered && (
            <p className="profile-description">{profile.description || t('profile.noDescription')}</p>
          )}
          {profile.website && (
            <a href={profile.website} target="_blank" rel="noopener noreferrer nofollow" className="profile-website">
              {profile.website}
            </a>
          )}
        </div>
        <div className="profile-actions">
          {isOwner ? (
            !editing && (
              <button type="button" className="btn btn-outline-primary" onClick={() => setEditing(true)}>
                {t('profile.editProfile')}
              </button>
            )
          ) : (
            // US-13: follow the channel from its profile too (the button hides itself on your own channel)
            <SubscribeButton channel={profile.username} />
          )}
        </div>
      </header>

      {isOwner && editing && (
        <ProfileEditForm profile={profile} onChange={onChange} onClose={() => setEditing(false)} />
      )}

      <ProfileVideos name={profile.username} />
    </div>
  );
};

const ProfileVideos = ({ name }: { name: string }) => {
  const { t } = useTranslation();
  const { videos, status, hasMore, loadMore, retry } = useProfileVideos(name);

  return (
    <section aria-labelledby="profile-videos-title" className="mt-4">
      <h2 id="profile-videos-title" className="section-title mb-3">
        {t('profile.videosTitle')}
      </h2>
      {status === 'success' && videos.length === 0 && <p className="text-muted">{t('profile.videosEmpty')}</p>}
      <VideoCardList videos={videos} label={t('profile.videosList', { name })} />
      <PagedListFooter
        status={status}
        hasMore={hasMore}
        onLoadMore={loadMore}
        onRetry={retry}
        loadingText={t('profile.videosLoading')}
        errorText={t('profile.videosError')}
        retryText={t('profile.retry')}
        loadMoreText={t('profile.loadMore')}
      />
    </section>
  );
};

export default ProfilePage;
