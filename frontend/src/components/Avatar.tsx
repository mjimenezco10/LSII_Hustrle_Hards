import type { CSSProperties } from 'react';
import { avatarHue } from '../utils/avatarHue';
import { toMediaUrl } from '../videos/videosApi';

type Props = {
  name: string;
  size?: 'sm' | 'md' | 'lg';
  // US-03: path under /media of the avatar the user uploaded; without it the initial is shown
  avatarUrl?: string | null;
  // Text alternative for the image; empty (decorative) when the name is already shown next to it
  alt?: string;
};

/**
 * Round avatar of a user or channel: the uploaded picture, or the first letter of the name coloured from the name.
 * Decorative by default: the name is always shown next to it.
 */
const Avatar = ({ name, size = 'md', avatarUrl, alt = '' }: Props) => {
  if (avatarUrl) {
    return <img src={toMediaUrl(avatarUrl)} alt={alt} className={`avatar avatar-${size} avatar-image`} />;
  }

  const initial = Array.from(name.trim())[0]?.toUpperCase() ?? '?';
  const style = { '--avatar-hue': avatarHue(name) } as CSSProperties;

  return (
    <span className={`avatar avatar-${size}`} style={style} aria-hidden="true">
      {initial}
    </span>
  );
};

export default Avatar;
