package com.google.re2j;

import java.util.ArrayList;
import java.util.Collections;

// Both RegexpTracks and Tracks do not calculate on any regexps.
// Instead, they just record states generated by the parser loop.
public class RegexpTracks {
    // L1: all elementary tracks of the current regexp
    private final ArrayList<Track> tracks = new ArrayList<Track>();

    // L2: composed tracks of the current regexp
    private final ArrayList<Track> composedTracks = new ArrayList<Track>();

    // L3: concat consecutive composed and individual tracks, as well as topmost tracks of subs.
    // A Regexp has only 1 topmost track unless concatenating 2 tracks both have topmost tracks.
    // We should build topmost tracks whenever each relative factor updated since we can't restore the Regexp if it
    // is removed from the current stack.
    private final ArrayList<Track> topmostTracks = new ArrayList<Track>(1);

    private final Regexp re;

    RegexpTracks(Regexp re) {
        this.re = re;
    }

    public void Clear() {
        topmostTracks.clear();
        composedTracks.clear();
        tracks.clear();
    }

    public ArrayList<Track> GetTracks() {
        return tracks;
    }

    public ArrayList<Track> GetComposedTracks() {
        return composedTracks;
    }

    // Only tracks scanned in a single parser loop can be composed together.
    public void ComposeTracks(ArrayList<Track> tracks) {
        if (tracks.size() == 0) {
            return;
        }
        // FIXME tracks must be consecutive. Validate them.
        this.tracks.addAll(tracks);
        if (tracks.size() > 1) {
            insertComposedTrack(Track.NewPlaceholder(tracks));
            buildComposedTracks();
        }

        Collections.sort(this.composedTracks);
        composeTopmostTracks();
    }

    // concat two RegexpTracks
    public void AddTracks(RegexpTracks that) {
        tracks.addAll(that.tracks);
        Collections.sort(tracks);
        composedTracks.addAll(that.composedTracks);
        Collections.sort(composedTracks);
        topmostTracks.addAll(that.topmostTracks);
        Collections.sort(topmostTracks);
        // FIXME validate composedTracks
    }

    // particular cases for different types of Regexps
    public void ConcatLiterals(RegexpTracks that) {
        if (composedTracks.size() > 0) {
            throw new IllegalStateException("literal track should have no composed track but " + composedTracks.size());
        }

        if (this.tracks.size() > 1) {
            throw new IllegalStateException("literal track should have only 1 track but " + tracks.size());
        }

        if (that.composedTracks.size() > 0) {
            throw new IllegalStateException("literal track should have no composed track but " + that.composedTracks.size());
        }

        if (that.tracks.size() > 1) {
            throw new IllegalStateException("literal track should have only 1 track but " + that.tracks.size());
        }

        if (tracks.get(0).End != that.tracks.get(0).Start) {
            throw new IllegalStateException("concatenated tracks should be consecutive");
        }

        StringBuilder b = new StringBuilder();
        for (int r : re.runes) {
            b.appendCodePoint(r);
        }

        tracks.get(0).Freeze(that.tracks.get(0).End, b.toString());
    }

    private void composeTopmostTracks() {
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

            topmost.Freeze(findTracks(topmost.Start, topmost.End), re);
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

    private boolean ccFromAlternation;
    public void SetFlagCCFromAlternation() {
        ccFromAlternation = true;
    }
    public boolean IsFromAlternation() {
        return ccFromAlternation;
    }
}
