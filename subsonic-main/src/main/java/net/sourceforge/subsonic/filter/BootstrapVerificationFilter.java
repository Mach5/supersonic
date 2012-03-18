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
package net.sourceforge.subsonic.filter;

import net.sourceforge.subsonic.service.SettingsService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * This filter is executed very early in the filter chain. It verifies that
 * the Supersonic home directory (c:\supersonic or /var/supersonic) exists and
 * is writable. If not, a proper error message is given to the user.
 * <p/>
 * (The Supersonic home directory is usually created automatically, but a common
 * problem on Linux is that the Tomcat user does not have the necessary
 * privileges).
 *
 * @author Sindre Mehus
 * @author Mach5
 */
public class BootstrapVerificationFilter implements Filter {

    private boolean subsonicHomeVerified = false;


    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Already verified?
        if (subsonicHomeVerified) {
            chain.doFilter(req, res);
            return;
        }

        File home = SettingsService.getSubsonicHome();
        if (!directoryExists(home)) {
            error(res, "<p>The directory <b>" + home + "</b> does not exist. Please create it and make it writable, " +
                       "then restart the servlet container.</p>" +
                       "<p>(You can override the directory location by specifying -Dsubsonic.home=... when " +
                       "starting the servlet container.)</p>");

        } else if (!directoryWritable(home)) {
            error(res, "<p>The directory <b>" + home + "</b> is not writable. Please change file permissions, " +
                       "then restart the servlet container.</p>" +
                       "<p>(You can override the directory location by specifying -Dsubsonic.home=... when " +
                       "starting the servlet container.)</p>");

        } else {
            subsonicHomeVerified = true;
            chain.doFilter(req, res);
        }
    }

    private boolean directoryExists(File dir) {
        return dir.exists() && dir.isDirectory();
    }

    private boolean directoryWritable(File dir) {
        try {
            File tempFile = File.createTempFile("test", null, dir);
            tempFile.delete();
            return true;
        } catch (IOException x) {
            return false;
        }
    }

    private void error(ServletResponse res, String error) throws IOException {
        ServletOutputStream out = res.getOutputStream();
        out.println("<html>" +
                    "<head><title>Supersonic Error</title></head>" +
                    "<body>" +
                    "<h2>Supersonic Error</h2>" +
                    error +
                    "</body>" +
                    "</html>");
    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }
}
