package com.google.re2j;

import java.util.ArrayList;
import java.util.Collections;

public class RegexpTracks {
    private ArrayList<Track> topmostTracks = new ArrayList<Track>();
    private ArrayList<Track> tracks = new ArrayList<Track>();

    RegexpTracks() {

    }

    RegexpTracks(RegexpTracks that) {
        topmostTracks.addAll(that.topmostTracks);
        tracks.addAll(that.tracks);
    }

    public void Clear() {
        topmostTracks.clear();
        tracks.clear();
    }

    public void SetTracks(ArrayList<Track> tracks) {
        this.tracks.addAll(tracks);
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

    private void buildTopmostTracks() {
        for (Track topmost : topmostTracks) {

        }
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

        return hasOverlappedTracks;
    }

    public ArrayList<Track> GetTracks() {
        return this.tracks;
    }

    public ArrayList<Track> GetAllTracks() {
        return this.tracks;
    }
}
