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

import net.sourceforge.subsonic.command.MusicFolderSettingsCommand;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.service.SearchService;
import net.sourceforge.subsonic.service.SettingsService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the page used to administrate the set of music folders.
 *
 * @author Sindre Mehus
 */
public class MusicFolderSettingsController extends SimpleFormController {

    private SettingsService settingsService;
    private SearchService searchService;

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        MusicFolderSettingsCommand command = new MusicFolderSettingsCommand();

        if (request.getParameter("scanNow") != null) {
            searchService.scanLibrary();
        }

        command.setInterval(String.valueOf(settingsService.getIndexCreationInterval()));
        command.setHour(String.valueOf(settingsService.getIndexCreationHour()));
        command.setFastCache(settingsService.isFastCacheEnabled());
        command.setScanning(searchService.isScanning());
        command.setMusicFolders(wrap(settingsService.getAllMusicFolders(true, true)));
        command.setNewMusicFolder(new MusicFolderSettingsCommand.MusicFolderInfo());
        command.setReload(request.getParameter("reload") != null);
        return command;
    }

    private List<MusicFolderSettingsCommand.MusicFolderInfo> wrap(List<MusicFolder> musicFolders) {
        ArrayList<MusicFolderSettingsCommand.MusicFolderInfo> result = new ArrayList<MusicFolderSettingsCommand.MusicFolderInfo>();
        for (MusicFolder musicFolder : musicFolders) {
            result.add(new MusicFolderSettingsCommand.MusicFolderInfo(musicFolder));
        }
        return result;
    }

    @Override
    protected ModelAndView onSubmit(Object comm) throws Exception {
        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) comm;

        for (MusicFolderSettingsCommand.MusicFolderInfo musicFolderInfo : command.getMusicFolders()) {
            if (musicFolderInfo.isDelete()) {
                settingsService.deleteMusicFolder(musicFolderInfo.getId());
            } else {
                settingsService.updateMusicFolder(musicFolderInfo.toMusicFolder());
            }
        }

        MusicFolder newMusicFolder = command.getNewMusicFolder().toMusicFolder();
        if (newMusicFolder != null) {
            settingsService.createMusicFolder(newMusicFolder);
        }

        settingsService.setIndexCreationInterval(Integer.parseInt(command.getInterval()));
        settingsService.setIndexCreationHour(Integer.parseInt(command.getHour()));
        settingsService.setFastCacheEnabled(command.isFastCache());
        settingsService.save();

        searchService.schedule();
        return new ModelAndView(new RedirectView(getSuccessView() + ".view?reload"));
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }
}