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

        if (!tableExists(template, "cache")) {
            LOG.info("Database table 'cache' not found.  Creating it.");
            template.execute("create cached table cache (" +
                             "type int not null," +
                             "key varchar not null," +
                             "value other not null," +
                             "created bigint not null)");
            template.execute("create index idx_cache_type_key on cache(type, key)");


            LOG.info("Database table 'cache' was created successfully.");
        }
    }
}