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

import net.sourceforge.subsonic.command.*;
import net.sourceforge.subsonic.dao.CacheDao;
import net.sourceforge.subsonic.domain.Cache;
import net.sourceforge.subsonic.service.*;
import org.springframework.web.servlet.mvc.*;

import javax.servlet.http.*;
import java.util.List;

/**
 * Controller for the page used to administrate the search index.
 *
 * @author Sindre Mehus
 */
public class SearchSettingsController extends SimpleFormController {

    private SettingsService settingsService;
    private SearchService searchService;
    private List<Cache> caches;
    private CacheDao cacheDao;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        SearchSettingsCommand command = new SearchSettingsCommand();

        if (request.getParameter("update") != null) {
            searchService.createIndex();
            command.setCreatingIndex(true);
        }
        if (request.getParameter("clear") != null) {
            for (Cache cache : caches) {
                cache.clearStatistics();
                cacheDao.clearDatabase();
            }
        }

        command.setInterval("" + settingsService.getIndexCreationInterval());
        command.setHour("" + settingsService.getIndexCreationHour());
        command.setBrand(settingsService.getBrand());
        command.setCaches(caches);

        return command;
    }

    protected void doSubmitAction(Object comm) throws Exception {
        SearchSettingsCommand command = (SearchSettingsCommand) comm;

        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));
        settingsService.save();

        searchService.schedule();
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setCaches(List<Cache> caches) {
        this.caches = caches;
    }

    public void setCacheDao(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }
}
