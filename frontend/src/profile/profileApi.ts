import axios from 'axios';
import { getEnv } from '../utils/Env';
import type { Page, VideoSummary } from '../videos/videosApi';

/**
 * US-03: public profile of a user or channel. Imported channels (videos of the store, no account) are not
 * `registered`: they only have a name and videos. Never contains private data such as the email.
 */
export type Profile = {
  username: string;
  registered: boolean;
  // Path under /media, e.g. /media/avatars/<id>.webp, or null without avatar
  avatarUrl: string | null;
  description: string | null;
  location: string | null;
  website: string | null;
  // ISO date, null for imported channels
  joinedAt: string | null;
  videoCount: number;
};

export type ProfileChanges = {
  description: string;
  location: string;
  website: string;
};

const api = () => getEnv().API_BASE_URL;

export async function fetchProfile(name: string): Promise<Profile> {
  const response = await axios.get<Profile>(`${api()}/profiles/${encodeURIComponent(name)}`);
  return response.data;
}

/**
 * One page of the videos of a profile, newest first.
 */
export async function fetchProfileVideos(name: string, page: number, size: number): Promise<Page<VideoSummary>> {
  const response = await axios.get<Page<VideoSummary>>(`${api()}/profiles/${encodeURIComponent(name)}/videos`, {
    params: { page, size },
  });
  return response.data;
}

// The calls below are made as the logged-in user: AuthProvider sends the token in the Authorization header

export async function updateProfile(changes: ProfileChanges): Promise<Profile> {
  const response = await axios.put<Profile>(`${api()}/account/profile`, changes);
  return response.data;
}

export async function uploadAvatar(file: File): Promise<Profile> {
  const form = new FormData();
  form.append('avatar', file);
  const response = await axios.put<Profile>(`${api()}/account/avatar`, form);
  return response.data;
}

export async function deleteAvatar(): Promise<Profile> {
  const response = await axios.delete<Profile>(`${api()}/account/avatar`);
  return response.data;
}
