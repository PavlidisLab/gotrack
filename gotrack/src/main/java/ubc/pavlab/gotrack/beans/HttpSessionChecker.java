package ubc.pavlab.gotrack.beans;

import java.util.Date;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class HttpSessionChecker implements HttpSessionListener {

    @Override
    public void sessionCreated( HttpSessionEvent event ) {
        System.out.printf( "Session ID %s created at %s%n", event.getSession().getId(), new Date() );
    }

    @Override
    public void sessionDestroyed( HttpSessionEvent event ) {
        System.out.printf( "Session ID %s destroyed at %s%n", event.getSession().getId(), new Date() );
    }
}
