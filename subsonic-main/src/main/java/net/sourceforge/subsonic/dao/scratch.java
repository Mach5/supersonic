package net.sourceforge.subsonic.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class scratch {

    public static void main(String[] args) throws IOException {
        new scratch();
    }

    public scratch() throws FileNotFoundException {
        for (int i = 57; i <= 96; i++) {
            try {
                process(i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void process(int i) throws FileNotFoundException {
        System.out.println("Processing " + i);
        List<Redirection> redirections = getRedirections(i);
        PrintWriter writer = new PrintWriter(new File("csv", i + ".csv"));
        writer.println("id, redirect_to, local_redirect_to, last_updated, last_read");
        for (Redirection redirection : redirections) {
            writer.println(redirection.id + ", " + redirection.redirectTo + ", " + redirection.localRedirectTo + ", " +
                    (redirection.lastUpdated == null ? 0 : redirection.lastUpdated.getTime()) + ", " +
                    (redirection.lastRead == null ? 0 : redirection.lastRead.getTime()));
        }

        writer.close();
    }

    public List<Redirection> getRedirections(int i) {
        JdbcTemplate template = getJdbcTemplate(i);
        List result = template.query("select id, redirect_to, local_redirect_to, last_updated, last_read from redirection", new MyRowMapper());
        template.update("shutdown") ;
        return result;
    }

    private JdbcTemplate getJdbcTemplate(int i) {
        return new JdbcTemplate(createDataSource(i));
    }

    private DataSource createDataSource(int i) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:file:/Users/sindre/tmp/backups/" + i);
        ds.setUsername("sa");
        ds.setPassword("");

        return ds;
    }

    private class MyRowMapper implements ParameterizedRowMapper<Redirection> {
        public Redirection mapRow(ResultSet rs, int rowNum) throws SQLException {
            Redirection redirection = new Redirection();
            redirection.id = rs.getInt(1);
            redirection.redirectTo = rs.getString(2);
            redirection.localRedirectTo = rs.getString(3);
            redirection.lastUpdated = rs.getTimestamp(4);
            redirection.lastRead = rs.getTimestamp(5);
            return redirection;
        }
    }

    private static class Redirection {
        int id;
        String redirectTo;
        String localRedirectTo;
        Date lastUpdated;
        Date lastRead;
    }
}
