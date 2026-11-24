import { useTranslation } from '../i18n/i18nContext';
import { useVideoList } from '../videos/useVideoList';
import PagedListFooter from './PagedListFooter';
import VideoCardList from './VideoCardList';

// Placeholders shown while the first page loads; fixed ids because they never change
const SKELETON_IDS = Array.from({ length: 8 }, (_, index) => `skeleton-${index}`);

type Props = {
  // US-08: when not empty, only the videos matching it, most relevant first
  query?: string;
};

/**
 * US-04: home page list of videos, newest first, with a "Load more" button for the next pages.
 */
const VideoGrid = ({ query = '' }: Props) => {
  const { t } = useTranslation();
  const { videos, status, hasMore, loadMore, retry } = useVideoList(query);

  if (status === 'success' && videos.length === 0) {
    return <p className="text-muted">{query ? t('videos.noResults', { query }) : t('videos.empty')}</p>;
  }

  return (
    <section>
      <VideoCardList videos={videos} label={query ? t('videos.resultsFor', { query }) : t('videos.listLabel')} />

      {status === 'loading' && videos.length === 0 && (
        <ul className="row row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xl-4 g-4 list-unstyled" aria-hidden="true">
          {SKELETON_IDS.map((id) => (
            <li key={id} className="col">
              <div className="video-card-skeleton">
                <div className="skeleton-thumbnail" />
                <div className="skeleton-line" />
                <div className="skeleton-line is-short" />
              </div>
            </li>
          ))}
        </ul>
      )}

      <PagedListFooter
        status={status}
        hasMore={hasMore}
        onLoadMore={loadMore}
        onRetry={retry}
        loadingText={t('videos.loading')}
        errorText={t('videos.loadError')}
        retryText={t('videos.retry')}
        loadMoreText={t('videos.loadMore')}
        hideLoadingText={videos.length === 0}
      />
    </section>
  );
};

export default VideoGrid;
