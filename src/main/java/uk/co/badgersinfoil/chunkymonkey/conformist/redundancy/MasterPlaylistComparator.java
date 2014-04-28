package uk.co.badgersinfoil.chunkymonkey.conformist.redundancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.Playlist;
import net.chilicat.m3u8.PlaylistInfo;

public class MasterPlaylistComparator {

	public static class PlaylistComparisonResult {
		private final List<Element> in1Only;
		private final List<Element> in2Only;
		private List<Element> inBoth;

		public PlaylistComparisonResult(List<Element> in1Only,
		                                List<Element> in2Only,
		                                List<Element> inBoth)
		{
			this.in1Only = in1Only;
			this.in2Only = in2Only;
			this.inBoth = inBoth;
		}
		public List<Element> getIn1Only() {
			return in1Only;
		}
		public List<Element> getIn2Only() {
			return in2Only;
		}
		public List<Element> getInBoth() {
			return inBoth;
		}
	}

	public PlaylistComparisonResult compare(final HlsRedundantStreamContext ctx,
	                    final Playlist playlist1,
	                    final Playlist playlist2)
	{
		List<Element> el1 = playlist1.getElements();
		List<Element> el2 = playlist2.getElements();
		// I probably missed something in the std library to do this,
		List<Element> in1Only = new ArrayList<Element>();
		List<Element> in2Only = new ArrayList<Element>();
		List<Element> inBoth = new ArrayList<Element>();
		for (Element element : el1) {
			if (hasEquivalent(el2, element)) {
				inBoth.add(element);
			} else {
				in1Only.add(element);
			}
		}
		for (Element element : el2) {
			if (!hasEquivalent(el1, element)) {
				in2Only.add(element);
			}
		}
		return new PlaylistComparisonResult(in1Only, in2Only, inBoth);
	}

	private boolean hasEquivalent(List<Element> elements, Element element) {
		PlaylistInfo info = element.getPlayListInfo();
		for (Element el : elements) {
			if (isEquivalent(el.getPlayListInfo(), info)) {
				return true;
			}
		}
		return false;
	}

	private boolean isEquivalent(PlaylistInfo info1,
	                             PlaylistInfo info2) {
		return info1.getBandWitdh() == info2.getBandWitdh()
		     && isEquivalentCodecs(info1.getCodecs(), info2.getCodecs())
		     && eqIgnoreCase(info1.getResolution(), info2.getResolution());
	}

	private boolean eqIgnoreCase(String s1, String s2) {
		return s1==null && s2==null
			|| s1!=null && s1.equalsIgnoreCase(s2);
	}

	private boolean isEquivalentCodecs(String codecs1, String codecs2) {
		Set<String> codecSet1 = ImmutableSet.copyOf(trim(codecs1.split(",")));
		Set<String> codecSet2 = ImmutableSet.copyOf(trim(codecs1.split(",")));
		return codecSet1.equals(codecSet2);
	}

	/**
	 * Replaces each String 's' in the given array with the result of
	 * 's.trim()'.
	 *
	 * @return the given array
	 */
	private static String[] trim(String[] strings) {
		for (int i = 0; i < strings.length; i++) {
			strings[i] = strings[i].trim();
		}
		return strings;
	}

}
