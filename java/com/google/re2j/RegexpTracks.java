package com.google.re2j;

import java.util.ArrayList;
import java.util.Collections;

public class RegexpTracks {
    // composed tracks of the current regexp
    private final ArrayList<Track> composedTracks = new ArrayList<Track>();

    // all elementary tracks of the current regexp
    private final ArrayList<Track> tracks = new ArrayList<Track>();

    RegexpTracks() {
    }

    public void Clear() {
        composedTracks.clear();
        tracks.clear();
    }

    public ArrayList<Track> GetTracks() {
        return tracks;
    }

    public ArrayList<Track> GetComposedTracks() {
        return composedTracks;
    }

    public void ComposeTracks(ArrayList<Track> tracks) {
        if (tracks.size() == 0) {
            return;
        }
        // FIXME tracks must be consecutive. Validate them.
        this.tracks.addAll(tracks);
        if (tracks.size() > 1) {
            insertComposedTrack(Track.NewPlaceholder(tracks));
            buildComposedTracks();
            return;
        }

        composeTracks();
        Collections.sort(this.composedTracks);
    }

    public void AddTracks(RegexpTracks tracks) {
        this.tracks.addAll(tracks.GetTracks());
        Collections.sort(this.tracks);
        this.composedTracks.addAll(tracks.GetComposedTracks());
        composeTracks();
        Collections.sort(this.composedTracks);
        // FIXME validate composedTracks
    }

    private void composeTracks() {
        if (tracks.size() <= 1) {
            return;
        }

        ArrayList<Track> consecutive = new ArrayList<Track>();
        for (Track track : tracks) {
            if (consecutive.size() == 0 || consecutive.get(consecutive.size()-1).End == track.Start) {
                consecutive.add(track);
                continue;
            }

            if (consecutive.size() > 1) {
                if (insertComposedTrack(Track.NewPlaceholder(consecutive))) {
                    buildComposedTracks();
                }
            }

            consecutive.clear();
            consecutive.add(track);
        }

        if (consecutive.size() > 1 && insertComposedTrack(Track.NewPlaceholder(consecutive))) {
            buildComposedTracks();
        }
    }

    private void buildComposedTracks() {
        for (Track topmost : composedTracks) {
            if (!topmost.IsPlaceholder()) {
                continue;
            }

            topmost.Freeze(findTracks(topmost.Start, topmost.End));
        }
    }

    private ArrayList<Track> findTracks(int start, int end) {
        ArrayList<Track> matched = new ArrayList<Track>();
        for (Track track : tracks) {
            if (track.Start >= start && track.End <= end) {
                matched.add(track);
            }
        }

        if (matched.size() == 0) {
            throw new IllegalStateException("no track matches the range [" + start + "," + end + ")");
        }

        return matched;
    }

    private boolean insertComposedTrack(Track track) {
        boolean hasOverlappedTracks = false;
        for (Track composed : composedTracks) {
            if (track.Start >= composed.End || track.End <= composed.Start) {
                continue;
            }

            // track.Start < composed.End && track.End > composed.Start

            if (track.Start == composed.Start && track.End == composed.End) {
                if (hasOverlappedTracks) {
                    throw new IllegalStateException("composed tracks should not overlap each other");
                }
                return false;
            }

            if (track.Start >= composed.Start) {
                if (track.End <= composed.End) {
                    // the composed contains track
                    return false;
                }

                if (track.Start == composed.Start) {
                    // track contains the composed
                    composed.UpdateRange(composed.Start, track.End);
                    return true;
                }

                // divide the composed track
                composed.UpdateRange(composed.Start, track.Start);
                hasOverlappedTracks = true;
                continue;
            }

            // else track.Start < composed.Start
            if (track.End >= composed.End) {
                // track contains the composed
                composed.UpdateRange(track.Start, track.End);
                return true;
            }

            composed.UpdateRange(track.End, composed.End);
            hasOverlappedTracks = true;
        }

        composedTracks.add(track);
        return hasOverlappedTracks | track.IsPlaceholder();
    }
}