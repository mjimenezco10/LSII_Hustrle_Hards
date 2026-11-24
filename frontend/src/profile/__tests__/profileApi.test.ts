import axios from 'axios';
import { deleteAvatar, fetchProfile, fetchProfileVideos, updateProfile, uploadAvatar } from '../profileApi';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

const profile = {
  username: 'alice',
  registered: true,
  avatarUrl: null,
  description: 'Hi',
  location: null,
  website: null,
  joinedAt: '2026-01-01T00:00:00Z',
  videoCount: 2,
};

describe('profileApi', () => {
  beforeEach(() => jest.resetAllMocks());

  it('fetches a profile, encoding the name', async () => {
    mockedAxios.get.mockResolvedValueOnce({ data: profile });

    await expect(fetchProfile('Cat & Dog')).resolves.toEqual(profile);
    expect(mockedAxios.get).toHaveBeenCalledWith('other/profiles/Cat%20%26%20Dog');
  });

  it('fetches one page of the videos of a profile', async () => {
    const page = { content: [], page: 1, size: 12, totalElements: 0, totalPages: 0 };
    mockedAxios.get.mockResolvedValueOnce({ data: page });

    await expect(fetchProfileVideos('alice', 1, 12)).resolves.toEqual(page);
    expect(mockedAxios.get).toHaveBeenCalledWith('other/profiles/alice/videos', { params: { page: 1, size: 12 } });
  });

  it('saves the profile changes', async () => {
    mockedAxios.put.mockResolvedValueOnce({ data: profile });
    const changes = { description: 'Hi', location: '', website: '' };

    await expect(updateProfile(changes)).resolves.toEqual(profile);
    expect(mockedAxios.put).toHaveBeenCalledWith('other/account/profile', changes);
  });

  it('uploads the avatar as multipart form data', async () => {
    mockedAxios.put.mockResolvedValueOnce({ data: profile });
    const file = new File(['x'], 'me.png', { type: 'image/png' });

    await uploadAvatar(file);
    const [url, body] = mockedAxios.put.mock.calls[0];
    expect(url).toBe('other/account/avatar');
    expect((body as FormData).get('avatar')).toBe(file);
  });

  it('removes the avatar', async () => {
    mockedAxios.delete.mockResolvedValueOnce({ data: profile });

    await expect(deleteAvatar()).resolves.toEqual(profile);
    expect(mockedAxios.delete).toHaveBeenCalledWith('other/account/avatar');
  });
});
