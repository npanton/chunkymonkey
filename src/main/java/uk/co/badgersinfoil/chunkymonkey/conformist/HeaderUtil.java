package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.HashSet;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;

public class HeaderUtil {

	/**
	 * <p>Given headers that are allowed to contain a list of simple elements,
	 * where multiple occurrences of the header are equivalent to a single
	 * header with a list, this method finds all headers with the given
	 * name are returns a set which merges together all the elements
	 * present.</p>
	 *
	 * <p>e.g. given, a response including the headers,</p>
	 *
	 * <ul>
	 * <li>X-Foo: one, two, three</li>
	 * <li>X-Foo: two, three, four</li>
	 * </ul>
	 *
	 * <p>a call to <code>getMergedHeaderElements(resp, "X-Foo")</code>
	 * would produce the set <code>{"one", "two", "three", "four"}</code>.</p>
	 *
	 */
	public static Set<String> getMergedHeaderElements(HttpResponse resp,
                                                          String name)
	{
		Header[] headers = resp.getHeaders(name);
		Set<String> elements = new HashSet<String>();
		for (Header header : headers) {
			for (HeaderElement elem : header.getElements()) {
				elements.add(elem.getName().toLowerCase());
			}
		}
		return elements;
	}
}
