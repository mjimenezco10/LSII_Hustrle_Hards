import { useCallback, useEffect, useState } from 'react';
import type { VideoSummary } from '../videos/videosApi';
import { fetchProfile, fetchProfileVideos, type Profile } from './profileApi';

export const PROFILE_PAGE_SIZE = 12;

type Status = 'loading' | 'success' | 'error' | 'notFound';

/**
 * US-03: loads a profile. `setProfile` shows the changes saved by its owner at once, without loading it again.
 */
export function useProfile(name: string) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [status, setStatus] = useState<Status>('loading');
  const [attempt, setAttempt] = useState(0);
  const [currentName, setCurrentName] = useState(name);

  // Reset while rendering so the previous profile is never shown under the new name
  if (name !== currentName) {
    setCurrentName(name);
    setProfile(null);
    setStatus('loading');
  }

  useEffect(() => {
    let cancelled = false;
    fetchProfile(currentName)
      .then((result) => {
        if (cancelled) return;
        setProfile(result);
        setStatus('success');
      })
      .catch((error: { response?: { status?: number } }) => {
        if (!cancelled) setStatus(error.response?.status === 404 ? 'notFound' : 'error');
      });
    return () => {
      cancelled = true;
    };
  }, [currentName, attempt]);

  const retry = useCallback(() => {
    setStatus('loading');
    setAttempt((current) => current + 1);
  }, []);

  return { profile, status, setProfile, retry };
}

/**
 * Videos of a profile page by page, like useVideoList: loadMore() appends the next page, retry() repeats a failed
 * request.
 */
export function useProfileVideos(name: string) {
  const [videos, setVideos] = useState<VideoSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [currentName, setCurrentName] = useState(name);

  if (name !== currentName) {
    setCurrentName(name);
    setVideos([]);
    setPage(0);
    setStatus('loading');
  }

  useEffect(() => {
    let cancelled = false;
    fetchProfileVideos(currentName, page, PROFILE_PAGE_SIZE)
      .then((result) => {
        if (cancelled) return;
        setVideos((current) => (page === 0 ? result.content : [...current, ...result.content]));
        setTotalPages(result.totalPages);
        setStatus('success');
      })
      .catch(() => {
        if (!cancelled) setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, [currentName, page, attempt]);

  const loadMore = useCallback(() => {
    setStatus('loading');
    setPage((current) => current + 1);
  }, []);

  const retry = useCallback(() => {
    setStatus('loading');
    setAttempt((current) => current + 1);
  }, []);

  return { videos, status, hasMore: page + 1 < totalPages, loadMore, retry };
}
