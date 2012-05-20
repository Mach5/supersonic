/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.LongRange;

/**
 * Miscellaneous string utility methods.
 *
 * @author Sindre Mehus
 */
public final class StringUtil {

    public static final String ENCODING_LATIN = "ISO-8859-1";
    public static final String ENCODING_UTF8 = "UTF-8";
    private static final DateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String[][] HTML_SUBSTITUTIONS = {
            {"&", "&amp;"},
            {"<", "&lt;"},
            {">", "&gt;"},
            {"'", "&#39;"},
            {"\"", "&#34;"},
    };

    private static final String[][] MIME_TYPES = {
            {"mp3", "audio/mpeg"},
            {"ogg", "audio/ogg"},
            {"oga", "audio/ogg"},
            {"ogx", "application/ogg"},
            {"aac", "audio/mp4"},
            {"m4a", "audio/mp4"},
            {"flac", "audio/flac"},
            {"wav", "audio/x-wav"},
            {"wma", "audio/x-ms-wma"},
            {"ape", "audio/x-monkeys-audio"},
            {"mpc", "audio/x-musepack"},
            {"shn", "audio/x-shn"},
            {"webm", "audio/webm"},

            {"flv", "video/x-flv"},
            {"avi", "video/avi"},
            {"mpg", "video/mpeg"},
            {"mpeg", "video/mpeg"},
            {"mp4", "video/mp4"},
            {"m4v", "video/x-m4v"},
            {"mkv", "video/x-matroska"},
            {"mov", "video/quicktime"},
            {"wmv", "video/x-ms-wmv"},
            {"ogv", "video/ogg"},
            {"divx", "video/divx"},
            {"m2ts", "video/MP2T"},

            {"gif", "image/gif"},
            {"jpg", "image/jpeg"},
            {"jpeg", "image/jpeg"},
            {"png", "image/png"},
            {"bmp", "image/bmp"},
    };

    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":", "\"", "?", "*"};

    /**
     * Disallow external instantiation.
     */
    private StringUtil() {
    }

    /**
     * Returns the specified string converted to a format suitable for
     * HTML. All single-quote, double-quote, greater-than, less-than and
     * ampersand characters are replaces with their corresponding HTML
     * Character Entity code.
     *
     * @param s the string to convert
     * @return the converted string
     */
    public static String toHtml(String s) {
        if (s == null) {
            return null;
        }
        for (String[] substitution : HTML_SUBSTITUTIONS) {
            if (s.contains(substitution[0])) {
                s = s.replaceAll(substitution[0], substitution[1]);
            }
        }
        return s;
    }


    /**
     * Formats the given date to a ISO-8601 date/time format, and UTC timezone.
     * <p/>
     * The returned date uses the following format: 2007-12-17T14:57:17
     *
     * @param date The date to format
     * @return The corresponding ISO-8601 formatted string.
     */
    public static String toISO8601(Date date) {
        if (date == null) {
            return null;
        }

        synchronized (ISO_8601_DATE_FORMAT) {
            return ISO_8601_DATE_FORMAT.format(date);
        }
    }

    /**
     * Removes the suffix (the substring after the last dot) of the given string. The dot is
     * also removed.
     *
     * @param s The string in question, e.g., "foo.mp3".
     * @return The string without the suffix, e.g., "foo".
     */
    public static String removeSuffix(String s) {
        int index = s.lastIndexOf('.');
        return index == -1 ? s : s.substring(0, index);
    }

    /**
     * Returns the proper MIME type for the given suffix.
     *
     * @param suffix The suffix, e.g., "mp3" or ".mp3".
     * @return The corresponding MIME type, e.g., "audio/mpeg". If no MIME type is found,
     *         <code>application/octet-stream</code> is returned.
     */
    public static String getMimeType(String suffix) {
        for (String[] map : MIME_TYPES) {
            if (map[0].equalsIgnoreCase(suffix) || ('.' + map[0]).equalsIgnoreCase(suffix)) {
                return map[1];
            }
        }
        return "application/octet-stream";
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     *
     * @param byteCount The number of bytes.
     * @param locale    The locale used for formatting.
     * @return The formatted string.
     */
    public static synchronized String formatBytes(long byteCount, Locale locale) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            NumberFormat gigaByteFormat = new DecimalFormat("0.00 GB", new DecimalFormatSymbols(locale));
            return gigaByteFormat.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            NumberFormat megaByteFormat = new DecimalFormat("0.0 MB", new DecimalFormatSymbols(locale));
            return megaByteFormat.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            NumberFormat kiloByteFormat = new DecimalFormat("0 KB", new DecimalFormatSymbols(locale));
            return kiloByteFormat.format((double) byteCount / 1024);
        }

        return byteCount + " B";
    }

    /**
     * Formats a duration with minutes and seconds, e.g., "93:45"
     */
    public static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(6);
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
    }

    /**
     * Splits the input string. White space is interpreted as separator token. Double quotes
     * are interpreted as grouping operator. <br/>
     * For instance, the input <code>"u2 rem "greatest hits""</code> will return an array with
     * three elements: <code>{"u2", "rem", "greatest hits"}</code>
     *
     * @param input The input string.
     * @return Array of elements.
     */
    public static String[] split(String input) {
        if (input == null) {
            return new String[0];
        }

        Pattern pattern = Pattern.compile("\".*?\"|\\S+");
        Matcher matcher = pattern.matcher(input);

        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            String element = matcher.group();
            if (element.startsWith("\"") && element.endsWith("\"") && element.length() > 1) {
                element = element.substring(1, element.length() - 1);
            }
            result.add(element);
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Reads lines from the given input stream. All lines are trimmed. Empty lines and lines starting
     * with "#" are skipped. The input stream is always closed by this method.
     *
     * @param in The input stream to read from.
     * @return Array of lines.
     * @throws IOException If an I/O error occurs.
     */
    public static String[] readLines(InputStream in) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(in));
            List<String> result = new ArrayList<String>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!line.startsWith("#") && line.length() > 0) {
                    result.add(line);
                }
            }
            return result.toArray(new String[result.size()]);

        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Converts the given string of whitespace-separated integers to an <code>int</code> array.
     *
     * @param s String consisting of integers separated by whitespace.
     * @return The corresponding array of ints.
     * @throws NumberFormatException If string contains non-parseable text.
     */
    public static int[] parseInts(String s) {
        if (s == null) {
            return new int[0];
        }

        String[] strings = StringUtils.split(s);
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    /**
     * Change protocol from "https" to "http" for the given URL. The port number is also changed,
     * but not if the given URL is already "http".
     *
     * @param url  The original URL.
     * @param port The port number to use, for instance 443.
     * @return The transformed URL.
     * @throws MalformedURLException If the original URL is invalid.
     */
    public static String toHttpUrl(String url, int port) throws MalformedURLException {
        URL u = new URL(url);
        if ("https".equals(u.getProtocol())) {
            return new URL("http", u.getHost(), port, u.getFile()).toString();
        }
        return url;
    }

    /**
     * Determines whether a is equal to b, taking null into account.
     *
     * @return Whether a and b are equal, or both null.
     */
    public static boolean isEqual(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Parses a locale from the given string.
     *
     * @param s The locale string. Should be formatted as per the documentation in {@link Locale#toString()}.
     * @return The locale.
     */
    public static Locale parseLocale(String s) {
        if (s == null) {
            return null;
        }

        String[] elements = s.split("_");

        if (elements.length == 0) {
            return new Locale(s, "", "");
        }
        if (elements.length == 1) {
            return new Locale(elements[0], "", "");
        }
        if (elements.length == 2) {
            return new Locale(elements[0], elements[1], "");
        }
        return new Locale(elements[0], elements[1], elements[2]);
    }

    /**
     * URL-encodes the input value using UTF-8.
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StringUtil.ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    /**
    * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
    *
    * @param s The string to encode.
    * @return The encoded string.
    */
    public static String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        try {
            utf8 = s.getBytes(ENCODING_UTF8);
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
        return String.valueOf(Hex.encodeHex(utf8));
    }

    /**
     * Decodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to decode.
     * @return The decoded string.
     * @throws Exception If an error occurs.
     */
    public static String utf8HexDecode(String s) throws Exception {
        if (s == null) {
            return null;
        }
        return new String(Hex.decodeHex(s.toCharArray()), ENCODING_UTF8);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    public static String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return new String(Hex.encodeHex(md5.digest(s.getBytes(ENCODING_UTF8))));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    /**
     * Returns the file part of an URL. For instance:
     * <p/>
     * <code>
     * getUrlFile("http://archive.ncsa.uiuc.edu:80/SDG/Software/Mosaic/Demo/url-primer.html")
     * </code>
     * <p/>
     * will return "url-primer.html".
     *
     * @param url The URL in question.
     * @return The file part, or <code>null</code> if no file can be resolved.
     */
    public static String getUrlFile(String url) {
        try {
            String path = new URL(url).getPath();
            if (StringUtils.isBlank(path) || path.endsWith("/")) {
                return null;
            }

            File file = new File(path);
            String filename = file.getName();
            if (StringUtils.isBlank(filename)) {
                return null;
            }
            return filename;

        } catch (MalformedURLException x) {
            return null;
        }
    }

    /**
     * Rewrites the URL by changing the protocol, host and port.
     *
     * @param urlToRewrite               The URL to rewrite.
     * @param urlWithProtocolHostAndPort Use protocol, host and port from this URL.
     * @return The rewritten URL, or an unchanged URL if either argument is not a proper URL.
     */
    public static String rewriteUrl(String urlToRewrite, String urlWithProtocolHostAndPort) {
        if (urlToRewrite == null) {
            return null;
        }

        try {
            URL urlA = new URL(urlToRewrite);
            URL urlB = new URL(urlWithProtocolHostAndPort);

            URL result = new URL(urlB.getProtocol(), urlB.getHost(), urlB.getPort(), urlA.getFile());
            return result.toExternalForm();
        } catch (MalformedURLException x) {
            return urlToRewrite;
        }
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes ("/" and "\")
     * with dashes ("-").
     *
     * @param filename The filename in question.
     * @return The filename with special characters replaced by underscores.
     */
    public static String fileSystemSafe(String filename) {
        for (String s : FILE_SYSTEM_UNSAFE) {
            filename = filename.replace(s, "-");
        }
        return filename;
    }

    /**
     * Parses the given string as a HTTP header byte range.  See chapter 14.36.1 in RFC 2068
     * for details.
     * <p/>
     * Only a subset of the allowed syntaxes are supported. Only ranges which specify first-byte-pos
     * are supported. The last-byte-pos is optional.
     *
     * @param range The range from the HTTP header, for instance "bytes=0-499" or "bytes=500-"
     * @return A range object (using inclusive values). If the last-byte-pos is not given, the end of
     *         the returned range is {@link Long#MAX_VALUE}. The method returns <code>null</code> if the syntax
     *         of the given range is not supported.
     */
    public static LongRange parseRange(String range) {
        if (range == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("bytes=(\\d+)-(\\d*)");
        Matcher matcher = pattern.matcher(range);

        if (matcher.matches()) {
            String firstString = matcher.group(1);
            String lastString = StringUtils.trimToNull(matcher.group(2));

            long first = Long.parseLong(firstString);
            long last = lastString == null ? Long.MAX_VALUE : Long.parseLong(lastString);

            if (first > last) {
                return null;
            }

            return new LongRange(first, last);
        }
        return null;
    }

    public static String removeMarkup(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("<.*?>", "");
    }

    public static String getRESTProtocolVersion() {
        // TODO: Read from xsd.
        return "1.8.0";
    }
}
