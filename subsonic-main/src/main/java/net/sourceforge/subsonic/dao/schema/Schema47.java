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
package net.sourceforge.subsonic.dao.schema;

import net.sourceforge.subsonic.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Subsonic version 4.7.
 *
 * @author Sindre Mehus
 */
public class Schema47 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema47.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 20") == 0) {
            LOG.info("Updating database schema to version 20.");
            template.execute("insert into version values (20)");
        }

        if (!tableExists(template, "media_file")) {
            LOG.info("Database table 'media_file' not found.  Creating it.");
            template.execute("create cached table media_file (" +
                    "id identity," +
                    "path varchar not null," +
                    "folder varchar," +
                    "type varchar not null," +
                    "format varchar," +
                    "title varchar," +
                    "album varchar," +
                    "artist varchar," +
                    "disc_number int," +
                    "track_number int," +
                    "year int," +
                    "genre varchar," +
                    "bit_rate int," +
                    "variable_bit_rate boolean not null," +
                    "duration_seconds int," +
                    "file_size bigint," +
                    "width int," +
                    "height int," +
                    "cover_art_path varchar," +
                    "parent_path varchar not null," +
                    "play_count int not null," +
                    "last_played datetime," +
                    "comment varchar," +
                    "created datetime not null," +
                    "last_modified datetime not null," +
                    "last_scanned datetime not null," +
                    "children_last_updated datetime not null," +
                    "present boolean not null," +
                    "version int not null," +
                    "unique (path))");

            template.execute("create index idx_media_file_path on media_file(path)");
            template.execute("create index idx_media_file_parent_path on media_file(parent_path)");
//            template.execute("create index idx_media_file_folder on media_file(folder)");
            template.execute("create index idx_media_file_type on media_file(type)");
            template.execute("create index idx_media_file_album on media_file(album)");
            template.execute("create index idx_media_file_artist on media_file(artist)");
//            template.execute("create index idx_media_file_year on media_file(year)");
            template.execute("create index idx_media_file_present on media_file(present)");
            template.execute("create index idx_media_file_genre on media_file(genre)");
            template.execute("create index idx_media_file_play_count on media_file(play_count)");
            template.execute("create index idx_media_file_last_played on media_file(last_played)");

            LOG.info("Database table 'media_file' was created successfully.");
        }
    }
}