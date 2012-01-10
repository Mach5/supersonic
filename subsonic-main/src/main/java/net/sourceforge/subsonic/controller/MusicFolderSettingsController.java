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
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.service.*;
import net.sourceforge.subsonic.domain.*;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.mvc.*;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.*;
import java.util.*;
import java.io.*;

/**
 * Controller for the page used to administrate the set of music folders.
 *
 * @author Sindre Mehus
 */
public class MusicFolderSettingsController extends ParameterizableViewController {

    private SettingsService settingsService;
    private SearchService searchService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        if (isFormSubmission(request)) {
            String error = handleParameters(request);
            map.put("error", error);
            if (error == null) {
                map.put("reload", true);
            }
        }

        ModelAndView result = super.handleRequestInternal(request, response);
        map.put("musicFolders", settingsService.getAllMusicFolders(true, true));

        result.addObject("model", map);
        return result;
    }

    /**
     * Determine if the given request represents a form submission.
     * @param request current HTTP request
     * @return if the request represents a form submission
     */
    private boolean isFormSubmission(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }

    private String handleParameters(HttpServletRequest request) {

        for (MusicFolder musicFolder : settingsService.getAllMusicFolders(true, true)) {
            Integer id = musicFolder.getId();

            String path = getParameter(request, "path", id);
            String name = getParameter(request, "name", id);
            boolean enabled = getParameter(request, "enabled", id) != null;
            boolean delete = getParameter(request, "delete", id) != null;

            if (delete) {
                settingsService.deleteMusicFolder(id);
            } else if (path == null) {
                return "musicfoldersettings.nopath";
            } else {
                File file = new File(path);
                if (name == null) {
                    name = file.getName();
                }
                musicFolder.setName(name);
                musicFolder.setPath(file);
                musicFolder.setEnabled(enabled);
                musicFolder.setChanged(new Date());
                settingsService.updateMusicFolder(musicFolder);
            }
        }

        String name = StringUtils.trimToNull(request.getParameter("name"));
        String path = StringUtils.trimToNull(request.getParameter("path"));
        boolean enabled = StringUtils.trimToNull(request.getParameter("enabled")) != null;

        if (name != null || path != null) {
            if (path == null) {
                return "musicfoldersettings.nopath";
            }
            File file = new File(path);
            if (name == null) {
                name = file.getName();
            }
            settingsService.createMusicFolder(new MusicFolder(file, name, enabled, new Date()));
        }

        if (request.getParameter("scanNow") != null) {
            searchService.createIndex();
        }

        return null;
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}