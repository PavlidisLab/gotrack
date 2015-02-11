package ubc.pavlab.gotrack.beans;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.log4j.Logger;

public class HttpSessionChecker implements HttpSessionListener {

    private static final Logger log = Logger.getLogger( HttpSessionChecker.class );

    @Override
    public void sessionCreated( HttpSessionEvent event ) {
        log.info( String.format( "Session ID %s created", event.getSession().getId() ) );
    }

    @Override
    public void sessionDestroyed( HttpSessionEvent event ) {
        log.info( String.format( "Session ID %s destroyed", event.getSession().getId() ) );
    }
}
