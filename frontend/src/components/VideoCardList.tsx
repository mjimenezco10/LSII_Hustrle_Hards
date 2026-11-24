import type { VideoSummary } from '../videos/videosApi';
import VideoCard from './VideoCard';

type Props = {
  videos: VideoSummary[];
  // Accessible name of the list, e.g. "Videos" or "Favourite videos"
  label: string;
};

/**
 * Responsive grid of video cards shared by the home page, favourites, subscriptions and profiles. Nothing is
 * rendered without videos: each page shows its own empty state.
 */
const VideoCardList = ({ videos, label }: Props) => {
  if (videos.length === 0) return null;
  return (
    <ul
      aria-label={label}
      className="row row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xl-4 g-4 list-unstyled video-grid"
    >
      {videos.map((video) => (
        <li key={video.id} className="col">
          <VideoCard video={video} />
        </li>
      ))}
    </ul>
  );
};

export default VideoCardList;
