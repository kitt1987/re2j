package com.google.re2j;

import java.util.ArrayList;
import java.util.Collections;

public class RegexpTracks {
    private ArrayList<Track> topmostTracks = new ArrayList<Track>();
    private ArrayList<Track> tracks = new ArrayList<Track>();

    RegexpTracks() {
        topmostTracks.add(Track.NewPlaceholder(0, 0));
    }

    RegexpTracks(RegexpTracks that) {
        topmostTracks.addAll(that.topmostTracks);
        tracks.addAll(that.tracks);
    }

    public void Clear() {
        topmostTracks.clear();
        topmostTracks.add(Track.NewPlaceholder(0, 0));
        tracks.clear();
    }

    public void SetTracks(ArrayList<Track> tracks) {
        this.tracks.addAll(tracks);
        resetTheTopmostTrack();
        buildTopmostTracks();
    }

    public void SetTopmostTracks(ArrayList<Track> tracks) {
        boolean hasOverlappedTracks = false;
        for (Track track : tracks) {
            hasOverlappedTracks |= insertTopmostTrack(track);
        }

        if (hasOverlappedTracks) {
            buildTopmostTracks();
        }

        Collections.sort(topmostTracks);
    }

    public static RegexpTracks JoinTracks(RegexpTracks a, RegexpTracks b) {
        RegexpTracks c = new RegexpTracks();
        c.tracks.addAll(a.tracks);
        c.tracks.addAll(b.tracks);

        c.topmostTracks.addAll(a.tracks);
        // FIXME Possibly need to insert individually.
        c.topmostTracks.addAll(b.tracks);
        return c;
    }

    private void resetTheTopmostTrack() {
        // The first topmost track is ours.
        topmostTracks.remove(0);
        insertTopmostTrack(Track.NewPlaceholder(0, 0));
    }

    private void buildTopmostTracks() {
        for (Track topmost : topmostTracks) {
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

    private boolean insertTopmostTrack(Track track) {
        boolean hasOverlappedTracks = false;
        for (Track topmost : topmostTracks) {
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

        topmostTracks.add(track);
        return hasOverlappedTracks;
    }

    public ArrayList<Track> GetTracks() {
        return this.tracks;
    }

    public ArrayList<Track> GetTopmostTracks() {
        return this.topmostTracks;
    }

    public ArrayList<Track> GetAllTracks() {
        return this.tracks;
    }
}
