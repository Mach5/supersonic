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
package net.sourceforge.subsonic.validator;

import net.sourceforge.subsonic.command.*;
import net.sourceforge.subsonic.controller.*;
import net.sourceforge.subsonic.service.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.*;

import java.io.*;

/**
 * Validator for {@link SavePlaylistController}.
 *
 * @author Sindre Mehus
 */
public class SavePlaylistValidator implements Validator {

    public boolean supports(Class clazz) {
        return clazz.equals(SavePlaylistCommand.class);
    }

    public void validate(Object obj, Errors errors) {
        String name = ((SavePlaylistCommand) obj).getName();
        if (StringUtils.isBlank(name)) {
            errors.rejectValue("name", "playlist.save.noname");
        }
    }
}
