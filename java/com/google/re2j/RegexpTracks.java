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
        // FIXME tracks must be sequential. Validate them.
        this.tracks.addAll(tracks);
        if (tracks.size() > 1) {
            insertComposedTrack(Track.NewPlaceholder(tracks));
            buildComposedTracks();
        }
    }

    public void AddTracks(RegexpTracks tracks) {
        this.tracks.addAll(tracks.GetTracks());
        Collections.sort(this.tracks);
        this.composedTracks.addAll(tracks.GetComposedTracks());
        Collections.sort(this.composedTracks);
        // FIXME validate composedTracks
    }

    private void composeTracks() {
        ArrayList<Track> 
        for (Track track : tracks) {

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
        for (Track topmost : composedTracks) {
            if (topmost.Start <= track.Start && topmost.End > track.Start) {
                topmost.UpdateRange(topmost.Start, track.Start);
                hasOverlappedTracks = true;
                continue;
            }

            if (topmost.Start >= track.Start && topmost.Start < track.End) {
                topmost.UpdateRange(track.End, topmost.End);
                hasOverlappedTracks = true;
            }
        }

        composedTracks.add(track);
        return hasOverlappedTracks;
    }
}
