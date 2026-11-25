import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProfilePage from '../ProfilePage';
import { AuthContext, type AuthContextValue } from '../../auth/authContext';
import { I18nProvider } from '../../i18n/I18nProvider';
import { LANGUAGE_STORAGE_KEY } from '../../i18n/languagePreference';
import {
  deleteAvatar,
  fetchProfile,
  fetchProfileVideos,
  updateProfile,
  uploadAvatar,
  type Profile,
} from '../../profile/profileApi';
import { fetchSubscription } from '../../subscriptions/subscriptionsApi';
import type { VideoSummary } from '../../videos/videosApi';

jest.mock('../../profile/profileApi');
jest.mock('../../subscriptions/subscriptionsApi', () => ({
  ...jest.requireActual('../../subscriptions/subscriptionsApi'),
  fetchSubscription: jest.fn(),
}));

const mockedFetchProfile = fetchProfile as jest.MockedFunction<typeof fetchProfile>;
const mockedFetchVideos = fetchProfileVideos as jest.MockedFunction<typeof fetchProfileVideos>;
const mockedUpdate = updateProfile as jest.MockedFunction<typeof updateProfile>;
const mockedUpload = uploadAvatar as jest.MockedFunction<typeof uploadAvatar>;
const mockedDelete = deleteAvatar as jest.MockedFunction<typeof deleteAvatar>;
const mockedFetchSubscription = fetchSubscription as jest.MockedFunction<typeof fetchSubscription>;

const alice = { id: 1, username: 'alice', email: 'alice@example.com' };

const aliceProfile: Profile = {
  username: 'alice',
  registered: true,
  avatarUrl: null,
  description: 'I film mountains.',
  location: 'Mataró',
  website: 'https://alice.example.com',
  joinedAt: '2026-03-05T10:00:00Z',
  videoCount: 1,
};

const video = (id: number): VideoSummary => ({
  id,
  title: `Video ${id}`,
  author: 'alice',
  durationSeconds: 60,
  publishedAt: '2026-01-01T00:00:00Z',
  thumbnailUrl: `/media/${id}.webp`,
  videoUrl: `/media/${id}.mp4`,
  ageRestricted: false,
});

const page = (videos: VideoSummary[], pageNumber = 0, totalPages = 1) => ({
  content: videos,
  page: pageNumber,
  size: 12,
  totalElements: videos.length,
  totalPages,
});

const renderPage = (name: string, user: AuthContextValue['user'] = null) =>
  render(
    <I18nProvider>
      <AuthContext.Provider
        value={{
          user,
          login: jest.fn(),
          register: jest.fn(),
          loginWithGoogle: jest.fn(),
          logout: jest.fn(),
          updateSession: jest.fn(),
        }}
      >
        <MemoryRouter initialEntries={[`/profile/${name}`]}>
          <Routes>
            <Route path="/profile/:name" element={<ProfilePage />} />
            <Route path="/" element={<p>Home page</p>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </I18nProvider>
  );

describe('ProfilePage', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    localStorage.clear();
    mockedFetchProfile.mockResolvedValue(aliceProfile);
    mockedFetchVideos.mockResolvedValue(page([video(1)]));
    mockedFetchSubscription.mockResolvedValue({ channel: 'alice', subscriberCount: 3, subscribed: false });
  });

  it('shows the public profile and the videos of another user, without edit controls', async () => {
    renderPage('alice', { ...alice, username: 'bob' });

    expect(screen.getByRole('status')).toHaveTextContent('Loading profile...');
    expect(await screen.findByRole('heading', { level: 1, name: 'alice' })).toBeInTheDocument();
    expect(screen.getByText('I film mountains.')).toBeInTheDocument();
    expect(screen.getByText('Mataró')).toBeInTheDocument();
    expect(screen.getByText('Joined 5 March 2026')).toBeInTheDocument();
    expect(screen.getByText('1 video')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'https://alice.example.com' })).toHaveAttribute(
      'rel',
      'noopener noreferrer nofollow'
    );
    expect(await screen.findByRole('button', { name: /subscribe/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit profile' })).not.toBeInTheDocument();
    const list = await screen.findByRole('list', { name: 'Videos of alice' });
    expect(within(list).getByText('Video 1')).toBeInTheDocument();
    expect(mockedFetchProfile).toHaveBeenCalledWith('alice');
  });

  it('shows the uploaded avatar with its text alternative', async () => {
    mockedFetchProfile.mockResolvedValue({ ...aliceProfile, avatarUrl: '/media/avatars/a.webp' });
    renderPage('alice');

    expect(await screen.findByRole('img', { name: 'Avatar of alice' })).toHaveAttribute(
      'src',
      'development/avatars/a.webp'
    );
  });

  it('shows an imported channel with only its name and videos', async () => {
    mockedFetchProfile.mockResolvedValue({
      ...aliceProfile,
      username: 'Nature Channel',
      registered: false,
      description: null,
      location: null,
      website: null,
      joinedAt: null,
      videoCount: 3,
    });
    renderPage('Nature Channel');

    expect(await screen.findByText('Channel of imported videos')).toBeInTheDocument();
    expect(screen.getByText('3 videos')).toBeInTheDocument();
    expect(screen.queryByText(/Joined/)).not.toBeInTheDocument();
    expect(screen.queryByText('No description yet.')).not.toBeInTheDocument();
  });

  it('says when a profile does not exist', async () => {
    mockedFetchProfile.mockRejectedValue({ response: { status: 404 } });
    renderPage('ghost');

    expect(await screen.findByRole('heading', { name: 'Profile not found' })).toBeInTheDocument();
    expect(screen.getByText('There is no user or channel called "ghost".')).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('link', { name: 'Back to home' }));
    expect(screen.getByText('Home page')).toBeInTheDocument();
  });

  it('lets the user retry when the profile cannot be loaded', async () => {
    const user = userEvent.setup();
    mockedFetchProfile.mockRejectedValueOnce(new Error('offline'));
    renderPage('alice');

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load this profile.');
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'alice' })).toBeInTheDocument();
  });

  it('shows an empty list, loads more videos and retries a failed page', async () => {
    const user = userEvent.setup();
    mockedFetchVideos.mockResolvedValueOnce(page([video(1)], 0, 2)).mockRejectedValueOnce(new Error('offline'));
    renderPage('alice');

    await user.click(await screen.findByRole('button', { name: 'Load more' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load the videos.');
    mockedFetchVideos.mockResolvedValueOnce(page([video(2)], 1, 2));
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    const list = await screen.findByRole('list', { name: 'Videos of alice' });
    expect(await within(list).findByText('Video 2')).toBeInTheDocument();
    expect(within(list).getByText('Video 1')).toBeInTheDocument();
  });

  it('says when a profile has no videos or description', async () => {
    mockedFetchProfile.mockResolvedValue({ ...aliceProfile, description: null, website: null, location: null });
    mockedFetchVideos.mockResolvedValue(page([], 0, 0));
    renderPage('alice');

    expect(await screen.findByText('No description yet.')).toBeInTheDocument();
    expect(await screen.findByText('No videos yet.')).toBeInTheDocument();
  });

  describe('own profile', () => {
    it('edits the description, location and website and shows them at once', async () => {
      const user = userEvent.setup();
      mockedUpdate.mockResolvedValue({
        ...aliceProfile,
        description: 'Now I film rivers.',
        location: 'Girona',
        website: 'https://rivers.example.com',
      });
      renderPage('alice', alice);

      await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
      expect(screen.queryByRole('button', { name: /subscribe/i })).not.toBeInTheDocument();
      const description = screen.getByLabelText('Description');
      expect(description).toHaveValue('I film mountains.');
      await user.clear(description);
      await user.type(description, 'Now I film rivers.');
      await user.clear(screen.getByLabelText('Location'));
      await user.type(screen.getByLabelText('Location'), 'Girona');
      await user.clear(screen.getByLabelText('Website'));
      await user.type(screen.getByLabelText('Website'), 'https://rivers.example.com');
      await user.click(screen.getByRole('button', { name: 'Save profile' }));

      expect(mockedUpdate).toHaveBeenCalledWith({
        description: 'Now I film rivers.',
        location: 'Girona',
        website: 'https://rivers.example.com',
      });
      expect(await screen.findByText('Profile saved.')).toBeInTheDocument();
      const header = screen.getByRole('banner');
      expect(within(header).getByText('Now I film rivers.')).toBeInTheDocument();
      expect(within(header).getByText('Girona')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.queryByLabelText('Description')).not.toBeInTheDocument();
    });

    it('checks the fields before saving', async () => {
      const user = userEvent.setup();
      renderPage('alice', alice);

      await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
      await user.clear(screen.getByLabelText('Website'));
      await user.type(screen.getByLabelText('Website'), 'alice.example.com');
      await user.click(screen.getByRole('button', { name: 'Save profile' }));

      expect(screen.getByText('Website must start with http:// or https://')).toBeInTheDocument();
      expect(screen.getByLabelText('Website')).toHaveClass('is-invalid');
      expect(mockedUpdate).not.toHaveBeenCalled();

      await user.type(screen.getByLabelText('Website'), '/x');
      expect(screen.getByLabelText('Website')).not.toHaveClass('is-invalid');
    });

    it('shows the errors of the backend next to their field or above the buttons', async () => {
      const user = userEvent.setup();
      mockedUpdate
        .mockRejectedValueOnce({ response: { data: { message: 'Bad', fieldErrors: { location: 'Too long here' } } } })
        .mockRejectedValueOnce({ response: { data: { message: 'Server is busy', fieldErrors: {} } } });
      renderPage('alice', alice);

      await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
      await user.click(screen.getByRole('button', { name: 'Save profile' }));
      expect(await screen.findByText('Too long here')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'Save profile' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('Server is busy');
    });

    it('uploads, rejects and removes the avatar', async () => {
      const user = userEvent.setup({ applyAccept: false });
      mockedUpload.mockResolvedValue({ ...aliceProfile, avatarUrl: '/media/avatars/new.png' });
      mockedDelete.mockResolvedValue(aliceProfile);
      renderPage('alice', alice);

      await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
      const input = screen.getByLabelText('Avatar');
      await user.upload(input, new File(['x'], 'me.gif', { type: 'image/gif' }));
      expect(screen.getByRole('alert')).toHaveTextContent('The avatar must be a JPG, PNG or WebP image');
      expect(mockedUpload).not.toHaveBeenCalled();

      const file = new File(['x'], 'me.png', { type: 'image/png' });
      await user.upload(input, file);
      expect(mockedUpload).toHaveBeenCalledWith(file);
      expect(await screen.findByText('Avatar updated.')).toBeInTheDocument();
      expect(screen.getByRole('img', { name: 'Avatar of alice' })).toHaveAttribute(
        'src',
        'development/avatars/new.png'
      );

      await user.click(screen.getByRole('button', { name: 'Remove avatar' }));
      expect(await screen.findByText('Avatar removed.')).toBeInTheDocument();
      expect(screen.queryByRole('img', { name: 'Avatar of alice' })).not.toBeInTheDocument();
    });

    it('shows the error of a failed avatar upload', async () => {
      const user = userEvent.setup();
      mockedUpload
        .mockRejectedValueOnce({ response: { data: { message: 'x', fieldErrors: { avatar: 'Too big for us' } } } })
        .mockRejectedValueOnce(new Error('offline'));
      renderPage('alice', alice);

      await user.click(await screen.findByRole('button', { name: 'Edit profile' }));
      await user.upload(screen.getByLabelText('Avatar'), new File(['x'], 'me.png', { type: 'image/png' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('Too big for us');
      await user.upload(screen.getByLabelText('Avatar'), new File(['x'], 'me.png', { type: 'image/png' }));
      expect(await screen.findByText('Could not update the avatar.')).toBeInTheDocument();
    });
  });

  it('is translated to Catalan', async () => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, 'ca');
    renderPage('alice', alice);

    expect(await screen.findByRole('button', { name: 'Edita el perfil' })).toBeInTheDocument();
    expect(screen.getByText('1 vídeo')).toBeInTheDocument();
    expect(screen.getByText("S'hi va unir el 5 de març del 2026")).toBeInTheDocument();
  });
});
