import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/authContext';
import PagedListFooter from '../components/PagedListFooter';
import VideoCardList from '../components/VideoCardList';
import { useTranslation } from '../i18n/i18nContext';
import { useFavourites } from '../favourites/useFavourites';

/**
 * US-12: favourite videos of the logged-in user, at /favourites, last saved first. Visitors who are not logged in are
 * sent to the login page and come back here afterwards.
 */
const FavouritesPage = () => {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <FavouritesList />;
};

// Separate component so the list is only requested for logged-in users (hooks cannot be called conditionally)
const FavouritesList = () => {
  const { videos, status, hasMore, loadMore, retry } = useFavourites();
  const { t } = useTranslation();

  return (
    <section aria-labelledby="favourites-title" className="text-start">
      <h1 id="favourites-title" className="section-title mb-4">
        {t('library.favouritesTitle')}
      </h1>

      {status === 'success' && videos.length === 0 && (
        <div className="favourites-empty">
          <p className="mb-2">{t('library.favouritesEmpty')}</p>
          <p className="mb-3">{t('library.favouritesEmptyHint')}</p>
          <Link to="/" className="btn btn-primary btn-sm">
            {t('library.browseVideos')}
          </Link>
        </div>
      )}

      <VideoCardList videos={videos} label={t('library.favouritesList')} />

      <PagedListFooter
        status={status}
        hasMore={hasMore}
        onLoadMore={loadMore}
        onRetry={retry}
        loadingText={t('library.favouritesLoading')}
        errorText={t('library.favouritesError')}
        retryText={t('library.retry')}
        loadMoreText={t('library.loadMore')}
      />
    </section>
  );
};

export default FavouritesPage;
