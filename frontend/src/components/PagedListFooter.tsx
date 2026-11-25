type Props = {
  status: 'loading' | 'success' | 'error';
  hasMore: boolean;
  onLoadMore: () => void;
  onRetry: () => void;
  loadingText: string;
  errorText: string;
  retryText: string;
  loadMoreText: string;
  // Keeps the loading text for screen readers only (e.g. while skeleton placeholders are shown)
  hideLoadingText?: boolean;
};

/**
 * Bottom of a list loaded page by page: the loading message, the error with "Try again", or "Load more" when there
 * are more pages. Shared by every paged video list (useVideoList, useFavourites, useSubscriptionVideos,
 * useProfileVideos return the same status).
 */
const PagedListFooter = ({
  status,
  hasMore,
  onLoadMore,
  onRetry,
  loadingText,
  errorText,
  retryText,
  loadMoreText,
  hideLoadingText = false,
}: Props) => (
  <div className="d-flex flex-column align-items-center gap-2 my-4">
    {status === 'loading' && (
      <p role="status" className={hideLoadingText ? 'visually-hidden' : 'text-muted mb-0'}>
        {loadingText}
      </p>
    )}
    {status === 'error' && (
      <div role="alert" className="alert alert-danger d-flex align-items-center gap-3 mb-0">
        <span>{errorText}</span>
        <button type="button" className="btn btn-outline-danger btn-sm" onClick={onRetry}>
          {retryText}
        </button>
      </div>
    )}
    {status === 'success' && hasMore && (
      <button type="button" className="btn btn-primary" onClick={onLoadMore}>
        {loadMoreText}
      </button>
    )}
  </div>
);

export default PagedListFooter;
