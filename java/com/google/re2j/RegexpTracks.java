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

    public ArrayList<Track> GetAll() {
        ArrayList<Track> allTracks = new ArrayList<Track>(topmostTracks);
        allTracks.addAll(composedTracks);
        allTracks.addAll(tracks);

        ArrayList<Track> collapsed = new ArrayList<Track>(topmostTracks.size());
        // omit all tracks w/ an empty range
        for (Track track : allTracks) {
            if (track.Start != track.End) {
                collapsed.add(track);
            }
        }

        return collapsed;
    }

    public ArrayList<Track> GetTopTracks() {
        ArrayList<Track> allTracks = new ArrayList<Track>();
        ArrayList<Track> availableSubs = new ArrayList<Track>();
        if (re.subs != null) {
            for (Regexp sub : re.subs) {
                availableSubs.addAll(sub.Tracks.GetTopTracks());
            }
        }

        ArrayList<Track> availableComposed = composedTracks;
        ArrayList<Track> availableTracks = tracks;
        for (Track top : topmostTracks) {
            allTracks.add(top);
            ArrayList<Track> tmpComposed = new ArrayList<Track>();
            for (Track composed : availableComposed) {
                if (composed.End <= top.Start || composed.Start >= top.End) {
                    tmpComposed.add(composed);
                }
            }
            availableComposed = tmpComposed;

            ArrayList<Track> tmpSubs = new ArrayList<Track>();
            for (Track sub : availableSubs) {
                if (sub.End <= top.Start || sub.Start >= top.End) {
                    tmpSubs.add(sub);
                }
            }
            availableSubs = tmpSubs;

            ArrayList<Track> tmpTracks = new ArrayList<Track>();
            for (Track track : availableTracks) {
                if (track.End <= top.Start || track.Start >= top.End) {
                    tmpTracks.add(track);
                }
            }
            availableTracks = tmpTracks;
        }

        allTracks.addAll(availableSubs);

        for (Track composed : availableComposed) {
            allTracks.add(composed);
            ArrayList<Track> tmpTracks = new ArrayList<Track>();
            for (Track track : availableTracks) {
                if (track.End <= composed.Start || track.Start >= composed.End) {
                    tmpTracks.add(track);
                }
            }
            availableTracks = tmpTracks;
        }

        allTracks.addAll(availableTracks);
        Collections.sort(allTracks);
        return allTracks;
    }

    // Only tracks scanned in a single parser loop can be composed together.
    public void ComposeTracks(ArrayList<Track> tracks) {
        if (tracks.size() == 0) {
            return;
        }
        // FIXME tracks must be consecutive. Validate them.
        this.tracks.addAll(tracks);
        if (tracks.size() > 1) {
            insertComposedTrack(composedTracks, Track.NewComposedTrack(tracks, re));
            scanAndComposeTracks(composedTracks);
        }

        Collections.sort(this.composedTracks);
        ComposeTopmostTracks();
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


//    if (topmostTracks.size() != 0 || that.topmostTracks.size() != 0) {
//        throw new IllegalStateException("literals should not have topmost tracks");
//    }
//
//    // If one of Regexps is an escaped literal, it would has a composed track and two track w/ an escape sign in it
//        if (composedTracks.size() > 0 && that.composedTracks.size() > 0
//                && composedTracks.get(composedTracks.size()-1).End != that.composedTracks.get(0).Start) {
//        throw new IllegalStateException("concatenated tracks should be consecutive");
//    }
//
//        if (this.tracks.size() > 0 && that.tracks.size() > 0
//                && tracks.get(tracks.size()-1).End != that.tracks.get(0).Start) {
//        throw new IllegalStateException("concatenated tracks should be consecutive");
//    }
//
//    StringBuilder b = new StringBuilder();
//        for (int r : re.runes) {
//        b.appendCodePoint(r);
//    }
//
//        tracks.get(0).Freeze(that.tracks.get(that.tracks.size()-1).End, b.toString());

    // particular cases for different types of Regexps
    public void ConcatLiterals(RegexpTracks that) {
        // If one of Regexps is an escaped literal, it would has a composed track and two track w/ an escape sign in it
        if (topmostTracks.size() > 0 || that.topmostTracks.size() > 0
                || composedTracks.size() > 0 || that.composedTracks.size() > 0) {
            int lastEnd, firstBegin;
            if (topmostTracks.size() > 0) {
                lastEnd = topmostTracks.get(topmostTracks.size()-1).End;
            } else if (composedTracks.size() > 0) {
                lastEnd = composedTracks.get(composedTracks.size()-1).End;
            } else {
                lastEnd = tracks.get(tracks.size()-1).End;
            }

            if (that.topmostTracks.size() > 0) {
                firstBegin = that.topmostTracks.get(0).Start;
            } else if (composedTracks.size() > 0) {
                firstBegin = that.composedTracks.get(0).Start;
            } else {
                firstBegin = that.tracks.get(0).Start;
            }

            if (lastEnd != firstBegin) {
                throw new IllegalStateException("concatenated tracks should be consecutive");
            }

            ArrayList<Track> concats = new ArrayList<Track>(tracks);
            concats.addAll(that.tracks);
            topmostTracks.clear();
            composedTracks.clear();
            tracks.clear();
            ComposeTracks(concats);
            return;
        }

        if (tracks.size() > 1) {
            throw new IllegalStateException("literal track should have only 1 track but " + tracks.size());
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

    public void ComposeTopmostTracks() {
        // FIXME concat tracks, composed tracks and topmost tracks of subs
        ArrayList<Track> topmost = GetTopTracks();
        if (topmost.size() <= 1) {
            return;
        }

        boolean needToCompose = false;
        ArrayList<Track> consecutive = new ArrayList<Track>();
        for (Track track : topmost) {
            if (consecutive.size() == 0 || consecutive.get(consecutive.size()-1).End == track.Start) {
                consecutive.add(track);
                continue;
            }

            if (consecutive.size() > 1) {
                needToCompose |= insertComposedTrack(topmostTracks, Track.NewComposedTrack(consecutive, re));
            }

            consecutive.clear();
            consecutive.add(track);
        }

        if (consecutive.size() > 1) {
            needToCompose |= insertComposedTrack(topmostTracks, Track.NewComposedTrack(consecutive, re));
        }

        if (needToCompose) {
            scanAndComposeTracks(topmostTracks);
        }
    }

    private void scanAndComposeTracks(ArrayList<Track> tracks) {
        for (Track track : tracks) {
            if (!track.IsPlaceholder()) {
                continue;
            }

            track.Freeze(findTracks(track.Start, track.End), re);
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

    private boolean insertComposedTrack(ArrayList<Track> list, Track track) {
        boolean hasOverlappedTracks = false;
        for (Track composed : list) {
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

        list.add(track);
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
